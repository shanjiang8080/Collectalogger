package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import com.example.collectalogger2.BuildConfig
import org.json.JSONArray
import org.json.JSONObject


/**
 * The bare minimum, you must get the steam library working.
 *
 */
class SteamDataSource(override var userId: String) : RemoteLibraryDataSource() {
    // Something that you really should do once you get basic functionality working
    // is to add caching (for already imported games) so you don't make calls to IGDB unnecessarily.
    override suspend fun getGames(): Flow<Game> {
        TODO("Not implemented")
        // so what you do, is that you gotta use the API-key
        // perhaps use a singleton in the future, but it seems fine for now
        // gotta close it when you're done though
        val client = HttpClient(Android)
        val response: String = client.get("http://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=${BuildConfig.STEAM_API_KEY}&input_json={\"steamid\":${userId}}")
        // check that it doesn't give a 429 error
        if (response[0] == '<' && response.contains("429")) {
            error("Received a 429 Too Many Requests error.")
        }
        // if not, parse it as JSON
        val jsonObject = JSONObject(response)
        // assert that it has a list of games
        if (!jsonObject.has("games")) {
            error("There are no games in the JSON response.")
        }
        val responseJSON: JSONObject = jsonObject.get("response") as JSONObject
        val games = responseJSON.get("games") as JSONArray
        // this has many games
        for (i in 0 until games.length()) {
            val game = games.getJSONObject(i)
            // for each game, get the appID, playtime. "Last Played" can come later.
            var steamAppID: Long = game.get("appid") as Long
            var playtime: Long = game.get("playtime_forever") as Long
        }

        client.close()
    }
}