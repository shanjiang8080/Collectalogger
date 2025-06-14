package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import kotlinx.coroutines.flow.Flow

/**
 * The base class that all libraries (Steam, Epic, etc) derive from.
 * Other methods may be added later, like achievements. Or not! I dunno about architecture.
 */

abstract class RemoteLibraryDataSource() {
    // It has a userID for like, SteamID, or Epic ID, etc.
    abstract var userId: String
    abstract suspend fun getGames(): Flow<Game>
}
