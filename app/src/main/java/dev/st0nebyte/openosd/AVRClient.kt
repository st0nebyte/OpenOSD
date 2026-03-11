package dev.st0nebyte.openosd

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG            = "AVRClient"
private const val POLL_NORMAL_MS = 400L   // Normal polling
private const val POLL_TURBO_MS  = 150L   // Fast polling during volume changes
private const val TIMEOUT_MS     = 2500
private const val TURBO_DURATION = 20     // Stay in turbo mode for N polls (~3 seconds)

class AVRClient(
    private val host: String,
    private val onUpdate: (AVRState, OSDTrigger) -> Unit,
    private val onConnected: (Boolean) -> Unit,
) : IAVRClient {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var thread: Thread? = null
    private var current = AVRState()
    private var firstConnect = true
    private var turboCountdown = 0  // Countdown for turbo mode

    override fun start() {
        running = true
        thread = Thread({ pollLoop() }, "AVRPoll").also {
            it.isDaemon = true; it.start()
        }
    }

    override fun stop() { running = false; thread?.interrupt() }

    private fun pollLoop() {
        while (running) {
            try {
                // Only fetch lite XML (power/volume/mute)
                val fields = fetchXml("/goform/formMainZone_MainZoneXmlStatusLite.xml")
                mainHandler.post {
                    onConnected(true)
                    applyFields(fields)
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.w(TAG, "Poll error: ${e.message}")
                mainHandler.post { onConnected(false) }
                sleepSafe(3000)
            }

            // Adaptive polling: faster during volume changes
            val pollInterval = if (turboCountdown > 0) {
                turboCountdown--
                POLL_TURBO_MS
            } else {
                POLL_NORMAL_MS
            }
            sleepSafe(pollInterval)
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

        // Parse only volume-relevant fields
        f["Power"]?.let { next = next.copy(power = it.equals("ON", true)) }
        f["MasterVolume"]?.takeIf { it != "--" }?.toDoubleOrNull()
            ?.let { next = next.copy(volumeDb = it) }
        f["Mute"]?.let { next = next.copy(muted = it.equals("ON", true)) }

        current = next

        // On first successful poll: show volume if powered on
        if (firstConnect) {
            firstConnect = false
            Log.d(TAG, "First connect: power=${next.power} vol=${next.volumeDb}")
            if (next.power) onUpdate(next, OSDTrigger.VOLUME)
            return
        }

        if (next == old) return

        // Determine trigger (priority order: mute > volume > source > mode)
        val trigger = when {
            !old.power && next.power                       -> OSDTrigger.VOLUME
            old.muted != next.muted                        -> OSDTrigger.MUTE
            Math.abs(old.volumeDb - next.volumeDb) > 0.1   -> OSDTrigger.VOLUME
            old.inputSource != next.inputSource            -> OSDTrigger.SOURCE
            old.soundMode != next.soundMode                -> OSDTrigger.SOUND_MODE
            else                                           -> return  // No relevant change
        }

        // Enable turbo mode for volume/mute changes only
        if (trigger == OSDTrigger.VOLUME || trigger == OSDTrigger.MUTE) {
            turboCountdown = TURBO_DURATION
        }

        if (next.power) onUpdate(next, trigger)
    }
}
