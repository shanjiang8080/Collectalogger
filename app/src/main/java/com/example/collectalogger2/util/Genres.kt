package com.example.collectalogger2.util

import androidx.annotation.StringRes
import com.example.collectalogger2.R

/**
 * A genre known to the app. [igdbId] is the stable IGDB genre id,
 * [nameRes] is its localized display name.
 */
data class Genre(
    val igdbId: Int,
    @StringRes val nameRes: Int
)

/**
 * The full, static list of IGDB genres (see app/genres.txt).
 * IGDB genre ids are stable; gaps in the id range are genres IGDB deleted.
 * Genres are display-only — games store raw IGDB genre ids, and filtering
 * happens on those ids.
 */
object Genres {
    val all: List<Genre> = listOf(
        Genre(2, R.string.genre_point_and_click),
        Genre(4, R.string.genre_fighting),
        Genre(5, R.string.genre_shooter),
        Genre(7, R.string.genre_music),
        Genre(8, R.string.genre_platform),
        Genre(9, R.string.genre_puzzle),
        Genre(10, R.string.genre_racing),
        Genre(11, R.string.genre_rts),
        Genre(12, R.string.genre_rpg),
        Genre(13, R.string.genre_simulator),
        Genre(14, R.string.genre_sport),
        Genre(15, R.string.genre_strategy),
        Genre(16, R.string.genre_tbs),
        Genre(24, R.string.genre_tactical),
        Genre(25, R.string.genre_hack_and_slash),
        Genre(26, R.string.genre_quiz_trivia),
        Genre(30, R.string.genre_pinball),
        Genre(31, R.string.genre_adventure),
        Genre(32, R.string.genre_indie),
        Genre(33, R.string.genre_arcade),
        Genre(34, R.string.genre_visual_novel),
        Genre(35, R.string.genre_card_and_board_game),
        Genre(36, R.string.genre_moba),
    )

    private val byId: Map<Int, Genre> = all.associateBy { it.igdbId }

    /**
     * Looks up a genre by its IGDB id, or null if the id is unknown.
     */
    fun byId(id: Int): Genre? = byId[id]
}
