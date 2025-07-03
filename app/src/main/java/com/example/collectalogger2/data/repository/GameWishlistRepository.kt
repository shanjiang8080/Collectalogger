package com.example.collectalogger2.data.repository

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.WishlistGame

// TODO
class GameWishlistRepository {
    fun getFakeWishlistGames(): List<WishlistGame> {
        var g1 = Game("Cult of the Lamb", 3)
        g1 = g1.copy(imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co55md.webp")
        var g2 = Game("Have a Nice Death", 4)
        g2 = g2.copy(imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co4a9g.webp")

        val w1 = WishlistGame(g1, 27.99f, null)
        val w2 = WishlistGame(g2, 14.99f, null)

        return listOf(w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,w1, w2,)
    }
}