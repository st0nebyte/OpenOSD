package dev.st0nebyte.openosd

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OSDView helper functions.
 * Tests speaker configuration formatting logic.
 */
class OSDViewHelperTest {

    // Helper function to test formatSpeakers logic
    // Since formatSpeakers is private in OSDView, we replicate the logic here for testing
    private fun formatSpeakers(speakers: List<String>): String {
        if (speakers.isEmpty()) return ""

        val count = speakers.size
        val hasSubwoofer = speakers.any { it == "SW" || it == "SW2" }
        val hasFrontHeight = speakers.any { it.startsWith("FH") }

        // Common configurations
        return when {
            // 5.1 configurations
            count == 6 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1"

            // 7.1 configurations
            count == 8 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "7.1"

            // 5.1.2 Atmos (5.1 + 2 height)
            count == 8 && hasSubwoofer && hasFrontHeight &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1.2"

            // Stereo
            count == 2 && speakers.containsAll(listOf("FL", "FR")) -> "2.0"

            // Stereo with sub
            count == 3 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "SW")) -> "2.1"

            // Custom: just list them
            count <= 4 -> speakers.joinToString(" ")

            // Many speakers: show count
            else -> "$count speakers"
        }
    }

    @Test
    fun formatSpeakers_stereo_returns20() {
        val speakers = listOf("FL", "FR")
        assertEquals("2.0", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_stereoWithSub_returns21() {
        val speakers = listOf("FL", "FR", "SW")
        assertEquals("2.1", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_51_returns51() {
        val speakers = listOf("FL", "FR", "C", "SW", "SL", "SR")
        assertEquals("5.1", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_51_orderDoesNotMatter() {
        val speakers = listOf("SR", "FL", "SW", "C", "FR", "SL")
        assertEquals("5.1", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_71_returns71() {
        val speakers = listOf("FL", "FR", "C", "SW", "SL", "SR", "SBL", "SBR")
        assertEquals("7.1", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_512Atmos_returns512() {
        val speakers = listOf("FL", "FR", "C", "SW", "SL", "SR", "FHL", "FHR")
        assertEquals("5.1.2", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_empty_returnsEmpty() {
        val speakers = emptyList<String>()
        assertEquals("", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_customSmall_returnsSpaceList() {
        val speakers = listOf("FL", "FR", "C")
        assertEquals("FL FR C", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_customManyNoMatch_returnsCount() {
        val speakers = listOf("FL", "FR", "C", "SL", "SR")  // 5.0 (no sub)
        assertEquals("5 speakers", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_dualSubwoofers_21() {
        val speakers = listOf("FL", "FR", "SW", "SW2")
        assertEquals("FL FR SW SW2", formatSpeakers(speakers))  // 4 speakers, listed
    }

    @Test
    fun formatSpeakers_atmosTopFront_notDetectedAs512() {
        // TFL/TFR (top front) vs FHL/FHR (front height) - only FH* triggers 5.1.2
        val speakers = listOf("FL", "FR", "C", "SW", "SL", "SR", "TFL", "TFR")
        assertEquals("8 speakers", formatSpeakers(speakers))  // Not 5.1.2 because TFL/TFR not FHL/FHR
    }

    @Test
    fun formatSpeakers_51WithExtraSpeaker_notDetectedAs51() {
        val speakers = listOf("FL", "FR", "C", "SW", "SL", "SR", "SB")  // 7 speakers
        assertEquals("7 speakers", formatSpeakers(speakers))
    }

    @Test
    fun formatSpeakers_singleSpeaker_listed() {
        val speakers = listOf("C")
        assertEquals("C", formatSpeakers(speakers))
    }
}
