package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.data.datasource.GameEvent.IncrementGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.ListNonImportedGames
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.AccountExpiryException
import com.example.collectalogger2.util.EpicSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

private class EpicGame(
    val appName: String,
    val namespace: String,
    val catalogItemId: String,
    var playTime: Long = 0
) {
    override fun toString(): String {
        return "$appName $namespace $catalogItemId $playTime"
    }
}

class EpicDataSource(
    var userInfoFlow: Flow<String>,
    var userInfoSetter: suspend (String) -> Unit,
    gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "Epic Games"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        val userInfo = userInfoFlow.first()
        if (userInfo == "") throw AccountException("User is not logged into Epic Games!", libraryName)
        // sets JSON to the thing
        var userJson = refreshLogin(JSONObject(userInfo))
        // after refreshing JSON if applicable, get the list of all games, then iterate through and emit.

        // Map of the sandbox name to the appName + namespace + artifactItemId
        var duplicateGamesMap: MutableMap<String, MutableList<EpicGame>> = mutableMapOf()
        // Map for duplicate games linking appNames to slugs
        var duplicateSlugsMap: MutableMap<String, String> = mutableMapOf()

        // Get the full map of items, where key is slug and value is the playtimes.
        var igdbCallMap = mutableMapOf<String, Pair<Long, String>>()
        // This map is for games that don't get caught by the bulk slug thing.
        var slugNamespaceCatalogItemIdMap = mutableMapOf<String, Pair<String, String>>()
        // Note that appName from items == artifactId from playtimes
        // Used for linking game to playtime
        // It is the artifactId to name
        var artifactSlugMap = mutableMapOf<String, String>()
        var cursor = ""
        do {
            var params = mapOf(
                "includeMetadata" to "true",
                "includeCategories" to "applications",
            )
            if (cursor != "") params = params.plus("cursor" to cursor)
            var itemsResponse = EpicSource.makeAPICall(
                domain = "library-service.live.use1a.on.epicgames.com",
                path = "library/api/public/items",
                isGet = true,
                headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
                params = params,
                bodyParams = mapOf(),
                isJsonArray = false
            ) as JSONObject
            // Get metadata and fill/unfill cursor depending on response
            var metadata = itemsResponse.getJSONObject("responseMetadata")
            if (metadata.has("nextCursor"))
                cursor = metadata.getString("nextCursor")
            else
                cursor = ""
            // get the "records" from the object, which is a JSONArray
            var records = itemsResponse.getJSONArray("records")
            for (i in 0 until records.length()) {
                var entry = records.getJSONObject(i)
                var gameSlug = getSlug(entry.getString("sandboxName"))

                var namespace = entry.getString("namespace")
                var catalogItemId = entry.getString("catalogItemId")
                var appName = entry.getString("appName")

                // HACK: make live games work
                if (gameSlug == "live") {
                    var tempSlug = "$gameSlug$catalogItemId"
                    artifactSlugMap[appName] = tempSlug
                    slugNamespaceCatalogItemIdMap[tempSlug] = namespace to catalogItemId
                    // You need a placeholder because if not it won't get imported
                    igdbCallMap[tempSlug] = 0L to convertEpicIdToString(namespace, catalogItemId)
                } else {
                    // Check if the gameSlug has been seen before
                    // If so, defer it until the next pass
                    // If it's Live, skip as the namespaces differ and it can handle it already
                    if (gameSlug in igdbCallMap || gameSlug in duplicateGamesMap) {
                        Log.d(
                            "EpicDataSource",
                            "Game with slug $gameSlug and id $namespace $catalogItemId is duplicate"
                        )
                        // If the slug is in the call map, remove the existing entries
                        // also remove the entries from the namespaceCatalogItemIdMap
                        // and artifactSlugMap
                        if (gameSlug in igdbCallMap) {
                            // Remove existing game from existing maps
                            var pair = slugNamespaceCatalogItemIdMap.remove(gameSlug)
                            var name = artifactSlugMap.entries.find { it.value == gameSlug }?.key
                            igdbCallMap.remove(gameSlug)
                            artifactSlugMap.remove(name)
                            if (pair != null && name != null) {
                                Log.d(
                                    "EpicDataSource",
                                    "Previous game with $gameSlug, appName $name and id ${pair.first} ${pair.second} removed from map"
                                )

                                // add existingGame to duplicate maps
                                EpicGame(name, pair.first, pair.second).let { oldGame ->
                                    if (gameSlug !in duplicateGamesMap) {
                                        duplicateGamesMap[gameSlug] = mutableListOf(oldGame)
                                    } else {
                                        duplicateGamesMap[gameSlug]!!.add(oldGame)
                                    }
                                }
                                duplicateSlugsMap[name] = gameSlug

                            } else {
                                Log.w(
                                    "EpicDataSource",
                                    "Game with slug $gameSlug has a null pair or appName! Pair: $pair, appName: $name"
                                )
                            }
                        }
                        // Add the dupe game to the duplicateGamesMap
                        EpicGame(appName, namespace, catalogItemId).let { dupeGame ->
                            if (gameSlug !in duplicateGamesMap) {
                                duplicateGamesMap[gameSlug] = mutableListOf(dupeGame)
                            } else {
                                duplicateGamesMap[gameSlug]!!.add(dupeGame)
                            }
                        }

                        continue
                    }
                    artifactSlugMap[appName] = gameSlug
                    slugNamespaceCatalogItemIdMap[gameSlug] = namespace to catalogItemId
                    // You need a placeholder because if not it won't get imported
                    igdbCallMap[gameSlug] = 0L to convertEpicIdToString(namespace, catalogItemId)

                }
            }
        } while (cursor != "") // Loop until cursor is gone

        // Now, invoke request with the playtime URL and tokens.
        var playtimeResponse = EpicSource.makeAPICall(
            domain = "library-service.live.use1a.on.epicgames.com",
            path = "library/api/public/playtime/account/${userJson.get("account_id")}/all",
            isGet = true,
            headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
            params = mapOf(),
            bodyParams = mapOf(),
            isJsonArray = true
        ) as JSONArray

        // Once you've gotten all games, emit a ExpectedGamesCount event
        // Note that this is approximate because of DLCs and such.
        emit(ExpectedGamesCount(artifactSlugMap.size))

        // Get the playtimes and map it to the igdbCallMap
        for (ii in 0 until playtimeResponse.length()) {
            val obj = playtimeResponse.getJSONObject(ii)
            val playTime = obj.getLong("totalTime") / 60
            val appName = obj.getString("artifactId")
            var slug = artifactSlugMap[appName] ?: ""
            // If the slug is empty, then the appName is not present in the artifactSlugMap,
            // meaning it's a duplicate
            if (slug == "") {
                // Check if the obj is present in the duplicate map
                // but that needs the appName (items list), or the artifactId (this)
                // so i need to get the app name
                // currently the duplicate map is a slug -> list<namespace, itemId>
                // so i need an app name -> slug
                var newSlug = duplicateSlugsMap[appName]
                var dupeList = duplicateGamesMap[newSlug]
                var matchingGames = dupeList?.filter { game: EpicGame ->
                    game.appName == appName
                }
                if (matchingGames == null) matchingGames = listOf()
                if (matchingGames.size > 1) {
                    Log.w(
                        "EpicDataSource",
                        "matchingGames > 1, which should not happen. matchingGames: $matchingGames"
                    )
                    // It shouldn't happen since appNames should be unique
                } else if (matchingGames.size == 1) {
                    var matchingGame = matchingGames[0]
                    matchingGame.playTime = playTime
                }
                // Even after, duplicates shouldn't be processed until their own pass
                continue
            }

            // Check if the game has been added to IGDB already
            var epicItem = slugNamespaceCatalogItemIdMap[slug]
            if (epicItem == null)
            {
                continue
            }
            var epicId = convertEpicIdToString(epicItem.first, epicItem.second)
            var epicGame = gameDao.getGameByEpicId(epicId)
            // If already in the database, skip (but update playtime)
            if (!forceUpdate && epicGame != null) {
                // Emit the game, updating the playtime
                var modifiedEpicGame = epicGame.copy(
                    platform = epicGame.platform.plus("PC"),
                    playTime = maxOf(epicGame.playTime, playTime)
                )

                if (modifiedEpicGame != epicGame)
                    emit(GameLoaded(modifiedEpicGame))

                // Now, remove it from the maps to prevent it updating again
                igdbCallMap.remove(slug)
                slugNamespaceCatalogItemIdMap.remove(slug)
                artifactSlugMap.remove(appName)
                duplicateGamesMap.remove(slug)
                duplicateSlugsMap.remove(appName)
                Log.d("EpicDataSource", "$slug skipped since it exists in the database")
                continue
            }
            // Otherwise, add the callMap
            igdbCallMap[slug] = playTime to epicId

        }

        // Now, once you've skipped duplicate entries for played games
        // Skip duplicate entries for unplayed games
        igdbCallMap.toMap().keys.forEach { key: String ->
            var pair = igdbCallMap[key]
            var epicId = pair!!.second
            var gameFromDb = gameDao.getGameByEpicId(epicId)
            if (!forceUpdate && gameFromDb != null) {
                // Remove it from the maps
                igdbCallMap.remove(key)
                slugNamespaceCatalogItemIdMap.remove(key)
                duplicateGamesMap.remove(key)
                Log.d(
                    "EpicDataSource",
                    "unplayed game $key skipped since it exists in the database"
                )
            }
        }


        var duplicateSet = mutableSetOf<Long>() // Contains game ids for duplicate handling

        Log.d("EpicDataSource", "Making bulk IGDB call")
        // Have the igdbCallMap
        callIGDB(
            gameIdentifiers = igdbCallMap,
            identifierName = "game.slug",
            endpoint = "external_games",
            gamePrefix = "game.",
            otherGameFilter = "external_game_source = 26",
            customField = "slug",
            customFieldIsGameAttribute = true,
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(GameLoaded(game))
                var slug = getSlug(game.title)
                if (slug in slugNamespaceCatalogItemIdMap) {
                    slugNamespaceCatalogItemIdMap.remove(slug)
                } else {
                    Log.d("EpicDataSource", "Slug $slug not removed from slugNamespaceCatalogItemIdMap!")
                }
                duplicateSet.add(game.igdbId)
            }
        }
        // Now, there are a few games left that haven't been caught by the response.
        // Loop through these and use the fallback approach (with Catalog endpoint).
        // But still, make as few IGDB calls as possible.
        var leftList = slugNamespaceCatalogItemIdMap.keys.toList()
        var newMap = mutableMapOf<String, Pair<Long, String>>()
        var newMapSlugs = mutableMapOf<String, Pair<Long, String>>()


        // Now, loop through the duplicates and use the Catalog endpoint to fill info.
        var duplicateLeftList = duplicateGamesMap.keys.toList()
        // like with the newMap, this contains found games with slugs paired to playtime and ID
        for (i in 0 until duplicateLeftList.size) {
            // Key is the slug
            var key = duplicateLeftList[i]
            var games = duplicateGamesMap[key]
            if (games != null) {
                for (j in 0 until games.size) {
                    var game = games[j]
                    var epicId = convertEpicIdToString(game.namespace, game.catalogItemId)
                    var existingGame = gameDao.getGameByEpicId(epicId)
                    if (existingGame != null) {
                        if (game.playTime != existingGame.playTime) {
                            emit(GameLoaded(existingGame.copy(playTime = game.playTime)))
                        }
                        Log.d("EpicDataSource", "game $key skipped since it exists in the database")
                        continue
                    }
                    // Make a request to gather info about the game with the API call.
                    var catalogResponseRaw = EpicSource.makeAPICall(
                        domain = "catalog-public-service-prod06.ol.epicgames.com",
                        path = "catalog/api/shared/namespace/${game.namespace}/bulk/items?id=${game.catalogItemId}&country=US&locale=en-US&includeMainGameDetails=true",
                        isGet = true,
                        headerss = mapOf(
                            "Authorization" to "${userJson.get("token_type")} ${
                                userJson.get(
                                    "access_token"
                                )
                            }"
                        ),
                        params = mapOf(),
                        bodyParams = mapOf()
                    ) as JSONObject
                    // emit an increment games count
                    emit(IncrementGamesCount)

                    var catalogResponse = catalogResponseRaw.get(game.catalogItemId) as JSONObject
                    // filter out DLCs
                    if (isGameExtra(catalogResponse)) continue
                    // since it isn't, add to the duplicate games map
                    // but with the slugs as the detailed name
                    var sanitizedTitle = catalogResponse.getString("title").replace("\"", "\\\"")
                    var sanitizedSlug =
                        getSlug(catalogResponse.getString("title").replace("\"", ""))
                    // add to both maps
                    Log.d("EpicDataSource", "SANITIZED_TITLE: $sanitizedTitle")

                    (game.playTime to epicId).let {
                        newMap[sanitizedTitle] = it
                        newMapSlugs[sanitizedSlug] = it
                    }
                }
            }

        }

        for (i in 0 until leftList.size) {
            var key = leftList[i]
            var pair = slugNamespaceCatalogItemIdMap[key] as Pair
            var epicIdString = convertEpicIdToString(pair.first, pair.second)
            var playtime = igdbCallMap[key]!!.first

            // For each remaining game, make an API call to items.
            var catalogResponseRaw = (EpicSource.makeAPICall(
                domain = "catalog-public-service-prod06.ol.epicgames.com",
                path = "catalog/api/shared/namespace/${pair.first}/bulk/items?id=${pair.second}&country=US&locale=en-US&includeMainGameDetails=true",
                isGet = true,
                headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
                params = mapOf(),
                bodyParams = mapOf()
            ) as JSONObject)
            // emit an increment games count
            emit(IncrementGamesCount)

            var catalogResponse = catalogResponseRaw.get(pair.second) as JSONObject

            if (isGameExtra(catalogResponse)) continue

            var sanitizedTitle = catalogResponse.getString("title").replace("\"", "\\\"")
            var sanitizedSlug = getSlug(catalogResponse.getString("title").replace("\"", ""))
            newMap[sanitizedTitle] = playtime to epicIdString
            newMapSlugs[sanitizedSlug] = playtime to epicIdString
        }


        // Call again with the newMap
        // Don't filter libraries this time
        Log.d("EpicDataSource", "Making bulk IGDB call for fallbacks")
        callIGDB(
            gameIdentifiers = newMap,
            identifierName = "name",
            endpoint = "games",
            gamePrefix = "",
            customField = "name",
            otherGameFilter = "websites.type.type = \"Epic\"",
            includeUpdates = true,
            gameJSONPath = { json -> return@callIGDB json }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(GameLoaded(game, false))
                var title = game.title
                if (title in newMap) {
                    newMap.remove(title)
                    newMapSlugs.remove(getSlug(title))
                }
                duplicateSet.add(game.igdbId)
            }
        }


        // Make a new bulk call for slugs

        Log.d("EpicDataSource", "Making bulk IGDB call for fallbacks with slugs")
        callIGDB(
            gameIdentifiers = newMapSlugs,
            identifierName = "game.slug",
            endpoint = "external_games",
            gamePrefix = "game.",
            customField = "slug",
            customFieldIsGameAttribute = true,
            includeUpdates = true,
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(GameLoaded(game))
                var slug = getSlug(game.title)
                if (slug in newMapSlugs) {
                    newMapSlugs.remove(slug)
                    newMap.remove(game.title)
                }
                duplicateSet.add(game.igdbId)
            }
        }


        // Now, use alternative names as an additional fallback

        // Remove all names filtered out in the last pass
        newMap = newMap.filter { name -> getSlug(name.key) in newMapSlugs }.toMutableMap()
        // contains keys caught by alternative names, will be subtracted with newMap
        var newKeys = mutableListOf<String>()

        Log.d("EpicDataSource", "Making bulk IGDB call for fallbacks with alternative names")
        callIGDB(
            gameIdentifiers = newMap,
            identifierName = "alternative_names.name",
            endpoint = "games",
            gamePrefix = "",
            customField = "alternative_names.name",
            customFieldLogic = { jsonObj ->
                var array = jsonObj.getJSONArray("alternative_names")
                for (i in 0 until array.length()) {
                    var altName = array.getJSONObject(i)
                    var name = altName.getString("name")
                    // If the alternative name is found, add to newKeys and place into
                    if (name in newMap.keys) {
                        newKeys.add(name)
                        return@callIGDB name
                    }
                }
                Log.w(
                    "EpicDataSource",
                    "custom field logic returned blank string, which should not happen"
                )
                return@callIGDB ""
            },
            customFieldIsGameAttribute = false,
            gameJSONPath = { json -> return@callIGDB json },
            includeUpdates = true,
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(GameLoaded(game))
            }
        }
        // emit a FinishGamesCount
        emit(FinishGamesCount)

        // Log not imported games (often unstable/beta versions)
        val missingGames = newMap.minus(newKeys)

        // emit non-imported games
        // Turn non-imported game strings into temporary game objects with the names of the games
        // and their specific IDs
        val newMissingGames = mutableListOf<Game>()
        missingGames.forEach { game ->
            newMissingGames.add(
                Game(
                    title = game.key,
                    epicId = game.value.second,
                    playTime = game.value.first
                )
            )
        }
        emit(ListNonImportedGames(newMissingGames))


    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(epicId = gameWithId.epicId)
    }

    private fun convertEpicIdToString(namespace: String, catalogItemId: String): String {
        return "$namespace $catalogItemId"
    }

    private fun isGameExtra(catalogResponse: JSONObject): Boolean {
        // sift out DLCs and such: parse through categories
        var categories = catalogResponse.get("categories") as JSONArray
        var categoriesList = mutableListOf<String>()
        for (j in 0 until categories.length()) {
            var categoryObj = categories.get(j) as JSONObject
            categoriesList.add(categoryObj.getString("path"))
        }
        if (!categoriesList.contains("applications")) {
            return true
        }
        if (catalogResponse.has("mainGameItem")) {
            return true
        }
        if ( // if it has a category that doesn't match with a game, skip
            categoriesList.contains("software") ||
            categoriesList.contains("digitalextras") ||
            categoriesList.contains("plugins") ||
            categoriesList.contains("plugins/engine") ||
            categoriesList.contains("addons")
        ) {
            return true
        }
        val platforms =
            catalogResponse.getJSONArray("releaseInfo").getJSONObject(0).getJSONArray("platform")
        val platformString = platforms.join("").lowercase()
        // if it's Android or iOS, skip
        if (
            platformString.contains("android") ||
            platformString.contains("ios")
        ) {
            return true
        }
        return false
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        if (localId == "") {
            Log.w("EpicDataSource", "localId is null!")
            throw Exception()
        }
        return game.copy(epicId = localId)
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
            throw AccountExpiryException(
                "Cannot refresh login! Must log in again to Epic Games!",
                libraryName
            )
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

private fun getSlug(name: String): String {
    var newName = name
        .lowercase()
        .replace(" ", "-")
        .replace(":", "")
        .replace(",", "")
        .replace(".", "")
        .replace("\"", "")
        .replace("'", "")
        .replace("’", "")
        .replace("‘", "")
        .replace("“", "")
        .replace("”", "")
        .replace("™", "")
        .replace("®", "")
        .replace("!", "")
        .replace("_", "-")
        .replace("–", "-")
        .replace(">", "")
        .replace("<", "")
        .replace("---", "-")
        .replace("--", "-") // helps with game of the year editions
    if (newName.endsWith("_")) newName = newName.substring(0, newName.length - 1)
    return newName
}