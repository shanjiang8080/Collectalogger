package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.util.libraryObjects.IGDBSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Searches IGDB for games by name.
 * Returns partial games, holding only the IGDB metadata
 * (id, name, summary, cover art, genres, developers/publishers)
 * without any store-specific IDs or playtime.
 * If there are no results, a fallback search is made with a cleaned up,
 * more lenient version of the search string (see buildFallbackSearch).
 */
suspend fun searchIGDB(search: String, limit: Int = 5): List<Game> {
    if (search.isBlank()) return emptyList()
    val games = searchIGDBOnce(search, limit)
    if (games.isNotEmpty()) return games

    val fallbackSearch = buildFallbackSearch(search) ?: return emptyList()
    Log.d("IGDBSearch", "fallbackSearch: $fallbackSearch")
    return searchIGDBOnce(fallbackSearch, limit)
}

/**
 * Builds a fallback search string from a game name that got no IGDB results,
 * by removing punctuation like dashes and colons and stripping parts that
 * identify versions rather than the game itself.
 * Returns null when there is nothing left to search for.
 */
internal fun buildFallbackSearch(search: String): String? {
    val cleaned = search
        .replace(Regex("[-:]"), " ")
        .replace(Regex("[©®™]"), "")
        .replace(editionRegex, "")
        .replace(episodeRegex, "")
        .replace(seasonRegex, "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isBlank()) return null
    return cleaned
}

// Matches e.g. "Standard Edition" or "Deluxe Edition"
private val editionRegex = Regex("""\b\S+\s+Edition\b""", RegexOption.IGNORE_CASE)
// Matches e.g. "Episode 1" or "Episode One"
private val episodeRegex = Regex("""\bEpisode\s+\S+""", RegexOption.IGNORE_CASE)
// Matches e.g. "Season 1" or "Season One"
private val seasonRegex = Regex("""\bSeason\s+\S+""", RegexOption.IGNORE_CASE)

private suspend fun searchIGDBOnce(search: String, limit: Int): List<Game> {
    // escape double quotes, since the search phrase is enclosed in quotes in the request
    val searchPhrase = search.replace("\"", "\\\"")
    val request = """
        search "$searchPhrase";
        fields
        id,
        summary,
        name,
        cover.image_id,
        genres.name,
        involved_companies.company.name,
        involved_companies.developer,
        involved_companies.publisher;
        limit $limit;
    """.trimIndent()
    val response = IGDBSource.makeAPICall(
        endpoint = "games",
        requestBody = request
    )
    val games = mutableListOf<Game>()
    for (i in 0 until response.length()) {
        val responseObj = response.get(i) as JSONObject
        if (!responseObj.has("name")) continue
        val title = responseObj.getString("name")
        val cover = if (responseObj.has("cover")) responseObj.get("cover") as JSONObject else null
        val genreList = if (responseObj.has("genres")) responseObj.get("genres") as JSONArray else null
        val genres = mutableSetOf<Int>()
        if (genreList != null) {
            for (j in 0 until genreList.length()) {
                val genre = genreList[j] as JSONObject
                if (genre.has("name")) genres.add(genre.getInt("id"))
            }
        }
        val involvedCompanies =
            if (responseObj.has("involved_companies")) responseObj.get("involved_companies") as JSONArray else null
        val developers = mutableListOf<String>()
        val publishers = mutableListOf<String>()
        for (j in 0 until (involvedCompanies?.length() ?: 0)) {
            val company = involvedCompanies!![j] as JSONObject
            val companyName = (company.get("company") as JSONObject).getString("name")
            if (company.getBoolean("developer")) developers.add(companyName)
            if (company.getBoolean("publisher")) publishers.add(companyName)
        }
        games.add(
            Game(
                title = title,
                sortingName = getSortingName(title),
                description = if (responseObj.has("summary")) responseObj.getString("summary") else "",
                igdbId = responseObj.getLong("id"),
                imageUrl = if (cover != null) "https://images.igdb.com/igdb/image/upload/t_cover_big/${cover.get("image_id")}.jpg" else "",
                genre = genres,
                developers = developers.toSet(),
                publishers = publishers.toSet()
            )
        )
    }
    return games
}
