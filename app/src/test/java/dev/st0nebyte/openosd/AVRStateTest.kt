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

    @Test
    fun volumeString_negativeVolume_formatsCorrectly() {
        // Negative relative volume (very low dB)
        val state = AVRState(volumeDb = -75.0)
        assertEquals("5", state.volumeString)
    }

    @Test
    fun volumeNorm_negativeDb_returnsCorrectRatio() {
        // -60 dB = 20/98 = ~0.204
        val state = AVRState(volumeDb = -60.0)
        assertEquals(0.204f, state.volumeNorm, 0.01f)
    }

    @Test
    fun state_withAllFields_toStringWorks() {
        // Ensure data class toString works with all fields populated
        val state = AVRState(
            power = true,
            volumeDb = -45.0,
            muted = true,
            soundMode = "DOLBY ATMOS",
            inputSource = "BLU-RAY",
            signalDetect = "HDMI",
            digitalMode = "AUTO",
            speakers = listOf("FL", "FR", "C", "SW", "SL", "SR"),
            drc = "AUTO",
            audioRestorer = "MED",
            hdmiAudioOut = "AMP",
            ecoMode = "OFF"
        )

        val str = state.toString()
        assertTrue(str.contains("DOLBY ATMOS"))
        assertTrue(str.contains("BLU-RAY"))
    }

    @Test
    fun state_equality_worksCorrectly() {
        val state1 = AVRState(volumeDb = -50.0, soundMode = "STEREO")
        val state2 = AVRState(volumeDb = -50.0, soundMode = "STEREO")
        val state3 = AVRState(volumeDb = -50.0, soundMode = "DIRECT")

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    @Test
    fun state_hashCode_consistentWithEquals() {
        val state1 = AVRState(volumeDb = -50.0, soundMode = "STEREO")
        val state2 = AVRState(volumeDb = -50.0, soundMode = "STEREO")

        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun state_copy_onlyChangesSpecifiedFields() {
        val original = AVRState(
            volumeDb = -50.0,
            soundMode = "STEREO",
            inputSource = "GAME"
        )

        val modified = original.copy(soundMode = "DIRECT")

        assertEquals(-50.0, modified.volumeDb, 0.001)
        assertEquals("DIRECT", modified.soundMode)
        assertEquals("GAME", modified.inputSource)
    }

    @Test
    fun volumeString_extremelyLowVolume_clampsAndFormats() {
        val state = AVRState(volumeDb = -200.0)  // Way below minimum
        assertEquals("0", state.volumeString)
    }

    @Test
    fun volumeString_extremelyHighVolume_clampsAndFormats() {
        val state = AVRState(volumeDb = 200.0)  // Way above maximum
        assertEquals("98", state.volumeString)
    }

    @Test
    fun volumeNorm_extremeValues_clampsTo0and1() {
        val stateLow = AVRState(volumeDb = -500.0)
        val stateHigh = AVRState(volumeDb = 500.0)

        assertEquals(0f, stateLow.volumeNorm, 0.001f)
        assertEquals(1f, stateHigh.volumeNorm, 0.001f)
    }

    @Test
    fun state_withComplexSpeakerSetup_storesCorrectly() {
        val speakers = listOf("FL", "FR", "C", "SW", "SW2", "SL", "SR", "SBL", "SBR", "TFL", "TFR")
        val state = AVRState(speakers = speakers)

        assertEquals(11, state.speakers.size)
        assertTrue(state.speakers.containsAll(listOf("TFL", "TFR")))
    }
}

