package com.example.collectalogger2.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.AutoMigration
import androidx.room.TypeConverters
import com.example.collectalogger2.util.Converters

// increase the version whenever you change the schema of the database table.
@Database(
    entities = [Game::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
    ])
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var Instance: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                Instance = instance
                instance
            }
        }
    }
}