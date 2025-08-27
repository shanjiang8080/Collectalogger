package com.example.collectalogger2.util

import com.example.collectalogger2.data.Game

data class Filter(
    val library: List<String>? = listOf(),
    val platform: List<String>? = listOf(),
    val genre: List<Int>? = listOf(),
    val developer: List<String>? = listOf(),
    val publisher: List<String>? = listOf(),
    val isFavorite: Boolean? = null
) {
    fun getFilteredItems(games: List<Game>): List<Game> {
        return games.filter { game: Game ->
            if (isFavorite != null) {
                if (isFavorite != game.isFavorite) return@filter false
            }
            if (library != null) {
                if (library.intersect(game.source).isEmpty()) return@filter false
            }
            if (platform != null) {
                if (platform.intersect(game.platform).isEmpty()) return@filter false
            }
            if (genre != null) {
                if (genre.intersect(game.genre).isEmpty()) return@filter false
            }
            if (developer != null) {
                if (developer.intersect(game.developers).isEmpty()) return@filter false
            }
            if (publisher != null) {
                if (publisher.intersect(game.publishers).isEmpty()) return@filter false
            }
            return@filter true
        }
    }
}