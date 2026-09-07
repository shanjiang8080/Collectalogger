package com.example.collectalogger2.data.datasource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IGDBSearchTest {

    @Test
    fun `fallback search removes punctuation, editions, episodes and seasons`() {
        assertEquals(
            "Warhammer 40,000 Mechanicus",
            buildFallbackSearch("Warhammer 40,000: Mechanicus - Standard Edition")
        )
        assertEquals(
            "The Walking Dead",
            buildFallbackSearch("The Walking Dead: Season One")
        )
        assertEquals(
            "Life is Strange",
            buildFallbackSearch("Life is Strange - Episode 1")
        )
        assertEquals(
            "Game",
            buildFallbackSearch("Game: Deluxe Edition")
        )
    }

    @Test
    fun `fallback search keeps unchanged queries as-is`() {
        assertEquals("Hades", buildFallbackSearch("Hades"))
    }

    @Test
    fun `fallback search returns null when nothing is left`() {
        assertNull(buildFallbackSearch("Standard Edition"))
    }
}
