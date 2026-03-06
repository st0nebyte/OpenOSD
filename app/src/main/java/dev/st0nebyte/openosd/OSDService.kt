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
    private var client:    AVRClient? = null
    private var hideTimer: Runnable?  = null

    override fun onCreate() {
        super.onCreate()
        wm  = getSystemService(WINDOW_SERVICE) as WindowManager
        osd = OSDView(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NID, buildNotif("Starte…"))
        val host = intent?.getStringExtra(KEY_HOST)
            ?: prefs().getString(KEY_HOST, "192.168.178.130") ?: "192.168.178.130"
        if (host.isBlank()) { stopSelf(); return START_NOT_STICKY }
        toast("Verbinde mit $host…")
        prefs().edit().putString(KEY_HOST, host).apply()
        client?.stop()
        client = AVRClient(host, ::onUpdate, ::onConnected).also { it.start() }
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
        notify(if (connected) "Verbunden ✓" else "Verbinde…")
    }

    private fun show(timeoutMs: Long) {
        if (!Settings.canDrawOverlays(this)) { toast("Overlay-Berechtigung fehlt!"); return }
        if (!attached) {
            try { wm.addView(osd, makeParams()); attached = true }
            catch (e: Exception) { Log.e(TAG, "addView: ${e.message}"); return }
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
        val d = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            (240f * d).toInt(),  // Tighter width: 240dp instead of 340dp
            (40f * d).toInt(),   // Compact height: 40dp
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
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL  // Centered at bottom
            y = (60 * d).toInt()  // 60dp from bottom
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
        fun start(ctx: Context, host: String) {
            ctx.getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)
                .edit().putString(KEY_HOST, host).apply()
            ctx.startForegroundService(Intent(ctx, OSDService::class.java).putExtra(KEY_HOST, host))
        }
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OSDService::class.java))
    }
}
