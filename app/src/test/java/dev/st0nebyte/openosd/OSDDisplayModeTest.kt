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
        assertEquals(5, triggers.size)
        assertTrue(triggers.contains(OSDTrigger.VOLUME))
        assertTrue(triggers.contains(OSDTrigger.MUTE))
        assertTrue(triggers.contains(OSDTrigger.UNMUTE))
        assertTrue(triggers.contains(OSDTrigger.SOURCE))
        assertTrue(triggers.contains(OSDTrigger.SOUND_MODE))
    }

    @Test
    fun trigger_valueOf_worksCorrectly() {
        assertEquals(OSDTrigger.VOLUME, OSDTrigger.valueOf("VOLUME"))
        assertEquals(OSDTrigger.MUTE, OSDTrigger.valueOf("MUTE"))
        assertEquals(OSDTrigger.UNMUTE, OSDTrigger.valueOf("UNMUTE"))
        assertEquals(OSDTrigger.SOURCE, OSDTrigger.valueOf("SOURCE"))
        assertEquals(OSDTrigger.SOUND_MODE, OSDTrigger.valueOf("SOUND_MODE"))
    }

    @Test
    fun trigger_hasTimeout() {
        // Volume trigger has 3 second timeout
        assertEquals(3000L, OSDTrigger.VOLUME.timeoutMs)
        // MUTE has no timeout (0ms, persists while muted)
        assertEquals(0L, OSDTrigger.MUTE.timeoutMs)
        // UNMUTE triggers fade-out
        assertEquals(350L, OSDTrigger.UNMUTE.timeoutMs)
        // Info triggers (source, sound_mode) have 5 second timeout
        assertEquals(5000L, OSDTrigger.SOURCE.timeoutMs)
        assertEquals(5000L, OSDTrigger.SOUND_MODE.timeoutMs)
    }

    @Test
    fun trigger_hasCorrectFlags() {
        // VOLUME shows only volume bar
        assertTrue(OSDTrigger.VOLUME.showVolume)
        assertFalse(OSDTrigger.VOLUME.showInfo)

        // MUTE shows both elements, persists while muted
        assertTrue(OSDTrigger.MUTE.showVolume)
        assertTrue(OSDTrigger.MUTE.showInfo)
        assertTrue(OSDTrigger.MUTE.persistWhileMuted)

        // UNMUTE hides both
        assertFalse(OSDTrigger.UNMUTE.showVolume)
        assertFalse(OSDTrigger.UNMUTE.showInfo)

        // SOURCE and SOUND_MODE show only info box
        assertFalse(OSDTrigger.SOURCE.showVolume)
        assertTrue(OSDTrigger.SOURCE.showInfo)

        assertFalse(OSDTrigger.SOUND_MODE.showVolume)
        assertTrue(OSDTrigger.SOUND_MODE.showInfo)
    }
}
