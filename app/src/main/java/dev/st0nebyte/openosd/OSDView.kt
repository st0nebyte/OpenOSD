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

    // ── Modern Light Colors ──
    private val BG        = Color.parseColor("#CC0A0F14")
    private val BORDER    = Color.parseColor("#18FFFFFF")
    private val ACCENT    = Color.parseColor("#60A0C0E0")
    private val TEXT      = Color.parseColor("#E5FFFFFF")
    private val TEXT_DIM  = Color.parseColor("#80FFFFFF")
    private val TEXT_MUTE = Color.parseColor("#D0FF6060")
    private val BAR_BG    = Color.parseColor("#18FFFFFF")
    private val BAR_FILL  = Color.parseColor("#90B0D0F0")

    // ── Cached Paints ──
    private val pBg       = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BG }
    private val pBorder   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = BORDER }
    private val pBarBg    = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BAR_BG }
    private val pBarFill  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = BAR_FILL }
    private val pText     = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT }
    private val pSpeaker  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rf        = RectF()

    fun animateVolume(target: Float) {
        volAnim?.cancel()
        volAnim = ValueAnimator.ofFloat(animVol, target).apply {
            duration = 120
            interpolator = DecelerateInterpolator()
            addUpdateListener { animVol = it.animatedValue as Float; invalidate() }
        }.also { it.start() }
    }

    override fun onDraw(canvas: Canvas) {
        // Always draw volume OSD at bottom
        drawVolumeOSD(canvas)

        // Draw info OSD at top-left only if INFO or EXTENDED mode
        if (displayMode == OSDDisplayMode.INFO || displayMode == OSDDisplayMode.EXTENDED) {
            drawInfoOSD(canvas)
        }
    }

    // ── Volume OSD (bottom-center, compact) ──────────────────────
    private fun drawVolumeOSD(canvas: Canvas) {
        val w = dp(200f)  // Compact width
        val h = dp(36f)   // Compact height
        val x = (width - w) / 2f
        val y = height - dp(80f)  // 80dp from bottom

        // Background
        rf.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rf, dp(8f), dp(8f), pBg)
        canvas.drawRoundRect(rf, dp(8f), dp(8f), pBorder)

        val pad = dp(12f)

        // VOL/MUTE label
        pText.color = if (state.muted) TEXT_MUTE else TEXT_DIM
        pText.textSize = dp(9f)
        pText.textAlign = Paint.Align.LEFT
        canvas.drawText(if (state.muted) "MUTE" else "VOL", x + pad, y + dp(13f), pText)

        // Volume bar
        val barX = x + pad + dp(38f)
        val barW = w - pad - dp(38f) - dp(42f)
        val barH = dp(8f)
        val barY = y + (h - barH) / 2f

        rf.set(barX, barY, barX + barW, barY + barH)
        canvas.drawRect(rf, pBarBg)

        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 2f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            canvas.drawRect(rf, pBarFill)
        }

        // Volume value
        pText.color = TEXT
        pText.textSize = dp(13f)
        pText.textAlign = Paint.Align.RIGHT
        canvas.drawText(state.volumeString, x + w - pad, y + dp(23f), pText)
    }

    // ── Info OSD (top-left, compact like Denon original) ─────────
    private fun drawInfoOSD(canvas: Canvas) {
        val x = dp(24f)
        val y = dp(24f)
        val w = dp(280f)
        var currentY = y

        // Background for entire info block
        val blockHeight = dp(if (displayMode == OSDDisplayMode.EXTENDED) 140f else 70f)
        rf.set(x, y, x + w, y + blockHeight)
        canvas.drawRoundRect(rf, dp(8f), dp(8f), pBg)
        canvas.drawRoundRect(rf, dp(8f), dp(8f), pBorder)

        val pad = dp(12f)
        currentY += dp(16f)

        // Source + Sound Mode
        pText.color = TEXT
        pText.textSize = dp(11f)
        pText.textAlign = Paint.Align.LEFT

        val line1 = buildString {
            state.inputSource?.let { append(it) }
            if (state.inputSource != null && state.soundMode != null) append(" • ")
            state.soundMode?.let { append(it) }
        }
        if (line1.isNotBlank()) {
            canvas.drawText(line1, x + pad, currentY, pText)
            currentY += dp(18f)
        }

        // Signal + Digital + Tech Info
        pText.textSize = dp(9f)
        pText.color = TEXT_DIM

        val line2 = buildString {
            state.signalDetect?.let { append(it) }
            if (state.signalDetect != null && state.digitalMode != null) append(" • ")
            state.digitalMode?.let { append(it) }

            val tech = mutableListOf<String>()
            state.drc?.let { if (it != "OFF" && it != "AUTO") tech.add("DRC:$it") }
            state.audioRestorer?.let { if (it != "OFF") tech.add("R:$it") }
            state.ecoMode?.let { if (it == "ON") tech.add("ECO") }
            state.hdmiAudioOut?.let { if (it == "TV") tech.add("→TV") }

            if (tech.isNotEmpty()) {
                if (state.signalDetect != null || state.digitalMode != null) append(" • ")
                append(tech.joinToString(" "))
            }
        }
        if (line2.isNotBlank()) {
            canvas.drawText(line2, x + pad, currentY, pText)
            currentY += dp(20f)
        }

        // Compact speaker display (EXTENDED mode only, like Denon original)
        if (displayMode == OSDDisplayMode.EXTENDED && state.speakers.isNotEmpty()) {
            drawCompactSpeakers(canvas, x + pad, currentY, w - pad * 2)
        }
    }

    // ── Compact Speaker Display (Box-style like Denon) ───────────
    private fun drawCompactSpeakers(canvas: Canvas, x: Float, y: Float, maxWidth: Float) {
        val speakers = state.speakers
        if (speakers.isEmpty()) return

        var currentY = y

        // Group speakers
        val frontSpeakers = speakers.filter { it in listOf("FL", "FR", "C") }
        val heightSpeakers = speakers.filter { it.startsWith("FH") || it.startsWith("T") }
        val surroundSpeakers = speakers.filter { it in listOf("SL", "SR", "SBL", "SBR", "SB") }
        val subs = speakers.filter { it.startsWith("SW") }

        // Front Speakers
        if (frontSpeakers.isNotEmpty()) {
            pText.color = TEXT_DIM
            pText.textSize = dp(7f)
            pText.textAlign = Paint.Align.LEFT
            canvas.drawText("FRONT SPEAKERS", x, currentY, pText)
            currentY += dp(12f)
            currentY = drawSpeakerRow(canvas, x, currentY, frontSpeakers)
            currentY += dp(8f)
        }

        // Height Speakers
        if (heightSpeakers.isNotEmpty()) {
            pText.color = TEXT_DIM
            pText.textSize = dp(7f)
            pText.textAlign = Paint.Align.LEFT
            canvas.drawText("HEIGHT SPEAKERS", x, currentY, pText)
            currentY += dp(12f)
            currentY = drawSpeakerRow(canvas, x, currentY, heightSpeakers)
            currentY += dp(8f)
        }

        // Surround + Subs (compact, one line)
        val others = surroundSpeakers + subs
        if (others.isNotEmpty()) {
            currentY = drawSpeakerRow(canvas, x, currentY, others)
        }
    }

    private fun drawSpeakerRow(canvas: Canvas, startX: Float, y: Float, speakers: List<String>): Float {
        var x = startX
        val boxW = dp(24f)
        val boxH = dp(16f)
        val spacing = dp(6f)

        speakers.forEach { code ->
            // Draw speaker box
            pSpeaker.color = ACCENT
            rf.set(x, y, x + boxW, y + boxH)
            canvas.drawRoundRect(rf, dp(3f), dp(3f), pSpeaker)

            // Draw label
            pText.color = TEXT
            pText.textSize = dp(8f)
            pText.textAlign = Paint.Align.CENTER
            canvas.drawText(code, x + boxW / 2, y + dp(11f), pText)

            x += boxW + spacing
        }

        return y + boxH + dp(4f)
    }
}
