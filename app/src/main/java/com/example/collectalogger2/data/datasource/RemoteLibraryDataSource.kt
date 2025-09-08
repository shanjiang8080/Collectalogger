package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.IGDBSource
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * These classes are emitted by the getGames methods.
 * - GameLoaded contains a game
 * - ExpectedGamesCount tells the caller the expected number of games the method should return
 * This can be used in combination with gameLoaded accumulation for a loading bar
 * Ideally, this is sent once.
 * - FinishGamesCount tells the caller that all games have been imported
 * - ListNonImportedGames tells the caller a list of non-imported games, if any
 */
sealed class GameEvent {
    data class GameLoaded(val game: Game) : GameEvent()
    data class ExpectedGamesCount(val count: Int) : GameEvent()
    object FinishGamesCount : GameEvent()
    data class ListNonImportedGames(val games: List<Game>) : GameEvent()
}


interface HasLibraryName {
    val name: String
}

/**
 * The base class that all libraries (Steam, Epic, etc) derive from.
 * Other methods may be added later, like achievements. Or not! I dunno about architecture.
 */

abstract class RemoteLibraryDataSource(val gameDao: GameDao) {
    // So like, Steam, or Epic, or GOG, or whatever.
    abstract var libraryName: String
    /**
     * This returns a flow of games.
     * Does not add the games to the database automatically.
     * - forceUpdate: If true, the response will re-import existing database games.
     */
    abstract suspend fun getGames(forceUpdate: Boolean = false): Flow<GameEvent>

    /**
     * Given a game and a game with a store-specific ID, it returns the game with the ID.
     */
    abstract fun copyWithID(game: Game, gameWithId: Game): Game

    /**
     * This method takes in a String and returns the String without
     * "The", "An", or "A" at the beginning, if there is one.
     */
    internal fun getSortingName(name: String): String {
        var nameWords = name.split(" ")
        // don't get rid of the whole thing!
        if (nameWords.size <= 1) return name
        if (nameWords[0].lowercase() in setOf("a", "an", "the"))
            return nameWords.drop(1).joinToString(" ")
        return name
    }

