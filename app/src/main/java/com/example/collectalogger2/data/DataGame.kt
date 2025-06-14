package com.example.collectalogger2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.collectalogger2.util.LibraryEnum

/**
 * model class for a Game.
 * Currently, there are some missing fields, and the interface isn't quite right.
 * Still, it's good enough for now.
 */
@Entity(tableName = "games")
data class Game(
    var title: String = "",
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0, // this is the normal id
) {
    // Sourced from IGDB but cached
    var description: String = ""
    // Title, but excluding The and other articles
    var sortingName: String = ""
    // The platform the user has the game on (PC, PS5, Switch, etc).
    // Later on, I'll add non-PC sources that will affect this.
    val platform: Set<String> = setOf()
    // The genre, as provided by IGDB.
    val genre: Set<String> = setOf()
    // The game libraries (can be multiple) that it is part of.
    val source: Set<LibraryEnum> = setOf()
    // Play status (set manually)
    var status: String = ""
    // The ID that IGDB uses. Of course, set by IGDB.
    val igdbId: Long = -1
    // a URL for IGDB, i guess...
    var imageUrl: String = ""
    var playTime: Long = 0
}