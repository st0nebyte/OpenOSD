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
    var displayMode: OSDDisplayMode = OSDDisplayMode.STANDARD
    var scale: OSDScale = OSDScale.MEDIUM

    // Animated volume fill
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
        when (displayMode) {
            OSDDisplayMode.STANDARD -> drawVolumeCompact(canvas)
            OSDDisplayMode.INFO -> drawVolumeWithInfo(canvas)
            OSDDisplayMode.EXTENDED -> drawVolumeExtended(canvas)
        }
    }

    // ── Volume OSD (tight & compact) ──────────────────────────────

    private fun drawVolumeCompact(canvas: Canvas) {
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

    // ── Volume OSD with Info (source + sound mode) ────────────────

    private fun drawVolumeWithInfo(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(8f)

        // Glassy background
        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad = dp(12f)
        val line1Y = h * 0.35f  // First line: volume bar
        val line2Y = h * 0.72f  // Second line: info

        // ── Line 1: VOL / MUTE + Bar + Value ──────────────────────

        // VOL / MUTE label
        makePaint {
            typeface = Typeface.DEFAULT
            color = if (state.muted) TEXT_MUTE else TEXT_DIM
            textSize = dp(10f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(if (state.muted) "MUTE" else "VOL", pad, line1Y + dp(3.5f), it)
        }

        // Volume bar
        val barX = pad + dp(32f)
        val barW = w - barX - dp(48f)
        val barH = dp(10f)
        val barY = line1Y - barH / 2f

        // Bar background
        rf.set(barX, barY, barX + barW, barY + barH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_BG })

        // Bar fill
        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 2f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_FILL })
        }

        // Volume value
        makePaint {
            typeface = Typeface.DEFAULT
            color = TEXT
            textSize = dp(15f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(state.volumeString, barX + barW + dp(6f), line1Y + dp(5f), it)
        }

        // ── Line 2: Input Source • Sound Mode ─────────────────────

        val infoText = buildString {
            state.inputSource?.let { append(it) }
            if (state.inputSource != null && state.soundMode != null) append(" • ")
            state.soundMode?.let { append(it) }
        }

        if (infoText.isNotBlank()) {
            makePaint {
                typeface = Typeface.DEFAULT
                color = TEXT_DIM
                textSize = dp(9f)
                textAlign = Paint.Align.CENTER
            }.let {
                canvas.drawText(infoText, w / 2f, line2Y, it)
            }
        }
    }

    // ── Volume OSD Extended (source + sound mode + signal + speakers) ──

    private fun drawVolumeExtended(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(8f)

        // Glassy background
        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad = dp(12f)
        val line1Y = h * 0.28f  // First line: volume bar
        val line2Y = h * 0.55f  // Second line: source + mode
        val line3Y = h * 0.78f  // Third line: signal + format + speakers

        // ── Line 1: VOL / MUTE + Bar + Value ──────────────────────

        // VOL / MUTE label
        makePaint {
            typeface = Typeface.DEFAULT
            color = if (state.muted) TEXT_MUTE else TEXT_DIM
            textSize = dp(10f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(if (state.muted) "MUTE" else "VOL", pad, line1Y + dp(3.5f), it)
        }

        // Volume bar
        val barX = pad + dp(32f)
        val barW = w - barX - dp(48f)
        val barH = dp(10f)
        val barY = line1Y - barH / 2f

        // Bar background
        rf.set(barX, barY, barX + barW, barY + barH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_BG })

        // Bar fill
        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 2f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BAR_FILL })
        }

        // Volume value
        makePaint {
            typeface = Typeface.DEFAULT
            color = TEXT
            textSize = dp(15f)
            textAlign = Paint.Align.LEFT
        }.let {
            canvas.drawText(state.volumeString, barX + barW + dp(6f), line1Y + dp(5f), it)
        }

        // ── Line 2: Input Source • Sound Mode ─────────────────────

        val infoText = buildString {
            state.inputSource?.let { append(it) }
            if (state.inputSource != null && state.soundMode != null) append(" • ")
            state.soundMode?.let { append(it) }
        }

        if (infoText.isNotBlank()) {
            makePaint {
                typeface = Typeface.DEFAULT
                color = TEXT_DIM
                textSize = dp(9f)
                textAlign = Paint.Align.CENTER
            }.let {
                canvas.drawText(infoText, w / 2f, line2Y, it)
            }
        }

        // ── Line 3: Signal • Format • Speakers ────────────────────

        val extendedText = buildString {
            state.signalDetect?.let { append(it) }
            if (state.signalDetect != null && state.digitalMode != null) append(" • ")
            state.digitalMode?.let { append(it) }
            val speakerStr = formatSpeakers(state.speakers)
            if (speakerStr.isNotBlank() && (state.signalDetect != null || state.digitalMode != null)) {
                append(" • ")
            }
            if (speakerStr.isNotBlank()) append(speakerStr)
        }

        if (extendedText.isNotBlank()) {
            makePaint {
                typeface = Typeface.DEFAULT
                color = TEXT_DIM
                textSize = dp(8f)
                textAlign = Paint.Align.CENTER
            }.let {
                canvas.drawText(extendedText, w / 2f, line3Y, it)
            }
        }
    }

    // ── Helper: Format speaker list ────────────────────────────────

    private fun formatSpeakers(speakers: List<String>): String {
        if (speakers.isEmpty()) return ""

        val count = speakers.size
        val hasSubwoofer = speakers.any { it == "SW" || it == "SW2" }
        val hasFrontHeight = speakers.any { it.startsWith("FH") }

        // Common configurations
        return when {
            // 5.1 configurations
            count == 6 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1"

            // 7.1 configurations
            count == 8 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "7.1"

            // 5.1.2 Atmos (5.1 + 2 height)
            count == 8 && hasSubwoofer && hasFrontHeight &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1.2"

            // Stereo
            count == 2 && speakers.containsAll(listOf("FL", "FR")) -> "2.0"

            // Stereo with sub
            count == 3 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "SW")) -> "2.1"

            // Custom: just list them
            count <= 4 -> speakers.joinToString(" ")

            // Many speakers: show count
            else -> "$count speakers"
        }
    }

}
