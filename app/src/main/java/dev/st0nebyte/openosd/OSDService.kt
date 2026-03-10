package dev.st0nebyte.openosd

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast

private const val TAG = "OSDService"
private const val CH  = "openosd_prefs"
private const val NID = 1

class OSDService : Service() {

    private lateinit var wm:  WindowManager
    private lateinit var osd: OSDView
    private var attached = false

    private val handler    = Handler(Looper.getMainLooper())
    private var client:    IAVRClient? = null
    private var hideTimer: Runnable?  = null
    private var isClientConnected = false

    override fun onCreate() {
        super.onCreate()
        wm  = getSystemService(WINDOW_SERVICE) as WindowManager
        osd = OSDView(this)
        updateDisplayMode()
        createChannel()
    }

    private fun updateDisplayMode() {
        val modeName = prefs().getString(KEY_DISPLAY_MODE, "STANDARD") ?: "STANDARD"
        osd.displayMode = try {
            OSDDisplayMode.valueOf(modeName)
        } catch (e: Exception) {
            OSDDisplayMode.STANDARD
        }

        val scaleName = prefs().getString(KEY_SCALE, "MEDIUM") ?: "MEDIUM"
        osd.scale = try {
            OSDScale.valueOf(scaleName)
        } catch (e: Exception) {
            OSDScale.MEDIUM
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NID, buildNotif("Starte…"))
        val host = intent?.getStringExtra(KEY_HOST)
            ?: prefs().getString(KEY_HOST, "192.168.178.130") ?: "192.168.178.130"
        if (host.isBlank()) { stopSelf(); return START_NOT_STICKY }
        toast("Verbinde mit $host…")
        prefs().edit().putString(KEY_HOST, host).apply()
        client?.stop()

        // Auto-detect: Try Telnet first, fallback to HTTP if port 23 is blocked
        // - AVRClientTelnet: Push updates via Telnet Port 23 (instant, 0ms lag)
        //   Requires: Port 23 unlocked (hardware reset on X-series, see TELNET.md)
        // - AVRClient: HTTP polling (compatible, works everywhere, ~500ms lag)

        // Try Telnet connection first
        isClientConnected = false
        client = AVRClientTelnet(host, this, ::onUpdate, ::onConnected).also { it.start() }

        // After 5 seconds, check if Telnet connected - if not, fallback to HTTP
        handler.postDelayed({
            if (!isClientConnected && client is AVRClientTelnet) {
                Log.i(TAG, "Telnet failed (Port 23 blocked), switching to HTTP fallback...")
                toast("Port 23 gesperrt - nutze HTTP (langsamer)")
                client?.stop()
                client = AVRClient(host, ::onUpdate, ::onConnected).also { it.start() }
            }
        }, 5000)

        return START_STICKY
    }

    override fun onDestroy() { client?.stop(); detachOverlay(); super.onDestroy() }
    override fun onBind(i: Intent?) = null

    private fun onUpdate(state: AVRState, trigger: OSDTrigger) {
        Log.d(TAG, "onUpdate trigger=$trigger power=${state.power} vol=${state.volumeDb}")
        if (!state.power) { hide(); return }
        osd.state = state
        osd.animateVolume(state.volumeNorm)
        show(trigger.timeoutMs)
    }

    private fun onConnected(connected: Boolean) {
        isClientConnected = connected
        val protocol = if (client is AVRClientTelnet) "Telnet" else "HTTP"
        notify(if (connected) "Verbunden ✓ ($protocol)" else "Verbinde…")
    }

    private fun show(timeoutMs: Long) {
        if (!Settings.canDrawOverlays(this)) { toast("Overlay-Berechtigung fehlt!"); return }
        updateDisplayMode()  // Refresh display mode in case settings changed
        if (!attached) {
            try { wm.addView(osd, makeParams()); attached = true }
            catch (e: Exception) { Log.e(TAG, "addView: ${e.message}"); return }
        } else {
            // Update layout params if already attached (display mode might have changed)
            try { wm.updateViewLayout(osd, makeParams()) }
            catch (e: Exception) { Log.e(TAG, "updateViewLayout: ${e.message}") }
        }
        osd.visibility = View.VISIBLE
        osd.alpha = 1f
        hideTimer?.let { handler.removeCallbacks(it) }
        val r = Runnable { fadeOut() }
        hideTimer = r
        handler.postDelayed(r, timeoutMs)
    }

    private fun fadeOut() {
        osd.animate().alpha(0f).setDuration(350)
            .withEndAction { osd.visibility = View.GONE; osd.alpha = 1f }.start()
    }

    private fun hide() {
        hideTimer?.let { handler.removeCallbacks(it) }
        if (attached) osd.visibility = View.GONE
    }

    private fun detachOverlay() {
        if (attached) { runCatching { wm.removeView(osd) }; attached = false }
    }

    private fun makeParams(): WindowManager.LayoutParams {
        // Full-screen overlay (but transparent) - allows drawing volume bottom + info top-left
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,  // Full width
            WindowManager.LayoutParams.MATCH_PARENT,  // Full height
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START  // Full screen from top-left
        }
    }

    private fun toast(msg: String) = handler.post {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CH, "Denon OSD", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "AVR Overlay"; setShowBadge(false) }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
    }

    private fun buildNotif(status: String) =
        Notification.Builder(this, CH)
            .setContentTitle("Denon OSD – $status")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .build()

    private fun notify(status: String) =
        getSystemService(NotificationManager::class.java)?.notify(NID, buildNotif(status))

    private fun prefs() = getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_HOST = "avr_host"
        const val KEY_DISPLAY_MODE = "osd_display_mode"
        const val KEY_SCALE = "osd_scale"
        fun start(ctx: Context, host: String) {
            ctx.getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)
                .edit().putString(KEY_HOST, host).apply()
            ctx.startForegroundService(Intent(ctx, OSDService::class.java).putExtra(KEY_HOST, host))
        }
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OSDService::class.java))
    }
}
