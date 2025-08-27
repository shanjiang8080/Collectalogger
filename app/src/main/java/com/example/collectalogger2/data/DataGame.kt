package com.example.collectalogger2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    // The IGDB genre id.
    val genre: Set<Int> = setOf(),
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
    // the playTime, in minutes. (note that it's the playtime of the version with highest playtime...)
    val playTime: Long = 0,
    // adding a steamId for this
    val steamId: Long = -1,
    // and an epicId as well, though it's a string.
    @ColumnInfo(name = "epicId", defaultValue = "")
    val epicId: String = "",
    // This is for IGDB screenshots
    @ColumnInfo(name = "screenshots", defaultValue = "")
    val screenshots: List<String> = listOf(),
    @ColumnInfo(name = "developers", defaultValue = "")
    val developers: Set<String> = setOf(),
    @ColumnInfo(name = "publishers", defaultValue = "")
    val publishers: Set<String> = setOf(),
    // denotes if the game is a favorite
    @ColumnInfo(name = "isFavorite", defaultValue = "0")
    val isFavorite: Boolean = false,
) {
}