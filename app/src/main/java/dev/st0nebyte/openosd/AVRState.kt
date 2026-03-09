package dev.st0nebyte.openosd

/**
 * AVR state for OSD display.
 * Populated from Telnet push updates.
 */
data class AVRState(
    val power:         Boolean = false,
    val volumeDb:      Double  = -80.0,  // -80.0 to +18.0 dB
    val muted:         Boolean = false,
    val soundMode:     String? = null,   // STEREO, DIRECT, DOLBY SURROUND, etc.
    val inputSource:   String? = null,   // GAME, DVD, TV, SAT/CBL, etc.
    val signalDetect:  String? = null,   // HDMI, DIGITAL, ANALOG, ARC
    val digitalMode:   String? = null,   // AUTO, PCM, DTS
    val speakers:      List<String> = emptyList(),  // Active speakers: FL, FR, C, SW, SL, SR, etc.
) {
    /** 0.0 – 1.0 for progress bar; volume range is -80 to +18 dB (raw 0–98) */
    val volumeNorm: Float
        get() = ((volumeDb + 80.0) / 98.0).toFloat().coerceIn(0f, 1f)

    /** Display volume as 0-98 scale (Denon relative mode) with .5 decimals */
    val volumeString: String
        get() {
            val raw = (volumeDb + 80.0).coerceIn(0.0, 98.0)
            // Show decimal if volume has .5 step
            return if (raw % 1.0 >= 0.1) {  // Has decimal part
                val intPart = raw.toInt()
                "$intPart.5"  // Always .5 for Denon
            } else {
                raw.toInt().toString()
            }
        }
}

enum class OSDTrigger(val timeoutMs: Long) {
    VOLUME(timeoutMs = 3_000),
    MUTE  (timeoutMs = 3_000),
}

enum class OSDDisplayMode {
    STANDARD,      // Volume only
    INFO,          // Volume + Sound Mode + Input Source
    EXTENDED       // Volume + Sound Mode + Input Source + Signal + Speakers
}

enum class OSDScale {
    SMALL,         // Compact, minimal screen coverage
    MEDIUM,        // Balanced size
    LARGE          // Maximum visibility
}
