package dev.st0nebyte.openosd

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.roundToInt

/**
 * Canvas-drawn OSD overlay.
 *
 * COMPACT mode (volume / mute): thin bar with speaker icon + gradient fill + dB label
 * FULL mode   (source / mode / ECO): complete Denon-style panel
 *
 * All geometry is derived from dp (density-independent) values converted via [dp].
 * This ensures pixel-perfect sharpness on 4K displays.
 */
class OSDView(context: Context) : View(context) {

    var state: AVRState = AVRState()

    // Animated volume fill
    private var animVol: Float = 0f
    private var volAnim: ValueAnimator? = null

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // ── Glassy Modern Palette ─────────────────────────────────────
    private val BG        = Color.parseColor("#CC0A0F14")  // 80% opaque, very dark blue-grey
    private val BORDER    = Color.parseColor("#18FFFFFF")  // Subtle white border (10% opacity)
    private val ACCENT    = Color.parseColor("#60A0C0E0")  // Soft blue-white accent (38% opacity)
    private val TEXT      = Color.parseColor("#E5FFFFFF")  // Off-white text (90% opacity)
    private val TEXT_DIM  = Color.parseColor("#80FFFFFF")  // Dimmed text (50% opacity)
    private val TEXT_MUTE = Color.parseColor("#D0FF6060")  // Soft red for mute (82% opacity)
    private val BAR_BG    = Color.parseColor("#18FFFFFF")  // Subtle bar background
    private val BAR_FILL  = Color.parseColor("#90B0D0F0")  // Soft blue-white fill (56% opacity)
    private val SEPARATOR = Color.parseColor("#12FFFFFF")  // Very subtle separator

    // ── Paints ───────────────────────────────────────────────────
    private fun makePaint(block: Paint.() -> Unit) = Paint(Paint.ANTI_ALIAS_FLAG).apply(block)

    private val pBg     = makePaint { style = Paint.Style.FILL; color = BG }
    private val pBorder = makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = BORDER }

    private val rf = RectF()

    // ── Animation ─────────────────────────────────────────────────

    fun animateVolume(target: Float) {
        volAnim?.cancel()
        volAnim = ValueAnimator.ofFloat(animVol, target).apply {
            duration = 120  // Faster animation for smoother feel with 150ms polling
            interpolator = DecelerateInterpolator()
            addUpdateListener { animVol = it.animatedValue as Float; invalidate() }
        }.also { it.start() }
    }

    // ── Draw ──────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        drawVolume(canvas)
    }

    // ── Volume OSD (tight & compact) ──────────────────────────────

    private fun drawVolume(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(8f)

        // Glassy background
        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad = dp(12f)
        val midY = h / 2f

        // VOL / MUTE label (compact)
        makePaint {
            typeface = Typeface.DEFAULT
            color = if (state.muted) TEXT_MUTE else TEXT_DIM
            textSize = dp(10f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(if (state.muted) "MUTE" else "VOL", pad, midY + dp(3.5f), it)
        }

        // Volume bar (tighter layout)
        val barX = pad + dp(32f)
        val barW = w - barX - dp(48f)
        val barH = dp(10f)
        val barY = midY - barH / 2f

        // Bar background
        rf.set(barX, barY, barX + barW, barY + barH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_BG })

        // Bar fill
        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 2f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_FILL })
        }

        // Volume value (compact)
        makePaint {
            typeface = Typeface.DEFAULT
            color = TEXT
            textSize = dp(15f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(state.volumeString, barX + barW + dp(6f), midY + dp(5f), it)
        }
    }

}
