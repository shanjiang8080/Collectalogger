package com.example.collectalogger2.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(genre: Genre)

    @Update
    suspend fun update(genre: Genre)

    @Delete
    suspend fun delete(genre: Genre)

    @Query("SELECT * FROM genres WHERE id = :id")
    suspend fun getGenre(id: Int): Genre?

    @Query("SELECT * FROM genres WHERE igdbId = :igdbId")
    suspend fun getGenreByIGDBId(igdbId: Int): Genre?

    @Query("SELECT * FROM genres ORDER BY id ASC")
    suspend fun getAllGenres(): List<Genre>

    @Query("SELECT * FROM genres ORDER BY id ASC")
    fun getAllGenresFlow(): Flow<List<Genre>>

    @Query("SELECT * FROM genres WHERE LOWER(name) LIKE '%' || LOWER(:search) || '%' LIMIT :maxResults")
    fun getGenresSearchLimited(search: String, maxResults: Int): List<Genre>

    @Query("SELECT * FROM genres WHERE LOWER(name) LIKE '%' || LOWER(:search) || '%'")
    fun getGenresSearch(search: String): List<Genre>

}