    /**
     * This method makes the central call to IGDB, given a list of identifiers.
     * This gets APIs 500 games at a time, though `games` can be unlimited in length.
     * Returns updated games.
     * gameIdentifiers: a map of identifiers (ID, URL, etc) for each game. The value is a pair with playtime and store-specific ID.
     * NOTE THAT FOR GAMEIDENTIFIERS, THE KEYS MUST BE ENCLOSED IN QUOTES
     * identifierIdMap: a map with keys identical to gameIdentifier keys but with values of the library ID.
     * identifierName: The name of the identifier in gameIdentifiers
     * endpoint: an endpoint for IGDB. can be games, websites, etc
     * gamePrefix: a prefix depending on the endpoint. MUST END WITH A PERIOD IF NOT BLANK
     * (Ex: For Steam it'd be URL)
     * customField: the custom field to get the games out of the map
     * includeUpdates: if the response should get updates and various other stuffs
     * gameJSONPath: A lambda returning the relevant object from the initial response.
     * platform: a string telling the platform it is. Default is PC.
     */
    internal suspend fun callIGDB(
        gameIdentifiers: Map<String, Pair<Long, String>>,
        identifierName: String,
        endpoint: String,
        gamePrefix: String,
        customField: String,
        customFieldLogic: ((JSONObject) -> String)? = null,
        customFieldIsGameAttribute: Boolean = false,
        otherGameFilter: String = "",
        gameJSONPath: (json: JSONObject) -> JSONObject,
        includeUpdates: Boolean = false,
        platform: String = "PC"): List<Game> {
        var gameList = mutableListOf<Game>()
        bulkCall(gameIdentifiers, 500) { gamesSnippet ->
            var updateParam = if (includeUpdates) {
                if (endpoint != "games") "& game.game_type != (1, 5, 13, 2)"
                else "& game_type != (1, 5, 13, 2)"
            } else {
                if (endpoint != "games") "& game.parent_game = null"
                else "& parent_game = null"
            }
            var request =
                """
                    fields
                    ${if (customFieldIsGameAttribute && endpoint != "games") "game.$customField," else "${customField},"}
                    ${gamePrefix}id,
                    ${gamePrefix}summary, 
                    ${gamePrefix}name,
                    ${gamePrefix}websites.type.type,
                    ${gamePrefix}parent_game,
                    ${gamePrefix}genres.name, 
                    ${gamePrefix}artworks.image_id, 
                    ${gamePrefix}screenshots.image_id, 
                    ${gamePrefix}involved_companies.company.name, 
                    ${gamePrefix}involved_companies.developer, 
                    ${gamePrefix}involved_companies.publisher, 
                    ${gamePrefix}cover.image_id;
                    where $identifierName = (${gamesSnippet.keys.joinToString(separator = ",") { "\"$it\""}}) $updateParam
                    ${if (!otherGameFilter.isEmpty()) "& $otherGameFilter" else ""};
                    limit 500;
                """.trimIndent()
            Log.d("IGDBParser", request)
            var igdbResponse = IGDBSource.makeAPICall(
                endpoint,
                request
            )
            Log.d("IGDBParser", "Response length: ${igdbResponse.length()}")
            for (i in 0 until igdbResponse.length()) {
                var igdbResponseObj = gameJSONPath(igdbResponse.get(i) as JSONObject)
                if (!igdbResponseObj.has("name")) {
                    Log.w("IGDBParser", "Missing name in response: $igdbResponse")
                    continue
                }
                // Add the info to the Game object
                var customFieldInstance: String
                if (customFieldLogic == null) {
                    customFieldInstance = if (customFieldIsGameAttribute && endpoint != "games") {
                        if (customField != "") igdbResponseObj.getString(customField) else ""
                    } else {
                        if (customField != "") igdbResponse.getJSONObject(i)
                            .getString(customField) else ""
                    }
                } else {
                    customFieldInstance = customFieldLogic(igdbResponseObj)
                }

                // Add value pair objects
                var playTime = 0L
                var localId = ""
                if (customFieldInstance != "") {
                    if (gamesSnippet[customFieldInstance] != null) {
                        playTime = (gamesSnippet[customFieldInstance] as Pair<Long, String>).first
                        localId = (gamesSnippet[customFieldInstance] as Pair<Long, String>).second
                    }
                    else {
                        Log.w("IGDBParser", "gamesSnippet doesn't have the customFieldInstance!")
                    }
                }


                var title = igdbResponseObj.getString("name")
                var igdbId = igdbResponseObj.getLong("id")
                var description = if (igdbResponseObj.has("summary")) igdbResponseObj.getString("summary") else ""
                var cover = if (igdbResponseObj.has("cover")) igdbResponseObj.get("cover") as JSONObject else null
                var artworks = if (igdbResponseObj.has("artworks")) igdbResponseObj.get("artworks") as JSONArray else null
                var screenshots = if (igdbResponseObj.has("screenshots")) igdbResponseObj.get("screenshots") as JSONArray else null
                var involvedCompanies = if (igdbResponseObj.has("involved_companies")) igdbResponseObj.get("involved_companies") as JSONArray else null

                // get screenshots
                var screenshotList: MutableList<String> = mutableListOf()
                for (i in 0 until minOf(screenshots?.length() ?: 0, 5)) {
                    var screenshot = screenshots!![i] as JSONObject
                    screenshotList.add("https://images.igdb.com/igdb/image/upload/t_720p/${screenshot.get("image_id")}.jpg")
                }

                // get genres
                var genreList = if (igdbResponseObj.has("genres")) igdbResponseObj.getJSONArray("genres") else null
                var genres: MutableSet<Int> = mutableSetOf()
                if (genreList != null) {
                    for (i in 0 until genreList.length()) {
                        var genre = genreList[i] as JSONObject
                        if (genre.has("name")) genres.add(genre.getInt("id"))
                    }
                }

                // get the developers/publishers
                var developers: MutableList<String> = mutableListOf()
                var publishers: MutableList<String> = mutableListOf()
                for (i in 0 until (involvedCompanies?.length() ?: 0)) {
                    var company = involvedCompanies!![i] as JSONObject
                    var companyName = (company.get("company") as JSONObject).getString("name")
                    if (company.getBoolean("developer")) developers.add(companyName)
                    if (company.getBoolean("publisher")) publishers.add(companyName)
                }


                // Combine everything together
                val game = Game(
                    description = description,
                    title = title,
                    playTime = playTime,
                    sortingName = getSortingName(title),
                    imageUrl = if (cover != null) "https://images.igdb.com/igdb/image/upload/t_cover_big/${cover.get("image_id")}.jpg" else "",
                    platform = setOf(platform),
                    source = setOf(libraryName),
                    igdbId = igdbId,
                    backgroundUrl = if (artworks != null && artworks.length() > 0) "https://images.igdb.com/igdb/image/upload/t_720p/${(artworks[0] as JSONObject).getString("image_id")}.jpg" else "",
                    screenshots = screenshotList,
                    genre = genres,
                    developers = developers.toSet(),
                    publishers = publishers.toSet()
                )
                gameList.add(addToSourceLibrary(game, localId))
            }

        }
        return gameList
    }

    /**
     * This abstract method copies the game passed in and modifies it to pass in the localId.
     */
    internal abstract fun addToSourceLibrary(game: Game, localId: String): Game
}

/**
 * This function executes your action on an entire map, but does it up to the limit at a time
 */
suspend fun bulkCall(games: Map<String, Any>, groupBy: Int = 10, action: suspend (gamesSnippet: Map<String, Any>) -> Unit) {
    val gamesMap: MutableMap<String, Any> = games.toMutableMap()
    while (gamesMap.isNotEmpty()) {
        // Get the number of games left
        var gamesLeftClamped = minOf(groupBy, gamesMap.size)
        // Get the keys/items for this iteration
        var iterationKeys = gamesMap.keys.toList().subList(0, gamesLeftClamped)
        var iterationItems = gamesMap.toList().subList(0, gamesLeftClamped)
        // Give the items to the action as a map
        action(iterationItems.toMap())
        // afterwards, remove the games from the map then
        iterationKeys.forEach(gamesMap::remove)
    }
}



