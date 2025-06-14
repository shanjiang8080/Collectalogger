package com.example.collectalogger2.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

// increase the version whenever you change the schema of the database table.
@Database(entities = [Game::class], version = 1, exportSchema = true)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var Instance: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            // if Instance isn't null, return it, otherwise create a new database instance.
            return (Instance ?: synchronized(this) {
                Room.databaseBuilder(context, GameDatabase::class.java, "game_database")
                    .fallbackToDestructiveMigration(false)
                    .also { Instance = it.build() } // different from template but idk??
            }) as GameDatabase
        }
    }
}