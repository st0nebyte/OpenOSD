package dev.st0nebyte.openosd

/**
 * Immutable snapshot of AVR state, populated from X1200W HTTP XML API.
 *
 * Field names match the <Tag><value>X</value></Tag> keys in:
 *   formMainZone_MainZoneXmlStatusLite.xml  (fast: power/vol/mute)
 *   formMainZone_MainZoneXml.xml            (rich: source name / ECO / sound mode)
 */
data class AVRState(
    val power:       Boolean = false,
    val volumeDb:    Double  = -80.0,   // actual dB value from XML, e.g. -18.0
    val muted:       Boolean = false,
    val source:      String  = "",      // InputFuncSelect – may be user-renamed ("FIRE TV")
    val soundMode:   String  = "",      // selectSurround – "Stereo", "Dolby Digital", etc.
    val ecoMode:     String  = "AUTO",  // ECOMode: AUTO / ON / OFF
    val multEQ:      String  = "",      // MultEQMode (if available)
    val dynEQ:       String  = "",      // DynamicEQ
    val dynVol:      String  = "",      // DynamicVolume
) {
    /** 0.0 – 1.0 for progress bar; volume range is -80 to +18 dB (raw 0–98) */
    val volumeNorm: Float
        get() = ((volumeDb + 80.0) / 98.0).toFloat().coerceIn(0f, 1f)

    val volumeString: String
        get() = when {
            volumeDb <= -79.5 -> "MIN"
            volumeDb >= 0     -> "+${"%.1f".format(volumeDb)} dB"
            else              -> "${"%.1f".format(volumeDb)} dB"
        }

    val sourceDisplay: String
        get() = source.ifBlank { "—" }

    val soundModeDisplay: String
        get() = soundMode.ifBlank { "—" }

    val signalInfo: String get() {
        val sm = soundMode.uppercase()
        return when {
            "ATMOS"          in sm -> "Dolby Audio – Atmos"
            "DOLBY HD"       in sm -> "Dolby Audio – TrueHD"
            "DOLBY D+"       in sm -> "Dolby Audio – DD+"
            "DOLBY D EX"     in sm -> "Dolby Digital EX"
            "DOLBY DIGITAL"  in sm -> "Dolby Audio – DD"
            "DOLBY SURROUND" in sm -> "Dolby Surround"
            "DTS:X MSTR"     in sm -> "DTS:X Master Audio"
            "DTS:X"          in sm -> "DTS:X"
            "DTS HD MSTR"    in sm -> "DTS-HD Master Audio"
            "DTS HD"         in sm -> "DTS-HD"
            "DTS ES"         in sm -> "DTS-ES"
            "DTS"            in sm -> "DTS"
            "NEURAL"         in sm -> "Neural:X"
            "PURE DIRECT"    in sm -> "Pure Direct"
            "DIRECT"         in sm -> "Direct"
            "MULTI CH STEREO" in sm -> "Multi Ch Stereo"
            "STEREO"         in sm -> "PCM / Stereo"
            else                   -> soundMode.ifBlank { "—" }
        }
    }

    val ecoDisplay: String get() = ecoMode.uppercase().ifBlank { "AUTO" }

    val multEQDisplay: String get() = when (multEQ.uppercase()) {
        "AUDYSSEY" -> "Audyssey"
        "BYP.LR"   -> "L/R Bypass"
        "FLAT"     -> "Flat"
        "MANUAL"   -> "Manual"
        "OFF"      -> "Off"
        else       -> multEQ.ifBlank { "—" }
    }

    /** Channels active in input signal (derived from sound mode) */
    val inputChannels: Set<String> get() {
        val sm = soundMode.uppercase()
        return when {
            "ATMOS" in sm || "DTS:X" in sm ->
                setOf("FL","FR","C","LFE","SL","SR","FHL","FHR")
            "ES" in sm || "D EX" in sm ->
                setOf("FL","FR","C","LFE","SL","SR","SBL","SBR")
            "MULTI CH STEREO" in sm ->
                setOf("FL","FR","C","SL","SR")
            "STEREO" in sm || "DIRECT" in sm ->
                setOf("FL","FR")
            else ->
                setOf("FL","FR","C","LFE","SL","SR")
        }
    }

    /** Active speaker outputs (derived from sound mode) */
    val activeChannels: Set<String> get() {
        val sm = soundMode.uppercase()
        return when {
            "ATMOS" in sm || "DTS:X" in sm ->
                setOf("FL","FR","C","SW","SL","SR","FDL","FDR")
            "STEREO" in sm || "DIRECT" in sm ->
                setOf("FL","FR")
            "MULTI CH STEREO" in sm ->
                setOf("FL","FR","C","SL","SR")
            else ->
                setOf("FL","FR","C","SW","SL","SR")
        }
    }
}

enum class OSDTrigger(val showFull: Boolean, val timeoutMs: Long) {
    VOLUME    (showFull = false, timeoutMs = 3_000),
    MUTE      (showFull = false, timeoutMs = 3_000),
    SOURCE    (showFull = true,  timeoutMs = 6_000),
    SOUND_MODE(showFull = true,  timeoutMs = 6_000),
    ECO       (showFull = true,  timeoutMs = 5_000),
    POWER_ON  (showFull = true,  timeoutMs = 8_000),
}
