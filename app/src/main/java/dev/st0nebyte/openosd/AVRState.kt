package dev.st0nebyte.openosd

/**
 * Minimal AVR state for volume-only OSD.
 * Populated from formMainZone_MainZoneXmlStatusLite.xml.
 */
data class AVRState(
    val power:    Boolean = false,
    val volumeDb: Double  = -80.0,  // -80.0 to +18.0 dB
    val muted:    Boolean = false,
) {
    /** 0.0 – 1.0 for progress bar; volume range is -80 to +18 dB (raw 0–98) */
    val volumeNorm: Float
        get() = ((volumeDb + 80.0) / 98.0).toFloat().coerceIn(0f, 1f)

    /** Display volume as 0-98 scale (Denon relative mode) */
    val volumeString: String
        get() {
            val raw = (volumeDb + 80.0).toInt().coerceIn(0, 98)
            return raw.toString()
        }
}

enum class OSDTrigger(val timeoutMs: Long) {
    VOLUME(timeoutMs = 3_000),
    MUTE  (timeoutMs = 3_000),
}
