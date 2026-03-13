package dev.st0nebyte.openosd

/**
 * AVR state for OSD display.
 * Populated from Telnet push updates.
 */
data class AVRState(
    val power:         Boolean = false,
    val volumeDb:      Double  = -80.0,  // -80.0 to +18.0 dB
    val muted:         Boolean = false,
    val soundMode:     String? = null,   // Full format name: DOLBY ATMOS, DTS:X MSTR, etc.
    val inputSource:   String? = null,   // GAME, DVD, TV, SAT/CBL, etc.
    val signalDetect:  String? = null,   // HDMI, DIGITAL, ANALOG, ARC
    val digitalMode:   String? = null,   // AUTO, PCM, DTS
    val speakers:      List<String> = emptyList(),  // Active speakers: FL, FR, C, SW, SL, SR, etc.
    val drc:           String? = null,   // Dynamic Range Compression: OFF, AUTO, LOW, MID, HI
    val audioRestorer: String? = null,   // Audio Restorer: OFF, LOW, MED, HI
    val hdmiAudioOut:  String? = null,   // HDMI Audio Output: AMP, TV
    val ecoMode:       String? = null,   // ECO mode: OFF, ON, AUTO
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

enum class OSDTrigger(val timeoutMs: Long, val showVolume: Boolean, val showInfo: Boolean, val persistWhileMuted: Boolean = false) {
    VOLUME     (timeoutMs = 3_000, showVolume = true,  showInfo = false),   // Only volume bar
    MUTE       (timeoutMs = 0,     showVolume = true,  showInfo = true,  persistWhileMuted = true),  // Both elements, no timeout while muted
    UNMUTE     (timeoutMs = 350,   showVolume = false, showInfo = false),   // Trigger fade-out
    SOURCE     (timeoutMs = 5_000, showVolume = false, showInfo = true),    // Only info box (source changed)
    SOUND_MODE (timeoutMs = 5_000, showVolume = false, showInfo = true),    // Only info box (mode changed)
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
