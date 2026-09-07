package com.example.collectalogger2.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportIgnoredResultDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ignoredResult: ImportIgnoredResult)

    @Delete
    suspend fun delete(ignoredResult: ImportIgnoredResult)

    @Query("SELECT * FROM import_ignored_results ORDER BY library ASC, name ASC")
    suspend fun getAllIgnoredResults(): List<ImportIgnoredResult>

    @Query("SELECT * FROM import_ignored_results ORDER BY library ASC, name ASC")
    fun getAllIgnoredResultsFlow(): Flow<List<ImportIgnoredResult>>

    @Query(
        "SELECT * FROM import_ignored_results " +
        "WHERE LOWER(name) = LOWER(:name) AND library = :library LIMIT 1"
    )
    suspend fun getIgnoredResultByNameAndLibrary(name: String, library: String): ImportIgnoredResult?
}
