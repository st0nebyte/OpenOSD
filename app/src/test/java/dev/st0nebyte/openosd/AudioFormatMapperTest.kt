package dev.st0nebyte.openosd

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests for AudioFormatMapper - ensures audio format codes are properly expanded.
 */
class AudioFormatMapperTest {

    @Test
    fun `test Dolby formats`() {
        assertEquals("Dolby Atmos", AudioFormatMapper.formatSoundMode("DOLBY ATMOS"))
        assertEquals("Dolby Digital Plus + Dolby Surround",
            AudioFormatMapper.formatSoundMode("DOLBY D+ +DS"))
        assertEquals("Dolby Digital Plus + Neural:X",
            AudioFormatMapper.formatSoundMode("DOLBY D+ +NEURAL:X"))
        assertEquals("Dolby Digital Plus", AudioFormatMapper.formatSoundMode("DOLBY D+"))
        assertEquals("Dolby TrueHD + Dolby Surround",
            AudioFormatMapper.formatSoundMode("DOLBY HD+DS"))
        assertEquals("Dolby TrueHD", AudioFormatMapper.formatSoundMode("DOLBY HD"))
        assertEquals("Dolby Digital", AudioFormatMapper.formatSoundMode("DOLBY DIGITAL"))
        assertEquals("Dolby Surround", AudioFormatMapper.formatSoundMode("DOLBY SURROUND"))
    }

    @Test
    fun `test DTS formats`() {
        assertEquals("DTS:X Master", AudioFormatMapper.formatSoundMode("DTS:X MSTR"))
        assertEquals("DTS:X", AudioFormatMapper.formatSoundMode("DTS:X"))
        assertEquals("DTS-HD Master Audio", AudioFormatMapper.formatSoundMode("DTS HD MSTR"))
        assertEquals("DTS-HD + Neural:X", AudioFormatMapper.formatSoundMode("DTS HD+NEURAL:X"))
        assertEquals("DTS-HD High Resolution", AudioFormatMapper.formatSoundMode("DTS HD"))
        assertEquals("DTS + Neural:X", AudioFormatMapper.formatSoundMode("DTS+NEURAL:X"))
        assertEquals("DTS-ES Matrix 6.1", AudioFormatMapper.formatSoundMode("DTS ES MTRX6.1"))
        assertEquals("DTS-ES Discrete 6.1", AudioFormatMapper.formatSoundMode("DTS ES DSCRT6.1"))
        assertEquals("DTS 96/24", AudioFormatMapper.formatSoundMode("DTS 96/24"))
        assertEquals("DTS Express", AudioFormatMapper.formatSoundMode("DTS EXPRESS"))
        assertEquals("DTS Surround", AudioFormatMapper.formatSoundMode("DTS SURROUND"))
    }

    @Test
    fun `test multi-channel formats`() {
        assertEquals("Multi-Channel In + Neural:X",
            AudioFormatMapper.formatSoundMode("M CH IN+NEURAL:X"))
        assertEquals("Multi-Channel In + Dolby Surround",
            AudioFormatMapper.formatSoundMode("M CH IN+DS"))
        assertEquals("Multi-Channel In 7.1", AudioFormatMapper.formatSoundMode("MULTI CH IN 7.1"))
        assertEquals("Multi-Channel In", AudioFormatMapper.formatSoundMode("MULTI CH IN"))
        assertEquals("Multi-Channel Stereo", AudioFormatMapper.formatSoundMode("MCH STEREO"))
    }

    @Test
    fun `test simple modes`() {
        assertEquals("Neural:X", AudioFormatMapper.formatSoundMode("NEURAL:X"))
        assertEquals("Stereo", AudioFormatMapper.formatSoundMode("STEREO"))
        assertEquals("Direct", AudioFormatMapper.formatSoundMode("DIRECT"))
        assertEquals("Movie", AudioFormatMapper.formatSoundMode("MOVIE"))
        assertEquals("Music", AudioFormatMapper.formatSoundMode("MUSIC"))
        assertEquals("Game", AudioFormatMapper.formatSoundMode("GAME"))
        assertEquals("Auto", AudioFormatMapper.formatSoundMode("AUTO"))
    }

    @Test
    fun `test sound field modes`() {
        assertEquals("Rock Arena", AudioFormatMapper.formatSoundMode("ROCK ARENA"))
        assertEquals("Jazz Club", AudioFormatMapper.formatSoundMode("JAZZ CLUB"))
        assertEquals("Mono Movie", AudioFormatMapper.formatSoundMode("MONO MOVIE"))
        assertEquals("Matrix", AudioFormatMapper.formatSoundMode("MATRIX"))
        assertEquals("Video Game", AudioFormatMapper.formatSoundMode("VIDEO GAME"))
        assertEquals("Virtual", AudioFormatMapper.formatSoundMode("VIRTUAL"))
    }

    @Test
    fun `test signal detection formatting`() {
        assertEquals("HDMI", AudioFormatMapper.formatSignalDetect("HDMI"))
        assertEquals("Digital", AudioFormatMapper.formatSignalDetect("DIGITAL"))
        assertEquals("Analog", AudioFormatMapper.formatSignalDetect("ANALOG"))
        assertEquals("HDMI ARC", AudioFormatMapper.formatSignalDetect("ARC"))
        assertEquals(null, AudioFormatMapper.formatSignalDetect("NO"))
        assertEquals(null, AudioFormatMapper.formatSignalDetect("—"))
    }

    @Test
    fun `test digital mode formatting`() {
        assertEquals("Auto", AudioFormatMapper.formatDigitalMode("AUTO"))
        assertEquals("PCM", AudioFormatMapper.formatDigitalMode("PCM"))
        assertEquals("DTS", AudioFormatMapper.formatDigitalMode("DTS"))
    }

    @Test
    fun `test null handling`() {
        assertEquals(null, AudioFormatMapper.formatSoundMode(null))
        assertEquals(null, AudioFormatMapper.formatSignalDetect(null))
        assertEquals(null, AudioFormatMapper.formatDigitalMode(null))
    }

    @Test
    fun `test real-world example - current AVR state`() {
        // Based on actual AVR response: "DOLBY D+ +DS"
        assertEquals("Dolby Digital Plus + Dolby Surround",
            AudioFormatMapper.formatSoundMode("DOLBY D+ +DS"))
    }
}
