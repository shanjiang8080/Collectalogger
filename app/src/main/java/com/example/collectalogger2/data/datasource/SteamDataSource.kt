package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import com.example.collectalogger2.BuildConfig
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import com.example.collectalogger2.util.IGDBSource
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject


/**
 * The bare minimum, you must get the steam library working.
 *
 */
class SteamDataSource(override var userId: String, gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = "Steam"

    override suspend fun getGames(forceUpdate: Boolean): Flow<Game> = flow {
        // so what you do, is that you gotta use the API-key
        val client = HttpClient(Android)
        val response: HttpResponse = client.get("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=${BuildConfig.STEAM_API_KEY}&input_json={\"steamid\":${userId}}")
        client.close()
        var responseText = response.bodyAsText()
        // check that it doesn't give a 429 error
        if (responseText[0] == '<' && responseText.contains("429")) {
            throw APIStatusException("Too Many Requests", 429)
        }
        if (!responseText.contains("games")) {
            throw APIException("No games in the JSON, instead the body is $responseText")
        }
        // if not, parse it as JSON
        val jsonObject = JSONObject(responseText)
        // assert that it has a list of games
        Log.i("Steam games fetched!", responseText)
        val responseJSON: JSONObject = jsonObject.get("response") as JSONObject
        val games = responseJSON.get("games") as JSONArray
        // this has many games
        for (i in 0 until games.length()) {
            val apiGame = games.getJSONObject(i)

            var steamAppID: Long = (apiGame.get("appid") as Integer).toLong()
            // Quick check: If the Steam version of the game exists in the database already,
            // skip.
            if (!forceUpdate && gameDao.getGameBySteamId(steamAppID) != null) continue

            // Make a new Game object to store the information
            val game = Game()
            // for each game, get the appID, playtime. "Last Played" can come later.
            game.playTime = (apiGame.get("playtime_forever") as Integer).toLong()

            // add the app ID to the game's SteamId
            game.steamId = steamAppID

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


            game.igdbId = (igdbResponseObj.get("game") as Integer).toLong()
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
            game.description = igdbResponseObj.get("summary") as String
            game.title = igdbResponseObj.get("name") as String
            game.sortingName = getSortingName(game.title)
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
                game.imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/${imageId}.jpg"
            }

            // Add PC (because it's Steam!)
            game.platform.plus("PC")
            game.source.plus(libraryName)
            // Add features to this in the future perhaps, but otherwise you are done!
            emit(game)
        }

    }
}

