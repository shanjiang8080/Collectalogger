package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.IGDBSource
import com.example.collectalogger2.util.SteamSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject


/**
 * The bare minimum, you must get the steam library working.
 *
 */
class SteamDataSource(var userIdFlow: Flow<String>, gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = "Steam"


    override suspend fun getGames(forceUpdate: Boolean): Flow<Game> = flow {
        // get the user ID
        val userId = userIdFlow.first()
        if (userId == "") throw AccountException("User is not logged into Steam!", libraryName)
        // so what you do, is that you gotta use the API-key
        // Make an API call to Steam
        val jsonObject = SteamSource.makeAPICall(
            endpoint = "IPlayerService",
            endpoint2 = "GetOwnedGames",
            version = 1,
            mapOf("steamid" to userId)
        )
        // assert that it has a list of games
        Log.i("Steam games fetched!", "")
        val responseJSON: JSONObject = jsonObject.get("response") as JSONObject
        val games = responseJSON.get("games") as JSONArray
        // this has many games
        for (i in 0 until games.length()) {
            val apiGame = games.getJSONObject(i)

            var steamAppID: Long = (apiGame.get("appid") as Integer).toLong()
            // Quick check: If the Steam version of the game exists in the database already,
            // skip.
            var steamGame = gameDao.getGameBySteamId(steamAppID)
            var newPlayTime = (apiGame.get("playtime_forever") as Integer).toLong()
            if (!forceUpdate && steamGame != null) {
                // if the platform doesn't have steam, add it
                var modifiedSteamGame = steamGame.copy(
                    platform = steamGame.platform.plus("PC"),
                    source = steamGame.source.plus(libraryName),
                    playTime = newPlayTime
                )

                if (modifiedSteamGame != steamGame)
                    emit(modifiedSteamGame)
                continue
            }

            // Make a new Game object to store the information
            var game = Game()
            // for each game, get the appID, playtime. "Last Played" can come later.
            game = game.copy(playTime = (apiGame.get("playtime_forever") as Integer).toLong())

            // add the app ID to the game's SteamId
            game = game.copy(steamId = steamAppID)

            // make an API request for IGDB, then delay for ms so you don't get rate limited
            var igdbResponse: JSONArray = IGDBSource.makeAPICall(
                "websites",
                "fields game; where url = \"https://store.steampowered.com/app/${steamAppID}\";")
            if (igdbResponse.length() == 0) {
                Log.w("Response is empty! Could be a hidden game like Starbound (Unstable). Skipping for now.", "steamAppID: $steamAppID")
                continue
            }
            var igdbResponseObj = igdbResponse.get(0) as JSONObject
            if (!igdbResponseObj.has("game")) {
                Log.w("No 'game' attribute from response! Could be a hidden game like Starbound (Unstable). Skipping for now.", "steamAppID: $steamAppID")
                continue
            }


            game = game.copy(igdbId = (igdbResponseObj.get("game") as Integer).toLong())
            // Now that you have the igdbID, make another API call to get info on the game.
            igdbResponse = IGDBSource.makeAPICall(
                "games",
                "fields summary,name,genres; where id = ${game.igdbId};")
            igdbResponseObj = igdbResponse.get(0) as JSONObject
            if (!igdbResponseObj.has("summary") ||
                !igdbResponseObj.has("name")
                ) { // Note: Some games don't have genre for whatever reason. Peggle Deluxe doesn't, for example.
                Log.w("Specific attribute not received from the response:", "$igdbResponse")
                continue
            } // Add the info to the Game object

            // add Description, Title
            game = game.copy(description = igdbResponseObj.get("summary") as String)
            game = game.copy(title = igdbResponseObj.get("name") as String)
            game = game.copy(sortingName = getSortingName(game.title))
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
                "fields image_id; where game = ${game.igdbId};")
            igdbResponseObj = igdbResponse.get(0) as JSONObject
            if (!igdbResponseObj.has("image_id")
            ) {
                Log.w("image_id not received from the response. Skipping adding image:", "$igdbResponse")
            } else {
                var imageId = igdbResponseObj.get("image_id")
                game = game.copy(imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/${imageId}.jpg")
            }

            // Add PC (because it's Steam!)
            game = game.copy(platform = game.platform.plus("PC"))
            game = game.copy(source = game.source.plus(libraryName))
            // Add features to this in the future perhaps, but otherwise you are done!
            emit(game)
        }

    }
}

