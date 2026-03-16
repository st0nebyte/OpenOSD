package dev.st0nebyte.openosd

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.roundToInt

/**
 * Modern split OSD overlay:
 * - Volume: always bottom-center (compact)
 * - Info: top-left only when INFO/EXTENDED mode (compact like Denon original)
 */
class OSDView(context: Context) : View(context) {

    var state: AVRState = AVRState()
    var displayMode: OSDDisplayMode = OSDDisplayMode.STANDARD
    var scale: OSDScale = OSDScale.MEDIUM

    // Separate visibility for each OSD element
    var showVolumeOSD: Boolean = false
    var showInfoOSD: Boolean = false

    private var animVol: Float = 0f
    private var volAnim: ValueAnimator? = null

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Float {
        val scaleFactor = when (scale) {
            OSDScale.SMALL  -> 0.75f
            OSDScale.MEDIUM -> 1.0f
            OSDScale.LARGE  -> 1.3f
        }
        return v * density * scaleFactor
    }

    // ── Apple tvOS Glassmorphism - Neutral Grey ──
    private val BG           = Color.parseColor("#B31E1E23")  // 70% opaque dark grey (more transparent for glass effect)
    private val BG_GLOW      = Color.parseColor("#05FFFFFF")  // Subtle white inner glow
    private val BORDER       = Color.parseColor("#4DFFFFFF")  // 30% white border
    private val BORDER_INNER = Color.parseColor("#1FFFFFFF")  // 12% white inner highlight
    private val ACCENT       = Color.parseColor("#80C8C8CD")  // Neutral grey accent
    private val TEXT         = Color.parseColor("#FFFFFFFF")  // Full white for readability
    private val TEXT_DIM     = Color.parseColor("#BFFFFFFF")  // 75% white
    private val TEXT_MUTE    = Color.parseColor("#FFFF6464")  // Bright red
    private val BAR_BG       = Color.parseColor("#33FFFFFF")  // 20% white
    private val BAR_FILL     = Color.parseColor("#D9FFFFFF")  // 85% white (Apple style)

    // ── Cached Paints ──
    private val pBg       = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BG }
    private val pBorder   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = BORDER }
    private val pBarBg    = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BAR_BG }
    private val pBarFill  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BAR_FILL }
    private val pText     = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT }
    private val rf        = RectF()

    fun animateVolume(target: Float) {
        volAnim?.cancel()
        volAnim = null
        volAnim = ValueAnimator.ofFloat(animVol, target).apply {
            duration = 120
            interpolator = DecelerateInterpolator()
            addUpdateListener { animVol = it.animatedValue as Float; invalidate() }
        }.also { it.start() }
    }

    override fun onDraw(canvas: Canvas) {
        // Draw volume OSD (bottom-center) if triggered by volume change
        if (showVolumeOSD) {
            drawVolumeOSD(canvas)
        }

        // Draw info OSD (top-left) if triggered by source/mode/mute AND in INFO/EXTENDED mode
        if (showInfoOSD && (displayMode == OSDDisplayMode.INFO || displayMode == OSDDisplayMode.EXTENDED)) {
            drawInfoOSD(canvas)
        }
    }

    // ── Volume OSD (bottom-center, Apple tvOS glassmorphism) ─────
    private fun drawVolumeOSD(canvas: Canvas) {
        val w = dp(200f)
        val h = dp(36f)
        val x = (width - w) / 2f
        val y = height - dp(80f)
        val radius = dp(10f)

        // Glassmorphism layers
        rf.set(x, y, x + w, y + h)

        // 1. Background (70% opaque for glass effect)
        pBg.color = BG
        canvas.drawRoundRect(rf, radius, radius, pBg)

        // 2. Subtle inner glow (glass highlight)
        pBg.color = BG_GLOW
        canvas.drawRoundRect(rf, radius, radius, pBg)
        pBg.color = BG  // Reset

        // 3. Outer border
        pBorder.strokeWidth = dp(1.5f)
        pBorder.color = BORDER
        canvas.drawRoundRect(rf, radius, radius, pBorder)

        // 4. Inner highlight (glass edge)
        pBorder.strokeWidth = dp(1f)
        pBorder.color = BORDER_INNER
        rf.set(x + dp(1f), y + dp(1f), x + w - dp(1f), y + h - dp(1f))
        canvas.drawRoundRect(rf, radius - dp(1f), radius - dp(1f), pBorder)

        val pad = dp(12f)

        // VOL/MUTE label (centered vertically)
        pText.color = if (state.muted) TEXT_MUTE else TEXT_DIM
        pText.textSize = dp(9f)
        pText.textAlign = Paint.Align.LEFT
        canvas.drawText(if (state.muted) "MUTE" else "VOL", x + pad, y + h / 2f + dp(3f), pText)

        // Volume bar
        val barX = x + pad + dp(38f)
        val barW = w - pad - dp(38f) - dp(42f)
        val barH = dp(8f)
        val barY = y + (h - barH) / 2f

        rf.set(barX, barY, barX + barW, barY + barH)
        pBarBg.color = BAR_BG
        canvas.drawRoundRect(rf, dp(4f), dp(4f), pBarBg)

        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 2f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            pBarFill.color = BAR_FILL
            canvas.drawRoundRect(rf, dp(4f), dp(4f), pBarFill)
        }

        // Volume value (centered vertically)
        pText.color = TEXT
        pText.textSize = dp(13f)
        pText.textAlign = Paint.Align.RIGHT
        canvas.drawText(state.volumeString, x + w - pad, y + h / 2f + dp(4.5f), pText)
    }

    // ── Info OSD (top-left, simplified - focus on audioformat) ───
    private fun drawInfoOSD(canvas: Canvas) {
        val x = dp(24f)
        val y = dp(24f)
        val w = dp(240f)
        val h = dp(42f)
        val radius = dp(10f)

        // Glassmorphism layers
        rf.set(x, y, x + w, y + h)

        // 1. Background (70% opaque for glass effect)
        pBg.color = BG
        canvas.drawRoundRect(rf, radius, radius, pBg)

        // 2. Subtle inner glow (glass highlight)
        pBg.color = BG_GLOW
        canvas.drawRoundRect(rf, radius, radius, pBg)
        pBg.color = BG  // Reset

        // 3. Outer border
        pBorder.strokeWidth = dp(1.5f)
        pBorder.color = BORDER
        canvas.drawRoundRect(rf, radius, radius, pBorder)

        // 4. Inner highlight (glass edge)
        pBorder.strokeWidth = dp(1f)
        pBorder.color = BORDER_INNER
        rf.set(x + dp(1f), y + dp(1f), x + w - dp(1f), y + h - dp(1f))
        canvas.drawRoundRect(rf, radius - dp(1f), radius - dp(1f), pBorder)

        // Info text: Source • Sound Mode (always normal text, even when muted)
        pText.color = TEXT
        pText.textSize = dp(11f)
        pText.textAlign = Paint.Align.LEFT

        val infoText = buildString {
            state.inputSource?.let { append(it) }
            if (state.inputSource != null && state.soundMode != null) append(" • ")
            state.soundMode?.let { append(it) }
        }

        if (infoText.isNotBlank()) {
            canvas.drawText(infoText, x + dp(12f), y + h / 2f + dp(4f), pText)
        }
    }

    // Speaker display removed for simplified design (focus on audioformat)
}
