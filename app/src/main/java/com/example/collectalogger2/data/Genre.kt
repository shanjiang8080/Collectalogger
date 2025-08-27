package com.example.collectalogger2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class Genre(
    val name: String = "",
    @PrimaryKey(autoGenerate = true)
    // The internal Id used for the database
    val id: Int = 0,
    // the IGDB id. If a custom genre, it is a negative number (not just -1)
    val igdbId: Int = -1
)
