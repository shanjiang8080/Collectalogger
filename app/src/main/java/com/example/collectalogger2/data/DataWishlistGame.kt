package com.example.collectalogger2.data
// wrapper for Game
data class WishlistGame(
    val game: Game,
    val price: Float,
    val lowestPrice: Float?
)