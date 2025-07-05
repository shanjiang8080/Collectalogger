package com.example.collectalogger2.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(game: Game)
    @Update
    suspend fun update(game: Game)
    @Delete
    suspend fun delete(game: Game)

    @Query("SELECT * from games WHERE id = :id")
    fun getGame(id: Long): Game?

    @Query("SELECT * from games WHERE id = :id")
    fun getGameStream(id: Long): Flow<Game?>


    @Query("SELECT * from games WHERE igdbId = :igdbId")
    fun getGameByIGDBId(igdbId: Long): Game?

    @Query("SELECT * from games WHERE steamId = :steamId")
    fun getGameBySteamId(steamId: Long): Game?

    @Query("SELECT * from games WHERE epicId = :epicId")
    fun getGameByEpicId(epicId: String): Game?

    @Query("SELECT * from games WHERE igdbId = :igdbId")
    fun getGameStreamByIGDBId(igdbId: Long): Flow<Game?>

    @Query("SELECT * from games ORDER BY sortingName ASC")
    fun getAllGames(): List<Game>

    @Query("SELECT * from games ORDER BY sortingName ASC")
    fun getAllGamesStream(): Flow<List<Game>>
}