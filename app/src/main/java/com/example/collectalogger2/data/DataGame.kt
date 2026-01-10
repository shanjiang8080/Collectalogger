package com.example.collectalogger2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model class for a Game.
 * Currently, there are some missing fields, and the interface isn't quite right.
 * Still, it's good enough for now.
 */
@Entity(tableName = "games")
data class Game(
    val title: String = "",
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // The internal id
    // Sourced from IGDB but cached
    val description: String = "",
    // Title excluding 'The' and other articles
    val sortingName: String = "",
    // The platform the user has the game on (PC, PS5, Switch, etc)
    val platform: Set<String> = setOf(),
    // The IGDB genre id
    val genre: Set<Int> = setOf(),
    // The game libraries (can be multiple) that it is part of.
    val source: Set<String> = setOf(),
    // Play status (set manually)
    val status: String = "",
    // The id used by IGDB
    val igdbId: Long = -1,
    // The URL for the IGDB cover
    val imageUrl: String = "",
    // The URL for the IGDB image background
    val backgroundUrl: String = "",
    // The playtime of the version with highest number of minutes
    val playTime: Long = 0,
    // Steam-specific id
    val steamId: Long = -1,
    // Epic-specific id which is a string combining namespace and id
    @ColumnInfo(name = "epicId", defaultValue = "")
    val epicId: String = "",
    // GOG-specific id
    @ColumnInfo(name = "gogId", defaultValue = "")
    val gogId: String = "",
    @ColumnInfo(name = "itchId", defaultValue = "")
    val itchId: String = "",
    // List of IGDB screenshot URLs
    @ColumnInfo(name = "screenshots", defaultValue = "")
    val screenshots: List<String> = listOf(),
    // List of developers according to IGDB
    @ColumnInfo(name = "developers", defaultValue = "")
    val developers: Set<String> = setOf(),
    // List of publishers according to IGDB
    @ColumnInfo(name = "publishers", defaultValue = "")
    val publishers: Set<String> = setOf(),
    // Denotes if the game is a favorite
    @ColumnInfo(name = "isFavorite", defaultValue = "0")
    val isFavorite: Boolean = false,
)