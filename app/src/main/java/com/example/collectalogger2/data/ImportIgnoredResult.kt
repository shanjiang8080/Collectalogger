package com.example.collectalogger2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A library game that failed to import which the user chose to ignore.
 * Ignored results will not be reported (or re-imported) in future imports.
 */
@Entity(
    tableName = "import_ignored_results",
    // The same game from the same library should only ever be ignored once
    indices = [Index(value = ["name", "library"], unique = true)]
)
data class ImportIgnoredResult(
    // The name of the game as reported by the library
    @ColumnInfo(name = "name")
    val name: String = "",
    // The library it came from (e.g. "Steam")
    @ColumnInfo(name = "library")
    val library: String = "",
    @PrimaryKey(autoGenerate = true)
    // The internal id used for the database
    val id: Long = 0,
)
