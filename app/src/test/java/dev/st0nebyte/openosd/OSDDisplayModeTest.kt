package dev.st0nebyte.openosd

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OSDDisplayMode and OSDScale enums.
 * Tests proper enum behavior and defaults.
 */
class OSDDisplayModeTest {

    @Test
    fun displayMode_allValuesExist() {
        val modes = OSDDisplayMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(OSDDisplayMode.STANDARD))
        assertTrue(modes.contains(OSDDisplayMode.INFO))
        assertTrue(modes.contains(OSDDisplayMode.EXTENDED))
    }

    @Test
    fun displayMode_valueOf_worksCorrectly() {
        assertEquals(OSDDisplayMode.STANDARD, OSDDisplayMode.valueOf("STANDARD"))
        assertEquals(OSDDisplayMode.INFO, OSDDisplayMode.valueOf("INFO"))
        assertEquals(OSDDisplayMode.EXTENDED, OSDDisplayMode.valueOf("EXTENDED"))
    }

    @Test
    fun scale_allValuesExist() {
        val scales = OSDScale.values()
        assertEquals(3, scales.size)
        assertTrue(scales.contains(OSDScale.SMALL))
        assertTrue(scales.contains(OSDScale.MEDIUM))
        assertTrue(scales.contains(OSDScale.LARGE))
    }

    @Test
    fun scale_valueOf_worksCorrectly() {
        assertEquals(OSDScale.SMALL, OSDScale.valueOf("SMALL"))
        assertEquals(OSDScale.MEDIUM, OSDScale.valueOf("MEDIUM"))
        assertEquals(OSDScale.LARGE, OSDScale.valueOf("LARGE"))
    }

    @Test
    fun trigger_allValuesExist() {
        val triggers = OSDTrigger.values()
        assertEquals(2, triggers.size)
        assertTrue(triggers.contains(OSDTrigger.VOLUME))
        assertTrue(triggers.contains(OSDTrigger.MUTE))
    }

    @Test
    fun trigger_valueOf_worksCorrectly() {
        assertEquals(OSDTrigger.VOLUME, OSDTrigger.valueOf("VOLUME"))
        assertEquals(OSDTrigger.MUTE, OSDTrigger.valueOf("MUTE"))
    }

    @Test
    fun trigger_hasTimeout() {
        // Both triggers should have 3 second timeout
        assertEquals(3000L, OSDTrigger.VOLUME.timeoutMs)
        assertEquals(3000L, OSDTrigger.MUTE.timeoutMs)
    }
}
