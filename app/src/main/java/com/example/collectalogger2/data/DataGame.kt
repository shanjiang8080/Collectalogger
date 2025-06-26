package com.example.collectalogger2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.collectalogger2.util.LibraryEnum
import java.util.Dictionary

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
    // Sourced from IGDB but cached
    var description: String = "",
    // Title, but excluding The and other articles
    var sortingName: String = "",
    // The platform the user has the game on (PC, PS5, Switch, etc).
    // Later on, I'll add non-PC sources that will affect this.
    var platform: Set<String> = setOf(),
    // The genre, as provided by IGDB.
    var genre: Set<String> = setOf(),
    // The game libraries (can be multiple) that it is part of.
    var source: Set<String> = setOf(),
    // Play status (set manually)
    var status: String = "",
    // The ID that IGDB uses. Of course, set by IGDB.
    var igdbId: Long = -1,
    // The URL for IGDB cover.
    var imageUrl: String = "",
    // The URL for IGDB image background.
    var backgroundUrl: String = "",
    var playTime: Long = 0,
    // adding a steamId for this
    var steamId: Long = -1,
) {
}