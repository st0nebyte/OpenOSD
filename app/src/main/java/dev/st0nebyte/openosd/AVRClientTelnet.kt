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
private const val CONNECT_TIMEOUT_MS = 5000
private const val READ_TIMEOUT_MS = 0  // No timeout for push updates
private const val RECONNECT_DELAY_MS = 5000L

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
    private val onUpdate: (AVRState, OSDTrigger) -> Unit,
    private val onConnected: (Boolean) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile private var connected = false
    private var thread: Thread? = null
    private var current = AVRState()
    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    fun start() {
        running = true
        thread = Thread({ connectionLoop() }, "AVRTelnet").also {
            it.isDaemon = true
            it.start()
        }
    }

    fun stop() {
        running = false
        closeConnection()
        thread?.interrupt()
    }

    private fun connectionLoop() {
        while (running) {
            try {
                connect()
                if (connected) {
                    queryInitialState()
                    readLoop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection error: ${e.message}")
                mainHandler.post { onConnected(false) }
                closeConnection()
            }

            if (running && !connected) {
                Log.d(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms...")
                Thread.sleep(RECONNECT_DELAY_MS)
            }
        }
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
        // Query current state on connect
        sendCommand("PW?")   // Power
        sendCommand("MV?")   // Master Volume
        sendCommand("MU?")   // Mute
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

            // Volume: MV50, MV355 (35.5), MVMAX (98)
            response.startsWith("MV") && !response.startsWith("MVMAX") -> {
                val volStr = response.substring(2).trim()
                val volRaw = volStr.toIntOrNull() ?: return

                // Convert Denon format to dB
                // Denon sends: 0-98 as volume values (corresponds to -80dB to +18dB)
                val volumeDb = volRaw - 80.0
                next = next.copy(volumeDb = volumeDb)
            }

            // Mute: MUON, MUOFF
            response.startsWith("MU") -> {
                val muted = response == "MUON"
                next = next.copy(muted = muted)
            }

            else -> return  // Ignore unknown responses
        }

        current = next

        if (next == old) return

        // Determine trigger
        val trigger = when {
            !old.power && next.power                       -> OSDTrigger.VOLUME
            old.muted != next.muted                        -> OSDTrigger.MUTE
            Math.abs(old.volumeDb - next.volumeDb) > 0.1   -> OSDTrigger.VOLUME
            else                                           -> return
        }

        if (next.power) {
            mainHandler.post { onUpdate(next, trigger) }
        }
    }
}
