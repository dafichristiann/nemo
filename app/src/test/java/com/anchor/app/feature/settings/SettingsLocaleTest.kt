package com.anchor.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class SettingsLocaleTest {

    private fun isIndonesian(locale: String) =
        locale.startsWith("id", ignoreCase = true) || locale.startsWith("in", ignoreCase = true)

    private fun normalizeLocale(locale: String) =
        if (isIndonesian(locale)) "id" else "en"

    private fun resolveTargetLocale(locale: String) =
        if (isIndonesian(locale)) Locale("in", "ID") else Locale.ENGLISH

    @Test
    fun isIndonesian_matchesIdAndIn() {
        assertTrue(isIndonesian("id"))
        assertTrue(isIndonesian("ID"))
        assertTrue(isIndonesian("id-ID"))
        assertTrue(isIndonesian("in"))
        assertTrue(isIndonesian("IN"))
        assertTrue(isIndonesian("in_ID"))
    }

    @Test
    fun normalizeLocale_mapsIndonesianToId() {
        assertEquals("id", normalizeLocale("id"))
        assertEquals("id", normalizeLocale("in"))
        assertEquals("id", normalizeLocale("in_ID"))
        assertEquals("en", normalizeLocale("en"))
        assertEquals("en", normalizeLocale("fr"))
    }

    @Test
    fun resolveTargetLocale_mapsToAospIndonesianLocale() {
        val indo = resolveTargetLocale("id")
        println("Locale language: ${indo.language}, country: ${indo.country}")
        assertTrue(indo.language == "in" || indo.language == "id")
        assertEquals("ID", indo.country)

        val indoLegacy = resolveTargetLocale("in")
        assertTrue(indoLegacy.language == "in" || indoLegacy.language == "id")
        assertEquals("ID", indoLegacy.country)

        val english = resolveTargetLocale("en")
        assertEquals("en", english.language)
    }

    @Test
    fun valuesInDirectoryExistsAndHasStrings() {
        val valuesIn = File("src/main/res/values-in/strings.xml")
        val fallbackPath = File("app/src/main/res/values-in/strings.xml")
        val file = if (valuesIn.exists()) valuesIn else fallbackPath
        assertTrue("values-in/strings.xml must exist", file.exists())
        assertTrue("values-in/strings.xml must not be empty", file.length() > 0)
    }
}
