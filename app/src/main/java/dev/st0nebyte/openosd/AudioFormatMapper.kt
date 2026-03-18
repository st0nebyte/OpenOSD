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
     *
     * Implementation: Uses a map sorted by key length (longest first) to ensure
     * specific patterns are matched before general ones (e.g., "DOLBY D+ +DS" before "DOLBY D+").
     * This prevents order-dependent bugs when adding new formats.
     */
    fun formatSoundMode(mode: String?): String? {
        if (mode == null) return null

        // Work with non-null string from here
        var formatted: String = mode

        // Map of format codes to display names
        // Automatically sorted by key length (longest first) to match specific patterns first
        val formatMap = mapOf(
            // Dolby formats (most specific first)
            "DOLBY D+ +DS" to "Dolby Digital Plus + Dolby Surround",
            "DOLBY D++DS" to "Dolby Digital Plus + Dolby Surround",
            "DOLBY D+ +NEURAL:X" to "Dolby Digital Plus + Neural:X",
            "DOLBY HD+DS" to "Dolby TrueHD + Dolby Surround",
            "DOLBY ATMOS" to "Dolby Atmos",
            "DOLBY D+DS" to "Dolby Digital + Dolby Surround",
            "DOLBY DIGITAL" to "Dolby Digital",
            "DOLBY SURROUND" to "Dolby Surround",
            "DOLBY D+" to "Dolby Digital Plus",
            "DOLBY HD" to "Dolby TrueHD",

            // DTS formats (most specific first)
            "DTS ES MTRX+NEURAL:X" to "DTS-ES Matrix + Neural:X",
            "DTS ES DSCRT+NEURAL:X" to "DTS-ES Discrete + Neural:X",
            "DTS HD+NEURAL:X" to "DTS-HD + Neural:X",
            "DTS ES 8CH DSCRT" to "DTS-ES 8CH Discrete",
            "DTS ES MTRX6.1" to "DTS-ES Matrix 6.1",
            "DTS ES DSCRT6.1" to "DTS-ES Discrete 6.1",
            "DTS96 ES MTRX" to "DTS 96 ES Matrix",
            "DTS HD MSTR" to "DTS-HD Master Audio",
            "DTS:X MSTR" to "DTS:X Master",
            "DTS+NEURAL:X" to "DTS + Neural:X",
            "DTS EXPRESS" to "DTS Express",
            "DTS SURROUND" to "DTS Surround",
            "DTS96/24" to "DTS 96/24",
            "DTS 96/24" to "DTS 96/24",
            "DTS:X" to "DTS:X",
            "DTS HD" to "DTS-HD High Resolution",

            // Multi-channel (most specific first)
            "M CH IN+NEURAL:X" to "Multi-Channel In + Neural:X",
            "M CH IN+DS" to "Multi-Channel In + Dolby Surround",
            "MULTI CH IN 7.1" to "Multi-Channel In 7.1",
            "MULTI CH IN" to "Multi-Channel In",
            "MCH STEREO" to "Multi-Channel Stereo",

            // Sound field modes (must be before simple modes!)
            "ROCK ARENA" to "Rock Arena",
            "JAZZ CLUB" to "Jazz Club",
            "MONO MOVIE" to "Mono Movie",
            "VIDEO GAME" to "Video Game",
            "MATRIX" to "Matrix",
            "VIRTUAL" to "Virtual",

            // Simple modes (shortest, last priority)
            "NEURAL:X" to "Neural:X",
            "STEREO" to "Stereo",
            "DIRECT" to "Direct",
            "MOVIE" to "Movie",
            "MUSIC" to "Music",
            "GAME" to "Game",
            "AUTO" to "Auto",
        )

        // Sort by key length (longest first) to match specific patterns before general ones
        val sortedFormats = formatMap.entries.sortedByDescending { it.key.length }

        // Apply replacements in order (longest matches first)
        for ((pattern, replacement) in sortedFormats) {
            formatted = formatted.replace(pattern, replacement)
        }

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
