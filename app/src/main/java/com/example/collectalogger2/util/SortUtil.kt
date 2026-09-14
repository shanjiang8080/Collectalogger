package com.example.collectalogger2.util

import com.example.collectalogger2.data.Game
import java.util.Calendar

/**
 * A class for sorting.
 */
data class Sort(
    val criteria: SortBy,
    val ascending: Boolean,
    val grouped: Boolean = false
) {
    /**
     * Returns the games sorted by this Sort's criteria and direction.
     */
    fun getSortedItems(games: List<Game>): List<Game> {
        val sorted = when (criteria) {
            SortBy.NAME -> games.sortedBy { nameSortKey(it) }
            SortBy.PLATFORM -> games.sortedBy {
                it.platform.minOrNull()?.lowercase() ?: ""
            }
            SortBy.RELEASED -> games.sortedBy { it.releaseDate }
        }
        return if (ascending) sorted else sorted.asReversed()
    }

    /**
     * Returns the games sorted and grouped into sections by this Sort's
     * criteria and direction.
     * - NAME: one section per starting letter
     * - PLATFORM: one section per platform; a game appears once per platform it is on
     * - RELEASED: one section per release year
     */
    fun getSortedSections(games: List<Game>): List<SortedSection> {
        val sections = when (criteria) {
            SortBy.NAME -> games
                .groupBy { nameSectionHeader(it) }
                .map { (header, gamesInSection) ->
                    SortedSection(header, gamesInSection.sortedBy { nameSortKey(it) })
                }
                .sortedBy { it.header }
            SortBy.PLATFORM -> games
                .flatMap { game ->
                    game.platform.ifEmpty { setOf("Unknown") }.map { platform ->
                        platform to game
                    }
                }
                .groupBy({ it.first }, { it.second })
                .map { (header, gamesInSection) ->
                    SortedSection(header, gamesInSection.sortedBy { nameSortKey(it) })
                }
                .sortedBy { it.header.lowercase() }
            SortBy.RELEASED -> games
                .groupBy { releaseYear(it) }
                .map { (year, gamesInSection) ->
                    Pair(
                        year,
                        SortedSection(
                            year?.toString() ?: "Unknown",
                            gamesInSection.sortedBy { it.releaseDate }
                        )
                    )
                }
                .sortedBy { it.first ?: Int.MAX_VALUE }
                .map { it.second }
        }
        return if (ascending) sections
        else sections.reversed().map { it.copy(games = it.games.reversed()) }
    }

    private fun nameSortKey(game: Game): String =
        game.sortingName.ifEmpty { game.title }.lowercase()

    private fun nameSectionHeader(game: Game): String {
        val first = nameSortKey(game).firstOrNull()?.uppercaseChar()
        return if (first != null && first.isLetter()) first.toString() else "#"
    }

    private fun releaseYear(game: Game): Int? =
        if (game.releaseDate == 0L) null
        else Calendar.getInstance().apply { timeInMillis = game.releaseDate * 1000 }
            .get(Calendar.YEAR)
}

/**
 * A section of games sharing a sort header (a letter, a year, a platform, etc).
 */
data class SortedSection(
    val header: String,
    val games: List<Game>
)

enum class SortBy {
    NAME,
    PLATFORM,
    RELEASED
}
