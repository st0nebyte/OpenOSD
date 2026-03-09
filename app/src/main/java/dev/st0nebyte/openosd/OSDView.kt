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

    // ── Volume OSD Extended (source + sound mode + signal + visual speaker layout) ──

    private fun drawVolumeExtended(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(8f)

        // Glassy background
        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad = dp(12f)
        val line1Y = h * 0.15f  // First line: volume bar
        val line2Y = h * 0.32f  // Second line: source + mode (full names!)
        val line3Y = h * 0.46f  // Third line: signal + format + tech info
        val layoutCenterY = h * 0.72f  // Speaker layout center

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

        // ── Line 2: Input Source • Sound Mode (FULL NAMES) ────────

        val infoText = buildString {
            state.inputSource?.let { append(it) }
            if (state.inputSource != null && state.soundMode != null) append(" • ")
            state.soundMode?.let { append(it) }  // Full format names now!
        }

        if (infoText.isNotBlank()) {
            makePaint {
                typeface = Typeface.DEFAULT
                color = TEXT_DIM
                textSize = dp(8f)
                textAlign = Paint.Align.CENTER
            }.let {
                canvas.drawText(infoText, w / 2f, line2Y, it)
            }
        }

        // ── Line 3: Signal • Format • Tech Info ───────────────────

        val techText = buildString {
            state.signalDetect?.let { append(it) }
            if (state.signalDetect != null && state.digitalMode != null) append(" • ")
            state.digitalMode?.let { append(it) }

            // Add technical features if active (only show non-defaults to reduce clutter)
            val techFeatures = mutableListOf<String>()
            state.drc?.let { if (it != "OFF" && it != "AUTO") techFeatures.add("DRC:$it") }  // Only non-default
            state.audioRestorer?.let { if (it != "OFF") techFeatures.add("R:$it") }  // Abbreviate to save space
            state.ecoMode?.let { if (it == "ON") techFeatures.add("ECO") }  // Only when active
            state.hdmiAudioOut?.let { if (it == "TV") techFeatures.add("→TV") }  // Only when non-standard

            if (techFeatures.isNotEmpty()) {
                if (state.signalDetect != null || state.digitalMode != null) append(" • ")
                append(techFeatures.joinToString(" "))
            }
        }

        if (techText.isNotBlank()) {
            makePaint {
                typeface = Typeface.DEFAULT
                color = TEXT_DIM
                textSize = dp(7f)
                textAlign = Paint.Align.CENTER
            }.let {
                canvas.drawText(techText, w / 2f, line3Y, it)
            }
        }

        // ── Visual Speaker Layout ──────────────────────────────────

        drawSpeakerLayout(canvas, w / 2f, layoutCenterY, w)
    }

    // ── Helper: Format speaker list ────────────────────────────────

    private fun formatSpeakers(speakers: List<String>): String {
        if (speakers.isEmpty()) return ""

        val count = speakers.size
        val hasSubwoofer = speakers.any { it == "SW" || it == "SW2" }
        val hasFrontHeight = speakers.any { it.startsWith("FH") }

        // Common configurations
        // NOTE: Order matters! Check specific configs before generic ones
        return when {
            // Stereo
            count == 2 && speakers.containsAll(listOf("FL", "FR")) -> "2.0"

            // Stereo with sub
            count == 3 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "SW")) -> "2.1"

            // 5.1 configurations
            count == 6 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1"

            // 5.1.2 Atmos (5.1 + 2 height) - CHECK BEFORE 7.1!
            count == 8 && hasSubwoofer && hasFrontHeight &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) -> "5.1.2"

            // 7.1 configurations (must be after 5.1.2 check)
            count == 8 && hasSubwoofer &&
                speakers.containsAll(listOf("FL", "FR", "C", "SW", "SL", "SR")) &&
                (speakers.contains("SBL") || speakers.contains("SB")) -> "7.1"

            // Custom: just list them
            count <= 4 -> speakers.joinToString(" ")

            // Many speakers: show count
            else -> "$count speakers"
        }
    }

    // ── Visual Speaker Layout (Overhead View) ──────────────────────

    private fun drawSpeakerLayout(canvas: Canvas, centerX: Float, centerY: Float, layoutWidth: Float) {
        val speakers = state.speakers
        // Always show layout - active speakers highlighted, inactive speakers dimmed

        // Speaker positions (relative to center, normalized -1.0 to 1.0)
        val positions = mapOf(
            // Front layer
            "FL"  to Pair(-0.7f, -0.5f),   // Front Left
            "FR"  to Pair(0.7f, -0.5f),    // Front Right
            "C"   to Pair(0f, -0.6f),      // Center

            // Surround layer
            "SL"  to Pair(-0.8f, 0.3f),    // Surround Left
            "SR"  to Pair(0.8f, 0.3f),     // Surround Right
            "SBL" to Pair(-0.5f, 0.8f),    // Surround Back Left
            "SBR" to Pair(0.5f, 0.8f),     // Surround Back Right
            "SB"  to Pair(0f, 0.85f),      // Surround Back (single)

            // Subwoofer(s)
            "SW"  to Pair(-0.3f, 0.6f),    // Subwoofer
            "SW2" to Pair(0.3f, 0.6f),     // Subwoofer 2

            // Front Height (Atmos)
            "FHL" to Pair(-0.7f, -0.75f),  // Front Height Left
            "FHR" to Pair(0.7f, -0.75f),   // Front Height Right

            // Top Front (Atmos)
            "TFL" to Pair(-0.5f, -0.9f),   // Top Front Left
            "TFR" to Pair(0.5f, -0.9f),    // Top Front Right

            // Top Middle (Atmos)
            "TML" to Pair(-0.5f, 0f),      // Top Middle Left
            "TMR" to Pair(0.5f, 0f),       // Top Middle Right

            // Dolby Atmos additional
            "FDL" to Pair(-0.6f, -0.8f),   // Front Dolby Left
            "FDR" to Pair(0.6f, -0.8f),    // Front Dolby Right
            "SDL" to Pair(-0.8f, 0.5f),    // Surround Dolby Left
            "SDR" to Pair(0.8f, 0.5f)      // Surround Dolby Right
        )

        val scale = layoutWidth * 0.4f  // Speaker area size
        val radius = dp(4f)  // Speaker circle radius

        // All possible speakers with their positions
        val allSpeakers = positions.keys

        allSpeakers.forEach { speakerCode ->
            val (relX, relY) = positions[speakerCode] ?: return@forEach
            val x = centerX + relX * scale
            val y = centerY + relY * scale

            val isActive = speakers.contains(speakerCode)

            // Draw speaker circle
            val circlePaint = makePaint {
                style = Paint.Style.FILL
                color = if (isActive) ACCENT else Color.parseColor("#10FFFFFF")
            }
            canvas.drawCircle(x, y, radius, circlePaint)

            // Draw speaker label
            val textPaint = makePaint {
                typeface = Typeface.MONOSPACE
                textSize = dp(7f)
                textAlign = Paint.Align.CENTER
                color = if (isActive) TEXT else TEXT_DIM
            }
            canvas.drawText(speakerCode, x, y - radius - dp(2f), textPaint)
        }

        // Draw listener position (center reference)
        val listenerPaint = makePaint {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
            color = Color.parseColor("#30FFFFFF")
        }
        canvas.drawCircle(centerX, centerY, dp(3f), listenerPaint)
    }

}
