package dev.st0nebyte.openosd

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AVRState data class.
 * Tests volume formatting and normalization logic.
 */
class AVRStateTest {

    @Test
    fun volumeString_wholeStep_formatsCorrectly() {
        // 27.0 relative = -53.0 dB
        val state = AVRState(volumeDb = -53.0)
        assertEquals("27", state.volumeString)
    }

    @Test
    fun volumeString_halfStep_formatsCorrectly() {
        // 27.5 relative = -52.5 dB
        val state = AVRState(volumeDb = -52.5)
        assertEquals("27.5", state.volumeString)
    }

    @Test
    fun volumeString_minimum_formatsCorrectly() {
        // 0 relative = -80.0 dB (minimum)
        val state = AVRState(volumeDb = -80.0)
        assertEquals("0", state.volumeString)
    }

    @Test
    fun volumeString_maximum_formatsCorrectly() {
        // 98 relative = +18.0 dB (maximum)
        val state = AVRState(volumeDb = 18.0)
        assertEquals("98", state.volumeString)
    }

    @Test
    fun volumeString_belowMinimum_clampsToZero() {
        // Below minimum should clamp to 0
        val state = AVRState(volumeDb = -100.0)
        assertEquals("0", state.volumeString)
    }

    @Test
    fun volumeString_aboveMaximum_clampsTo98() {
        // Above maximum should clamp to 98
        val state = AVRState(volumeDb = 50.0)
        assertEquals("98", state.volumeString)
    }

    @Test
    fun volumeNorm_minimum_returnsZero() {
        val state = AVRState(volumeDb = -80.0)
        assertEquals(0f, state.volumeNorm, 0.001f)
    }

    @Test
    fun volumeNorm_maximum_returnsOne() {
        val state = AVRState(volumeDb = 18.0)
        assertEquals(1f, state.volumeNorm, 0.001f)
    }

    @Test
    fun volumeNorm_midpoint_returnsHalf() {
        // Midpoint: -80 + (98/2) = -31 dB
        val state = AVRState(volumeDb = -31.0)
        assertEquals(0.5f, state.volumeNorm, 0.001f)
    }

    @Test
    fun volumeNorm_belowMinimum_clampsToZero() {
        val state = AVRState(volumeDb = -100.0)
        assertEquals(0f, state.volumeNorm, 0.001f)
    }

    @Test
    fun volumeNorm_aboveMaximum_clampsToOne() {
        val state = AVRState(volumeDb = 50.0)
        assertEquals(1f, state.volumeNorm, 0.001f)
    }

    @Test
    fun dataClass_copy_worksCorrectly() {
        val original = AVRState(
            power = true,
            volumeDb = -50.0,
            muted = false,
            soundMode = "DOLBY ATMOS"
        )

        val modified = original.copy(muted = true)

        assertTrue(modified.muted)
        assertEquals(true, modified.power)
        assertEquals(-50.0, modified.volumeDb, 0.001)
        assertEquals("DOLBY ATMOS", modified.soundMode)
    }

    @Test
    fun dataClass_defaultValues_areCorrect() {
        val state = AVRState()

        assertFalse(state.power)
        assertEquals(-80.0, state.volumeDb, 0.001)
        assertFalse(state.muted)
        assertNull(state.soundMode)
        assertNull(state.inputSource)
        assertNull(state.signalDetect)
        assertNull(state.digitalMode)
        assertTrue(state.speakers.isEmpty())
        assertNull(state.drc)
        assertNull(state.audioRestorer)
        assertNull(state.hdmiAudioOut)
        assertNull(state.ecoMode)
    }

    @Test
    fun volumeString_edgeCase_0point4_roundsToHalf() {
        // 27.4 should round to 27.5 (Denon only uses .5 steps)
        val state = AVRState(volumeDb = -52.6)
        assertEquals("27.5", state.volumeString)
    }

    @Test
    fun volumeString_edgeCase_0point6_roundsToHalf() {
        // 27.6 should show as 27.5 (Denon only uses .5 steps)
        val state = AVRState(volumeDb = -52.4)
        assertEquals("27.5", state.volumeString)
    }
}
