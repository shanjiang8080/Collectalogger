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
    val title: String = "",
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // this is the normal id
    // Sourced from IGDB but cached
    val description: String = "",
    // Title, but excluding The and other articles
    val sortingName: String = "",
    // The platform the user has the game on (PC, PS5, Switch, etc).
    // Later on, I'll add non-PC sources that will affect this.
    val platform: Set<String> = setOf(),
    // The genre, as provided by IGDB.
    val genre: Set<String> = setOf(),
    // The game libraries (can be multiple) that it is part of.
    val source: Set<String> = setOf(),
    // Play status (set manually)
    val status: String = "",
    // The ID that IGDB uses. Of course, set by IGDB.
    val igdbId: Long = -1,
    // The URL for IGDB cover.
    val imageUrl: String = "",
    // The URL for IGDB image background.
    val backgroundUrl: String = "",
    val playTime: Long = 0,
    // adding a steamId for this
    val steamId: Long = -1,
) {
}