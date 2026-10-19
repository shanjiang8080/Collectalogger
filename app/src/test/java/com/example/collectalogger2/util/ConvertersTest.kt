package com.example.collectalogger2.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `set with comma-containing name round trips`() {
        val publishers = setOf("Nicalis, Inc.")
        assertEquals(publishers, converters.toSet(converters.fromSet(publishers)))
    }

    @Test
    fun `multiple values including comma-containing names round trip`() {
        val values = setOf("Nicalis, Inc.", "Chucklefish", "PC (Microsoft Windows)")
        assertEquals(values, converters.toSet(converters.fromSet(values)))
    }

    @Test
    fun `legacy comma separated values still parse`() {
        assertEquals(setOf("Chucklefish", "ConcernedApe"), converters.toSet("Chucklefish,ConcernedApe"))
        assertEquals(setOf("PC", "Switch"), converters.toSet("PC,Switch"))
    }

    @Test
    fun `legacy stored name with corporate suffix is not split`() {
        assertEquals(setOf("Nicalis, Inc."), converters.toSet("Nicalis, Inc."))
        assertEquals(setOf("Nicalis, Inc.", "Square Enix"), converters.toSet("Nicalis, Inc.,Square Enix"))
    }

    @Test
    fun `empty values round trip`() {
        assertEquals(setOf<String>(), converters.toSet(""))
        assertEquals("", converters.fromSet(setOf()))
        assertEquals(listOf<String>(), converters.toList(""))
    }

    @Test
    fun `list round trips with comma-containing url`() {
        val screenshots = listOf("https://images.igdb.com/1.jpg", "https://images.igdb.com/2,3.jpg")
        assertEquals(screenshots, converters.toList(converters.fromList(screenshots)))
    }
}
