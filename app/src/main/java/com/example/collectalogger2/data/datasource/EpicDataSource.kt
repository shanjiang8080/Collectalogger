package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.EpicSource
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

        // Get the full map of items, where key is slug and value is the playtimes.
        var igdbCallMap = mutableMapOf<String, Pair<Long, String>>()
        // This map is for games that don't get caught by the bulk slug thing.
        var slugNamespaceCatalogItemIdMap = mutableMapOf<String, Pair<String, String>>()
        // Note that appName from items == artifactId from playtimes
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

                artifactSlugMap[entry.getString("appName")] = gameSlug
                slugNamespaceCatalogItemIdMap[gameSlug] = entry.getString("namespace") to entry.getString("catalogItemId")
                // You need a placeholder because if not it won't get imported
                igdbCallMap[gameSlug] = 0L to convertEpicIdToString(entry.getString("namespace"), entry.getString("catalogItemId"))
            }
        } while (cursor != "") // Loop until cursor is gone

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

        // Get the playtimes and map it to the igdbCallMap
        for (ii in 0 until playtimeResponse.length()) {
            val obj = playtimeResponse.getJSONObject(ii)
            var slug = artifactSlugMap[obj.getString("artifactId")] ?: ""
            // Check if the game has been added to IGDB already
            var epicItem = slugNamespaceCatalogItemIdMap[slug]
            if (epicItem == null)
            {
                continue
            }
            var epicId = "${epicItem.first} ${epicItem.second}"
            var epicGame = gameDao.getGameByEpicId(epicId)
            // If already in the database, skip (but update playtime)
            if (!forceUpdate && epicGame != null) {
                // Emit the game, updating the playtime
                var modifiedEpicGame = epicGame.copy(
                    platform = epicGame.platform.plus("PC"),
                    playTime = maxOf(epicGame.playTime, obj.getLong("totalTime") / 60)
                )

                if (modifiedEpicGame != epicGame)
                    emit(modifiedEpicGame)
                continue
            }
            // Otherwise, add the callMap
            igdbCallMap[slug] = obj.getLong("totalTime") / 60 to epicId

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
                emit(game)
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
        for (i in 0 until leftList.size) {
            var key = leftList[i]
            var pair = slugNamespaceCatalogItemIdMap[key] as Pair
            var epicIdString = convertEpicIdToString(pair.first, pair.second)
            var playtime = igdbCallMap[key]!!.first
            // Remove the game with the epicIdString from the database, if it exists
            // Since it has a bad title like "Lemon" or something
            if (key == "Lemon") {
                Log.d("EpicDataSource", "what")
            }


            // For each remaining game, make an API call to items.
            var catalogResponseRaw = (EpicSource.makeAPICall(
                domain = "catalog-public-service-prod06.ol.epicgames.com",
                path = "catalog/api/shared/namespace/${pair.first}/bulk/items?id=${pair.second}&country=US&locale=en-US&includeMainGameDetails=true",
                isGet = true,
                headerss = mapOf("Authorization" to "${userJson.get("token_type")} ${userJson.get("access_token")}"),
                params = mapOf(),
                bodyParams = mapOf()
            ) as JSONObject)
            var catalogResponse = catalogResponseRaw.get(pair.second) as JSONObject
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
            var sanitizedSlug = getSlug(catalogResponse.getString("title").replace("\"", ""))
            newMap[sanitizedTitle] = playtime to epicIdString
            newMapSlugs[sanitizedSlug] = playtime to epicIdString
        }


        // Call again with the newMap
        // Don't filter libraries this time
        Log.d("EpicDataSource", "Making bulk IGDB call for fallbacks")
        callIGDB(
            gameIdentifiers = newMap,
            identifierName = "game.name",
            endpoint = "external_games",
            gamePrefix = "game.",
            customField = "name",
            customFieldIsGameAttribute = true,
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(game)
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
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(game)
                var slug = getSlug(game.title)
                if (slug in newMap) {
                    newMapSlugs.remove(slug)
                }
                duplicateSet.add(game.igdbId)
            }
        }


        // Stop it there (for now, add more in the future probably)
        // But then log the remaining items

        newMapSlugs.forEach { what ->
            Log.w("EpicDataSource", "Not imported: $what")
        }



    }
    private fun convertEpicIdToString(namespace: String, catalogItemId: String): String {
        return "$namespace $catalogItemId"
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        if (localId == "") {
            Log.w("EpicDataSource", "localId is null! What the fuck!")
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