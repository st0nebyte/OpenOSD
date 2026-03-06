package dev.st0nebyte.openosd

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG         = "AVRClient"
private const val POLL_FAST_MS = 400L
private const val POLL_FULL_N  = 12
private const val TIMEOUT_MS   = 2500

class AVRClient(
    private val host: String,
    private val onUpdate: (AVRState, OSDTrigger) -> Unit,
    private val onConnected: (Boolean) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var thread: Thread? = null
    private var current = AVRState()
    private var firstConnect = true

    fun start() {
        running = true
        thread = Thread({ pollLoop() }, "AVRPoll").also {
            it.isDaemon = true; it.start()
        }
    }

    fun stop() { running = false; thread?.interrupt() }

    private fun pollLoop() {
        var tick = 0
        var cachedFull: Map<String, String> = emptyMap()

        while (running) {
            try {
                val lite = fetchXml("/goform/formMainZone_MainZoneXmlStatusLite.xml")
                if (tick % POLL_FULL_N == 0) {
                    cachedFull = fetchXml("/goform/formMainZone_MainZoneXml.xml")
                }
                tick++
                val merged = cachedFull + lite
                mainHandler.post {
                    onConnected(true)
                    applyFields(merged)
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.w(TAG, "Poll error: ${e.message}")
                mainHandler.post { onConnected(false) }
                sleepSafe(3000)
            }
            sleepSafe(POLL_FAST_MS)
        }
    }

    private fun sleepSafe(ms: Long) {
        try { Thread.sleep(ms) } catch (_: InterruptedException) { running = false }
    }

    private fun fetchXml(path: String): Map<String, String> {
        val conn = (URL("http://$host$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS; requestMethod = "GET"
        }
        return try {
            parseXml(BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() })
        } finally { conn.disconnect() }
    }

    private fun parseXml(xml: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        Regex("""<([A-Za-z][A-Za-z0-9_]*)>\s*<value>([^<]*)</value>""")
            .findAll(xml).forEach { map[it.groupValues[1]] = it.groupValues[2].trim() }
        return map
    }

    private fun applyFields(f: Map<String, String>) {
        val old = current
        var next = old

        f["Power"]?.let           { next = next.copy(power     = it.equals("ON", true)) }
        f["MasterVolume"]?.takeIf { it != "--" }?.toDoubleOrNull()
                                  ?.let { next = next.copy(volumeDb  = it) }
        f["Mute"]?.let            { next = next.copy(muted     = it.equals("ON", true)) }
        f["InputFuncSelect"]?.let { next = next.copy(source    = it.trim()) }
        f["selectSurround"]?.let  { next = next.copy(soundMode = it.trim()) }
        f["ECOMode"]?.let         { next = next.copy(ecoMode   = it.trim()) }
        f["MultEQMode"]?.let      { next = next.copy(multEQ    = it.trim()) }
        f["DynamicEQ"]?.let       { next = next.copy(dynEQ     = it.trim()) }
        f["DynamicVolume"]?.let   { next = next.copy(dynVol    = it.trim()) }

        current = next

        // On first successful poll: always show full state regardless of changes
        if (firstConnect) {
            firstConnect = false
            Log.d(TAG, "First connect – forcing display: power=${next.power} vol=${next.volumeDb} src=${next.source}")
            if (next.power) onUpdate(next, OSDTrigger.POWER_ON)
            return
        }

        if (next == old) return

        val trigger = when {
            !old.power && next.power                       -> OSDTrigger.POWER_ON
            old.muted     != next.muted                    -> OSDTrigger.MUTE
            Math.abs(old.volumeDb - next.volumeDb) > 0.1   -> OSDTrigger.VOLUME
            old.source    != next.source                   -> OSDTrigger.SOURCE
            old.soundMode != next.soundMode                -> OSDTrigger.SOUND_MODE
            old.ecoMode   != next.ecoMode                  -> OSDTrigger.ECO
            else                                           -> OSDTrigger.SOUND_MODE
        }

        if (next.power || trigger == OSDTrigger.POWER_ON) onUpdate(next, trigger)
    }
}
