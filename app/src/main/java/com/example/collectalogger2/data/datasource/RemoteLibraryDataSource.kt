package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import kotlinx.coroutines.flow.Flow

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
    abstract suspend fun getGames(forceUpdate: Boolean = false): Flow<Game>

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
}
