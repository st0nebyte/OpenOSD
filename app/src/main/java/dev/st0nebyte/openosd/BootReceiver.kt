package dev.st0nebyte.openosd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val host = ctx.getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)
            .getString(OSDService.KEY_HOST, "") ?: return
        if (host.isNotBlank()) OSDService.start(ctx, host)
    }
}
