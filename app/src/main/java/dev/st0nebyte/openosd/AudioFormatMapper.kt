package dev.st0nebyte.openosd

/**
 * Maps Denon AVR audio format codes to human-readable names.
 * Expands abbreviations for better readability in OSD.
 */
object AudioFormatMapper {

    /**
     * Convert AVR sound mode code to readable format.
     *
     * Examples:
     * - "DOLBY D+ +DS" → "Dolby Digital Plus + Dolby Surround"
     * - "DTS+NEURAL:X" → "DTS + Neural:X"
     * - "STEREO" → "Stereo"
     */
    fun formatSoundMode(mode: String?): String? {
        if (mode == null) return null

        // Apply replacements in order (most specific first)
        var formatted = mode

        // Dolby formats
        formatted = formatted.replace("DOLBY ATMOS", "Dolby Atmos")
        formatted = formatted.replace("DOLBY D+ +DS", "Dolby Digital Plus + Dolby Surround")
        formatted = formatted.replace("DOLBY D++DS", "Dolby Digital Plus + Dolby Surround")
        formatted = formatted.replace("DOLBY D+ +NEURAL:X", "Dolby Digital Plus + Neural:X")
        formatted = formatted.replace("DOLBY D+DS", "Dolby Digital + Dolby Surround")
        formatted = formatted.replace("DOLBY D+", "Dolby Digital Plus")
        formatted = formatted.replace("DOLBY HD+DS", "Dolby TrueHD + Dolby Surround")
        formatted = formatted.replace("DOLBY HD", "Dolby TrueHD")
        formatted = formatted.replace("DOLBY DIGITAL", "Dolby Digital")
        formatted = formatted.replace("DOLBY SURROUND", "Dolby Surround")

        // DTS formats
        formatted = formatted.replace("DTS:X MSTR", "DTS:X Master")
        formatted = formatted.replace("DTS:X", "DTS:X")
        formatted = formatted.replace("DTS HD MSTR", "DTS-HD Master Audio")
        formatted = formatted.replace("DTS HD+NEURAL:X", "DTS-HD + Neural:X")
        formatted = formatted.replace("DTS HD", "DTS-HD High Resolution")
        formatted = formatted.replace("DTS+NEURAL:X", "DTS + Neural:X")
        formatted = formatted.replace("DTS ES MTRX6.1", "DTS-ES Matrix 6.1")
        formatted = formatted.replace("DTS ES DSCRT6.1", "DTS-ES Discrete 6.1")
        formatted = formatted.replace("DTS ES MTRX+NEURAL:X", "DTS-ES Matrix + Neural:X")
        formatted = formatted.replace("DTS ES DSCRT+NEURAL:X", "DTS-ES Discrete + Neural:X")
        formatted = formatted.replace("DTS 96/24", "DTS 96/24")
        formatted = formatted.replace("DTS EXPRESS", "DTS Express")
        formatted = formatted.replace("DTS SURROUND", "DTS Surround")

        // Multi-channel
        formatted = formatted.replace("M CH IN+NEURAL:X", "Multi-Channel In + Neural:X")
        formatted = formatted.replace("M CH IN+DS", "Multi-Channel In + Dolby Surround")
        formatted = formatted.replace("MULTI CH IN 7.1", "Multi-Channel In 7.1")
        formatted = formatted.replace("MULTI CH IN", "Multi-Channel In")
        formatted = formatted.replace("MCH STEREO", "Multi-Channel Stereo")

        // Sound field modes (must be before simple modes!)
        formatted = formatted.replace("ROCK ARENA", "Rock Arena")
        formatted = formatted.replace("JAZZ CLUB", "Jazz Club")
        formatted = formatted.replace("MONO MOVIE", "Mono Movie")
        formatted = formatted.replace("VIDEO GAME", "Video Game")
        formatted = formatted.replace("MATRIX", "Matrix")
        formatted = formatted.replace("VIRTUAL", "Virtual")

        // Simple modes
        formatted = formatted.replace("NEURAL:X", "Neural:X")
        formatted = formatted.replace("STEREO", "Stereo")
        formatted = formatted.replace("DIRECT", "Direct")
        formatted = formatted.replace("MOVIE", "Movie")
        formatted = formatted.replace("MUSIC", "Music")
        formatted = formatted.replace("GAME", "Game")
        formatted = formatted.replace("AUTO", "Auto")

        return formatted
    }

    /**
     * Format signal detection info.
     *
     * Examples:
     * - "HDMI" → "HDMI"
     * - "DIGITAL" → "Digital"
     * - "ANALOG" → "Analog"
     */
    fun formatSignalDetect(signal: String?): String? {
        if (signal == null) return null

        return when (signal) {
            "HDMI" -> "HDMI"
            "DIGITAL" -> "Digital"
            "ANALOG" -> "Analog"
            "ARC" -> "HDMI ARC"
            "NO", "—" -> null  // Hide "No signal"
            else -> signal
        }
    }

    /**
     * Format digital mode.
     *
     * Examples:
     * - "AUTO" → "Auto"
     * - "PCM" → "PCM"
     * - "DTS" → "DTS"
     */
    fun formatDigitalMode(mode: String?): String? {
        if (mode == null) return null

        return when (mode) {
            "AUTO" -> "Auto"
            "PCM" -> "PCM"
            "DTS" -> "DTS"
            else -> mode
        }
    }
}
