package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.EpicSource
import com.example.collectalogger2.util.IGDBSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant


class EpicDataSource(
    var userInfoFlow: Flow<String>,
    var userInfoSetter: suspend (String) -> Unit,
    gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = "Epic Games"


    override suspend fun getGames(forceUpdate: Boolean): Flow<Game> = flow {
        val userInfo = userInfoFlow.first()
        if (userInfo == "") throw AccountException("User is not logged into Epic Games!", libraryName)
        // sets JSON to the thing
        var userJson = refreshLogin(JSONObject(userInfo))
        // after refreshing JSON if applicable, get the list of all games, then iterate through and emit.
        /*
        1. Get assets from the API.
        2. Get playtime from the API.
        3. Make a for loop iterating through assets.
        4. Skip elements where the Playnite implementation skips
        5. Get IGDB information for each game.
        6. Create a game, fill it with info, and emit it.
         */
        // invoke request with the assets URL and tokens.
        var assetsResponse = EpicSource.makeAPICall(
            domain = "launcher-public-service-prod06.ol.epicgames.com",
            path = "launcher/api/public/assets/Windows?label=Live",
            isGet = true,
            headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
            params = mapOf(),
            bodyParams = mapOf(),
            isJsonArray = true
        ) as JSONArray
        // now, invoke request with the playtime URL and tokens.
        var playtimeResponse = EpicSource.makeAPICall(
            domain = "library-service.live.use1a.on.epicgames.com",
            path = "library/api/public/playtime/account/${userJson.get("account_id")}/all",
            isGet = true,
            headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
            params = mapOf(),
            bodyParams = mapOf(),
            isJsonArray = true
        ) as JSONArray

        // make a map of playtimes for efficiency
        val artifactMap = mutableMapOf<String, JSONObject>()
        for (ii in 0 until playtimeResponse.length()) {
            val obj = playtimeResponse.get(ii) as JSONObject
            artifactMap[obj.getString("artifactId")] = obj
        }

        // finally, make a for loop.
        for (i in 0 until assetsResponse.length()) {
            val epicGame = assetsResponse.get(i) as JSONObject
            var newGame = Game()
            // get playtime
            // in playtimes, artifact ID lines up with appName
            val playtimeObj = artifactMap[epicGame.getString("appName")]
            if (playtimeObj != null) {
                // since epic playtimes are likely in seconds
                val newTime = (playtimeObj.get("totalTime") as Integer).toLong() / 60
                newGame = newGame.copy(playTime = newTime)
            }
            // check if the ID is in the database already
            var epicId = "${epicGame.get("namespace")} ${epicGame.get("catalogItemId")}"
            var existingGame = gameDao.getGameByEpicId(epicId)
            if (!forceUpdate && existingGame != null) {
                var modifiedGame = existingGame.copy(
                    platform = existingGame.platform.plus("PC"),
                    source = existingGame.source.plus(libraryName),
                    playTime = newGame.playTime
                )
                // update playtime
                if (modifiedGame != existingGame)
                    emit(modifiedGame)
                // skip all the other stuff
                continue
            }

            // set the epic ID (which i'm making up)
            newGame = newGame.copy(epicId = epicId)

            var catalogResponseRaw = (EpicSource.makeAPICall(
                domain = "catalog-public-service-prod06.ol.epicgames.com",
                path = "catalog/api/shared/namespace/${epicGame.get("namespace")}/bulk/items?id=${epicGame.get("catalogItemId")}&country=US&locale=en-US&includeMainGameDetails=true",
                isGet = true,
                headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
                params = mapOf(),
                bodyParams = mapOf()
            ) as JSONObject)
            var catalogResponse = catalogResponseRaw.get("${epicGame.get("catalogItemId")}") as JSONObject
            // sift out DLCs and such: parse through categories
            var categories = catalogResponse.get("categories") as JSONArray
            var categoriesList = mutableListOf<String>()
            for (j in 0 until categories.length()) {
                var categoryObj = categories.get(j) as JSONObject
                categoriesList.add(categoryObj.getString("path"))
            }
            if (!categoriesList.contains("applications")) {
                continue
            }
            if (catalogResponse.has("mainGameItem")) {
                continue
            }
            if ( // if it has a category that doesn't match with a game, skip
                categoriesList.contains("software") ||
                categoriesList.contains("digitalextras") ||
                categoriesList.contains("plugins") ||
                categoriesList.contains("plugins/engine") ||
                categoriesList.contains("addons")
            ) {
                continue
            }

            var sanitizedTitle = catalogResponse.getString("title").replace("\"", "\\\"")

            // make a call to IGDB
            var igdbResponse: JSONArray = IGDBSource.makeAPICall(
                "external_games",
                "fields game, external_game_source; where name = \"${sanitizedTitle}\";"
            )
            var igdbId: Long? = null
            var steamIgdbId: Long? = null
            // loop through the response to get the ID we want
            for (iii in 0 until igdbResponse.length()) {
                val item: JSONObject = igdbResponse.get(iii) as JSONObject
                if (!item.has("external_game_source")) continue
                if (item.get("external_game_source") == 26) { // store ID matches, so get it and go home
                    igdbId = (item.get("game") as Integer).toLong()
                    break
                }
                if (item.get("external_game_source") == 1) {
                    steamIgdbId = (item.get("game") as Integer).toLong()
                }
            }
            if (igdbId == null && steamIgdbId != null) {
                Log.i("Game does not have Epic listing, using Steam as a fallback", sanitizedTitle)
                igdbId = steamIgdbId
            }
            else if (igdbId == null && igdbResponse.length() > 0) {
                Log.i("Game does not have Steam listing, using anything as a fallback", sanitizedTitle)
                igdbId = ((igdbResponse.get(0) as JSONObject).get("game") as Integer).toLong()
            }
            else if (igdbId == null) {
                // try to use a slug as another fallback
                var slug = sanitizedTitle
                    .lowercase()
                    .replace(" ", "-")
                    .replace(":", "")
                    .replace(",", "")
                    .replace(".", "")
                    .replace("\"", "")
                    .replace("'", "")
                    .replace("™", "")
                    .replace("®", "")
                    .replace("–", "-")
                    .replace("---", "-")
                    .replace("--", "-") // helps with game of the year editions
                // make an API call with the slug
                igdbResponse = IGDBSource.makeAPICall(
                    "games",
                    "fields id; where slug = \"$slug\";"
                )
                if (igdbResponse.length() > 0) {
                    Log.i("Fallback to slug!", slug)
                    igdbId = ((igdbResponse.get(0) as JSONObject).get("id") as Integer).toLong()
                } else {
                    Log.w("Still does not match! Skipping for now", slug)
                    continue
                }


            }
            // set the igdbId of the game
            newGame = newGame.copy(igdbId = igdbId)
            // now, make a second call to IGDB to get the info.
            igdbResponse = IGDBSource.makeAPICall(
                "games",
                "fields summary, name, genres; where id = ${igdbId};"
            )
            var igdbResponseObj = igdbResponse.get(0) as JSONObject
            if (!igdbResponseObj.has("summary") ||
                !igdbResponseObj.has("name")
            ) { // Note: Some games don't have genre for whatever reason. Peggle Deluxe doesn't, for example.
                Log.w("Specific attribute not received from the response:", "$igdbResponse")
                continue
            } // Add the info to the Game object

            newGame = newGame.copy(description = igdbResponseObj.get("summary") as String)
            newGame = newGame.copy(title = igdbResponseObj.get("name") as String)
            newGame = newGame.copy(sortingName = getSortingName(newGame.title))
            /*
            TODO: Genre is not added yet since that'd require looping and also associating
            IGDB values with strings, which is outside the wheelhouse for now.
             */
            // make a third API call for the image ID from which you can construct a URL out of
            // the endpoint should be "covers"
            // and the body should be "fields image_id; where game = ${game_id};"
            // then set the imageURL to "https://images.igdb.com/igdb/image/upload/t_cover_big/${imageID}.jpg"
            igdbResponse = IGDBSource.makeAPICall(
                "covers",
                "fields image_id; where game = ${newGame.igdbId};")
            igdbResponseObj = igdbResponse.get(0) as JSONObject
            if (!igdbResponseObj.has("image_id")
            ) {
                Log.w("image_id not received from the response. Skipping adding image:", "$igdbResponse")
            } else {
                var imageId = igdbResponseObj.get("image_id")
                newGame = newGame.copy(imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/${imageId}.jpg")
            }

            // Add PC (because it's Steam!)
            newGame = newGame.copy(platform = newGame.platform.plus("PC"))
            newGame = newGame.copy(source = newGame.source.plus(libraryName))
            // Add features to this in the future perhaps, but otherwise you are done!
            emit(newGame)



        }
    }

    /**
     * Checks for expired Login info and if a refresh is possible.
     * If so, refreshes and updates the json. Returns the object.
     */
    private suspend fun refreshLogin(json: JSONObject): JSONObject {
        // check the expiration date
        // Minus one hour to prevent it from expiring midway through getting games
        var expiry = Instant.parse(json.get("expires_at") as String).minus(Duration.ofHours(1))
        var now = Instant.now()
        // if not expired, who cares!
        if (now.isBefore(expiry)) return json
        // if it is expired, check to see if the refresh token is expired
        var refreshExpiry = Instant.parse(json.get("refresh_expires_at") as String)
        // if so, throw an exception
        if (!now.isBefore(refreshExpiry))
            throw AccountException("Cannot refresh login! Must log in again to Epic Games!", libraryName)
        // call a refresh.
        val newJson = EpicSource.makeAPICall(
            domain = "account-public-service-prod03.ol.epicgames.com",
            path = "account/api/oauth/token",
            isGet = false,
            headerss = mapOf("Authorization" to "basic MzRhMDJjZjhmNDQxNGUyOWIxNTkyMTg3NmRhMzZmOWE6ZGFhZmJjY2M3Mzc3NDUwMzlkZmZlNTNkOTRmYzc2Y2Y="),
            params = mapOf(),
            bodyParams = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to "${json.get("refresh_token")}",
                "token_type" to "eg1"
            )
        ) as JSONObject

        // update the thing
        userInfoSetter(newJson.toString())
        return newJson
    }
}