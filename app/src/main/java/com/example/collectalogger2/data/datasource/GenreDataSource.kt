package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.util.IGDBSource
import org.json.JSONObject

/**
 * This class gets its genres from IGDB.
 */
class GenreDataSource {
    suspend fun getGenres(): List<Genre> {
        var request =
            """
                    fields id, name;
                    limit 500;
                """.trimIndent()
        Log.d("IGDBParser", request)
        var igdbResponse = IGDBSource.makeAPICall(
            endpoint = "genres",
            request
        )
        Log.d("GenreDataSource", "Response: ${igdbResponse}")
        var newGenres: MutableList<Genre> = mutableListOf()
        for (i in 0 until igdbResponse.length()) {
            var genre: JSONObject = igdbResponse.getJSONObject(i)
            newGenres.add(Genre(name = genre.getString("name"), igdbId = genre.getInt("id")))
        }
        return newGenres.toList()
    }
}