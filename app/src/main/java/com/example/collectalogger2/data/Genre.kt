package com.example.collectalogger2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class Genre(
    val name: String = "",
    @PrimaryKey(autoGenerate = true)
    // The internal id used for the database
    val id: Int = 0,
    // The IGDB id, with custom genres being negative numbers (not just -1)
    val igdbId: Int = -1
)
