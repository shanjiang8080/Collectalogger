package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.data.datasource.GameEvent.ListNonImportedGames
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.AccountExpiryException
import com.example.collectalogger2.util.libraryObjects.NintendoSessionExpiredException
import com.example.collectalogger2.util.libraryObjects.NintendoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Imports the user's Nintendo Switch (and Switch 2) games through the
 * Nintendo Account Virtual Game Cards portal, based on the flow of Playnite's
 * NintendoLibrary extension (https://github.com/XenorPLxx/playnite-library-nintendo):
 * fetch the account's owned virtual game cards, then match them to IGDB games
 * by name. Nintendo provides no playtime through this surface, so playTime is
 * always 0.
 *
 * Coverage note: the portal lists virtual game cards, which digital purchases
 * issue automatically and physical cartridges get through Nintendo's VGC
 * flow, so the imported list follows that, not every owned cartridge.
 */
class NintendoDataSource(
    var userInfoFlow: Flow<String>,
    var userInfoSetter: suspend (String) -> Unit,
    gameDao: GameDao
) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "Nintendo"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        val cookieInfo = userInfoFlow.first()
        if (cookieInfo == "")
            throw AccountException("User is not logged into the Nintendo Account portal!", name)

        // Fetch all of the account's virtual game cards with the saved cookies
        val titles = try {
            NintendoSource.getVirtualGameCards(cookieInfo)
        } catch (e: NintendoSessionExpiredException) {
            throw AccountExpiryException(
                "Cannot access Nintendo with the saved login! Must log in again to Nintendo!",
                libraryName
            )
        }

        // Skip add-on-only entries (DLC without a base game)
        val gamesToImport = titles.filterNot { NintendoSource.isAddOnOnly(it) }

        // Map of applicationId to cleaned title, used for IGDB matching.
        // Multiple virtual game cards can share one application id
        // (e.g. one per household member), so deduplicate by id.
        val idTitleMap = linkedMapOf<String, String>()
        // Platform sets per application id, from the cards' platform flags
        // ("Switch" and/or "Switch 2")
        val idPlatformMap = linkedMapOf<String, Set<String>>()
        gamesToImport.forEach { title ->
            idTitleMap.putIfAbsent(title.applicationId, NintendoSource.cleanTitle(title.name))
            idPlatformMap.putIfAbsent(title.applicationId, NintendoSource.platformsOf(title))
        }

        // Emit an expected games count for the loading bar
        emit(ExpectedGamesCount(idTitleMap.size))

        // Skip games that are already in the database (unless forcing an update)
        for ((nintendoId, _) in idTitleMap.toList()) {
            val existingGame = gameDao.getGameByNintendoId(nintendoId)
            if (!forceUpdate && existingGame != null) {
                val modifiedGame = existingGame.copy(
                    platform = existingGame.platform.plus(idPlatformMap[nintendoId] ?: setOf("Switch"))
                )
                if (modifiedGame != existingGame)
                    emit(GameLoaded(modifiedGame))
                idTitleMap.remove(nintendoId)
                Log.d("NintendoDataSource", "$nintendoId skipped since it exists in the database")
            }
        }

        // Nintendo can have multiple virtual game cards for a game that was
        // already imported under another library. When one of them has been
        // imported, match the leftover ones to the database game by title, so
        // they don't show up as non-imported forever.
        if (!forceUpdate) {
            val gamesByTitle = gameDao.getAllGames().associateBy { it.title.lowercase() }
            for ((nintendoId, title) in idTitleMap.toList()) {
                val existingGame = gamesByTitle[title.lowercase()] ?: continue
                val modifiedGame = existingGame.copy(
                    platform = existingGame.platform.plus(idPlatformMap[nintendoId] ?: setOf("Switch")),
                    // only bind this card if the database game has no Nintendo id yet
                    nintendoId = if (existingGame.nintendoId == "") nintendoId else existingGame.nintendoId
                )
                if (modifiedGame != existingGame)
                    emit(GameLoaded(modifiedGame))
                idTitleMap.remove(nintendoId)
            }
        }

        val duplicateSet = mutableSetOf<Long>() // Contains game ids for duplicate handling

        // Nintendo's games are not in IGDB's external_games database, so match
        // the remaining games by title, restricted to IGDB's Switch games.
        val fallbackMap = mutableMapOf<String, Pair<Long, String>>()
        idTitleMap.forEach { (nintendoId, title) ->
            fallbackMap[sanitizeTitle(title)] = 0L to nintendoId
        }
        if (fallbackMap.isNotEmpty()) {
            Log.d("NintendoDataSource", "Making bulk IGDB call with names")
            matchNames(fallbackMap, idPlatformMap).forEach { game ->
                emit(GameLoaded(game))
                idTitleMap.remove(game.nintendoId)
                duplicateSet.add(game.igdbId)
            }
        }

        // Retry pass: eShop titles sometimes come in ALL CAPS or odd casing
        // (e.g. "SUPER MARIO ODYSSEY"), which the exact name matching above
        // can miss. Retry the leftovers with title-cased names.
        val casingMap = mutableMapOf<String, Pair<Long, String>>()
        idTitleMap.forEach { (nintendoId, title) ->
            val titleCased = titleCase(title)
            if (titleCased != title) {
                casingMap[sanitizeTitle(titleCased)] = 0L to nintendoId
            }
        }
        if (casingMap.isNotEmpty()) {
            Log.d("NintendoDataSource", "Making bulk IGDB call with title-cased names")
            matchNames(casingMap, idPlatformMap).forEach { game ->
                if (game.igdbId !in duplicateSet) {
                    emit(GameLoaded(game))
                    idTitleMap.remove(game.nintendoId)
                    duplicateSet.add(game.igdbId)
                }
            }
        }

        // Emit a FinishGamesCount
        emit(FinishGamesCount)

        // Emit non-imported games with their names, IDs and platforms
        val newMissingGames = mutableListOf<Game>()
        idTitleMap.forEach { (nintendoId, title) ->
            newMissingGames.add(
                Game(
                    title = title,
                    nintendoId = nintendoId,
                    platform = idPlatformMap[nintendoId] ?: setOf("Switch")
                )
            )
        }
        if (newMissingGames.isNotEmpty()) {
            emit(ListNonImportedGames(newMissingGames))
        }
    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(nintendoId = gameWithId.nintendoId)
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        if (localId == "") {
            Log.w("NintendoDataSource", "localId is empty, game will have no Nintendo id!")
            return game
        }
        return game.copy(nintendoId = localId)
    }

    /**
     * Matches the given title map (sanitized title to (0, applicationId))
     * against IGDB's Switch games by name, applying the card's platform
     * flags to the results and dropping games seen in a previous pass.
     */
    private suspend fun matchNames(
        nameMap: Map<String, Pair<Long, String>>,
        idPlatformMap: Map<String, Set<String>>
    ): List<Game> {
        val matchedGames = mutableListOf<Game>()
        val duplicateSet = mutableSetOf<Long>()
        callIGDB(
            gameIdentifiers = nameMap,
            identifierName = "name",
            endpoint = "games",
            gamePrefix = "",
            customField = "name",
            customFieldIsGameAttribute = false,
            otherGameFilter = "platforms.name = (\"Nintendo Switch\", \"Nintendo Switch 2\")",
            includeUpdates = true,
            platform = "Switch",
            gameJSONPath = { json -> return@callIGDB json }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                // The card's platform flags are authoritative for the
                // user's copy ("Switch", "Switch 2", or both)
                matchedGames.add(
                    game.copy(platform = idPlatformMap[game.nintendoId] ?: game.platform)
                )
                duplicateSet.add(game.igdbId)
            }
        }
        return matchedGames
    }
}

/**
 * Title-cases a name, e.g. "SUPER MARIO ODYSSEY" to "Super Mario Odyssey".
 */
private fun titleCase(title: String): String {
    return title.lowercase()
        .split(" ")
        .joinToString(" ") { word -> if (word.isEmpty()) word else word.replaceFirstChar { it.uppercase() } }
}

/**
 * Sanitizes a title for the IGDB name matching, escaping quotes.
 * Trademark symbols and "full game" suffixes are already removed by
 * NintendoSource's cleanTitle when building idTitleMap.
 */
private fun sanitizeTitle(title: String): String {
    return title.replace("\"", "\\\"")
}
