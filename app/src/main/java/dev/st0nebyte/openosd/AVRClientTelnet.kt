package dev.st0nebyte.openosd

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

private const val TAG = "AVRClientTelnet"
private const val DENON_PORT = 23
private const val CONNECT_TIMEOUT_MS = 2000  // Reduced to avoid ANR
private const val READ_TIMEOUT_MS = 0  // No timeout for push updates
private const val RECONNECT_DELAY_MS = 3000L  // Faster reconnect

/**
 * Telnet-based AVR client with push updates (instant, 0ms lag).
 *
 * Protocol: Text-based commands over TCP port 23
 * - Commands end with \r (carriage return)
 * - AVR sends unsolicited status updates (push mode)
 * - Single client connection only
 *
 * Example commands:
 * - PW?        Query power status
 * - PWON       Power on
 * - MV?        Query volume
 * - MV50       Set volume to 50
 * - MVUP/DOWN  Volume up/down
 *
 * Responses from AVR:
 * - PW<status>  (PWON, PWSTANDBY)
 * - MV<value>   (MV50, MV355 = 35.5)
 * - MU<status>  (MUON, MUOFF)
 */
class AVRClientTelnet(
    private val host: String,
    private val context: android.content.Context,
    private val onUpdate: (AVRState, OSDTrigger) -> Unit,
    private val onConnected: (Boolean) -> Unit,
) : IAVRClient {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile private var connected = false
    private var thread: Thread? = null
    private var current = AVRState()
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private val activeSpeakers = mutableListOf<String>()

    override fun start() {
        running = true
        thread = Thread({ connectionLoop() }, "AVRTelnet").also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        thread?.interrupt()
        // Close connection async to avoid blocking caller
        Thread {
            try {
                Thread.sleep(100)  // Let thread exit first
            } catch (e: InterruptedException) {
                // Interrupted during shutdown, just close immediately
            }
            closeConnection()
        }.start()
    }

    private fun connectionLoop() {
        while (running) {
            try {
                connect()
                if (connected) {
                    queryInitialState()
                    readLoop()
                }
            } catch (e: InterruptedException) {
                Log.i(TAG, "Connection loop interrupted, stopping...")
                running = false
                break
            } catch (e: Exception) {
                Log.w(TAG, "Connection error: ${e.message}")
                mainHandler.post { onConnected(false) }
                closeConnection()
            }

            if (running && !connected) {
                try {
                    Log.d(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms...")
                    Thread.sleep(RECONNECT_DELAY_MS)
                } catch (e: InterruptedException) {
                    Log.i(TAG, "Sleep interrupted, stopping...")
                    running = false
                    break
                }
            }
        }
        Log.d(TAG, "Connection loop exited")
    }

    private fun connect() {
        Log.d(TAG, "Connecting to $host:$DENON_PORT...")
        socket = Socket().apply {
            soTimeout = READ_TIMEOUT_MS
            connect(InetSocketAddress(host, DENON_PORT), CONNECT_TIMEOUT_MS)
        }
        writer = OutputStreamWriter(socket!!.getOutputStream(), "UTF-8")
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream(), "UTF-8"))
        connected = true
        mainHandler.post { onConnected(true) }
        Log.d(TAG, "Connected!")
    }

    private fun closeConnection() {
        connected = false
        runCatching { reader?.close() }
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        reader = null
        writer = null
        socket = null
    }

    private fun sendCommand(command: String) {
        if (!connected) return
        try {
            writer?.write("$command\r")
            writer?.flush()
            Log.d(TAG, "→ $command")
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
            connected = false
        }
    }

    private fun queryInitialState() {
        // Clear speaker list from previous connection
        activeSpeakers.clear()

        // Query current state on connect
        sendCommand("PW?")        // Power
        sendCommand("MV?")        // Master Volume
        sendCommand("MU?")        // Mute
        sendCommand("SI?")        // Input Source
        sendCommand("MS?")        // Sound Mode
        sendCommand("SD?")        // Signal Detection (HDMI/DIGITAL/ANALOG)
        sendCommand("DC?")        // Digital Mode (AUTO/PCM/DTS)
        sendCommand("CV?")        // Channel Volume (returns active speakers)
        sendCommand("PSDRC ?")    // Dynamic Range Compression
        sendCommand("PSRSTR ?")   // Audio Restorer
        sendCommand("VSAUDIO ?")  // HDMI Audio Output
        sendCommand("ECO?")       // ECO Mode
    }

    private fun readLoop() {
        while (running && connected) {
            try {
                val line = reader?.readLine() ?: break
                if (line.isBlank()) continue
                Log.d(TAG, "← $line")
                processResponse(line.trim())
            } catch (e: SocketTimeoutException) {
                // Normal for blocking read, continue
            } catch (e: Exception) {
                Log.e(TAG, "Read error: ${e.message}")
                break
            }
        }
        connected = false
    }

    private fun processResponse(response: String) {
        val old = current
        var next = old

        when {
            // Power: PWON, PWSTANDBY
            response.startsWith("PW") -> {
                val powered = response == "PWON"
                next = next.copy(power = powered)
            }

            // Volume: MV27 = 27, MV275 = 27.5, MVMAX ignored
            response.startsWith("MV") && !response.startsWith("MVMAX") -> {
                val volStr = response.substring(2).trim()
                val volRaw = volStr.toIntOrNull() ?: return

                // Denon format: 2 digits (27 = 27) or 3 digits (275 = 27.5)
                val volumeActual = if (volStr.length == 3) {
                    volRaw / 10.0  // 3-digit: 275 → 27.5
                } else {
                    volRaw.toDouble()  // 2-digit: 27 → 27.0
                }

                // Convert to dB: range is -80 to +18 (relative 0-98 maps to this)
                val volumeDb = volumeActual - 80.0
                next = next.copy(volumeDb = volumeDb)
            }

            // Mute: MUON, MUOFF
            response.startsWith("MU") -> {
                val muted = response == "MUON"
                next = next.copy(muted = muted)
            }

            // Input Source: SIGAME, SIDVD, SITV, etc.
            // Use user-configurable display names
            response.startsWith("SI") -> {
                val sourceCode = response.substring(2).trim()
                val displayName = SourceNameMapping.getDisplayName(context, sourceCode)
                next = next.copy(inputSource = displayName)
            }

            // Sound Mode: MSSTEREO, MSDIRECT, MSDOLBY SURROUND, etc.
            // Keep full format names (no abbreviations)
            response.startsWith("MS") -> {
                val mode = response.substring(2).trim()
                next = next.copy(soundMode = mode)
            }

            // Signal Detection: SDHDMI, SDDIGITAL, SDANALOG, SDARC, SDNO
            response.startsWith("SD") -> {
                val signal = response.substring(2).trim()
                    .replace("NO", "—")  // No signal
                next = next.copy(signalDetect = signal)
            }

            // Digital Mode: DCAUTO, DCPCM, DCDTS
            response.startsWith("DC") -> {
                val digital = response.substring(2).trim()
                next = next.copy(digitalMode = digital)
            }

            // Channel Volume: CV** responses until CVEND
            response.startsWith("CV") -> {
                when {
                    response == "CVEND" -> {
                        // End of CV list - update state with collected speakers
                        next = next.copy(speakers = activeSpeakers.toList())
                        activeSpeakers.clear()
                    }
                    response.matches(Regex("^CV[A-Z]{1,3} \\d+$")) -> {
                        // CV response: CVFL 50, CVFR 50, etc.
                        val speaker = response.substring(2).split(" ")[0]
                        activeSpeakers.add(speaker)
                    }
                }
                return  // Don't trigger update yet for CV responses
            }

            // Dynamic Range Compression: PSDRC OFF, PSDRC AUTO, PSDRC LOW, PSDRC MID, PSDRC HI
            // Note: DRC changes update state but don't trigger OSD display (only shown in EXTENDED mode)
            response.startsWith("PSDRC ") -> {
                val drc = response.substring(6).trim()
                next = next.copy(drc = drc)
            }

            // Audio Restorer: PSRSTR OFF, PSRSTR LOW, PSRSTR MED, PSRSTR HI
            // Note: Restorer changes update state but don't trigger OSD display
            response.startsWith("PSRSTR ") -> {
                val restorer = response.substring(7).trim()
                next = next.copy(audioRestorer = restorer)
            }

            // HDMI Audio Output: VSAUDIO AMP, VSAUDIO TV
            // Note: HDMI output changes update state but don't trigger OSD display
            response.startsWith("VSAUDIO ") -> {
                val hdmiOut = response.substring(8).trim()
                next = next.copy(hdmiAudioOut = hdmiOut)
            }

            // ECO Mode: ECOON, ECOAUTO, ECOOFF
            // Note: ECO changes update state but don't trigger OSD display
            response.startsWith("ECO") -> {
                val eco = when (response) {
                    "ECOON" -> "ON"
                    "ECOAUTO" -> "AUTO"
                    "ECOOFF" -> "OFF"
                    else -> response.substring(3).trim()  // Fallback for unknown values
                }
                next = next.copy(ecoMode = eco)
            }

            else -> return  // Ignore unknown responses
        }

        current = next

        if (next == old) return

        // Determine trigger (priority order: mute/unmute > volume > source > mode)
        val trigger = when {
            !old.power && next.power                       -> OSDTrigger.VOLUME
            old.muted != next.muted && next.muted          -> OSDTrigger.MUTE    // Muted
            old.muted != next.muted && !next.muted         -> OSDTrigger.UNMUTE  // Unmuted
            Math.abs(old.volumeDb - next.volumeDb) > 0.1   -> OSDTrigger.VOLUME
            old.inputSource != next.inputSource            -> OSDTrigger.SOURCE
            old.soundMode != next.soundMode                -> OSDTrigger.SOUND_MODE
            else                                           -> return  // No relevant change
        }

        if (next.power) {
            mainHandler.post { onUpdate(next, trigger) }
        }
    }
}
