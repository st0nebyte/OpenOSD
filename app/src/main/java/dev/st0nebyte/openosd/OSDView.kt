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

    var state: AVRState    = AVRState()
    var showFull: Boolean  = false

    // Animated volume fill
    private var animVol: Float = 0f
    private var volAnim: ValueAnimator? = null

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // ── Palette ──────────────────────────────────────────────────
    private val CYAN      = Color.parseColor("#00D8D8")
    private val CYAN_MID  = Color.parseColor("#009898")
    private val CYAN_DIM  = Color.parseColor("#004444")
    private val BG        = Color.parseColor("#E8080F18")  // 91% opaque dark
    private val WHITE     = Color.WHITE
    private val DIM       = Color.parseColor("#7799AA")
    private val GREEN     = Color.parseColor("#40CC40")
    private val RED       = Color.parseColor("#FF4444")
    private val CH_ON_BG  = Color.parseColor("#00D8D8")
    private val CH_ON_FG  = Color.parseColor("#001818")
    private val CH_OFF_BG = Color.parseColor("#0A1C1C")
    private val CH_OFF_FG = Color.parseColor("#1A3A3A")
    private val BORDER    = Color.parseColor("#55007878")

    // ── Paints ───────────────────────────────────────────────────
    private fun makePaint(block: Paint.() -> Unit) = Paint(Paint.ANTI_ALIAS_FLAG).apply(block)

    private val pBg      = makePaint { style = Paint.Style.FILL; color = BG }
    private val pBorder  = makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1.5f); color = BORDER }
    private val pLabelBg = makePaint { style = Paint.Style.FILL; color = Color.parseColor("#30003C3C") }
    private val pLabelBd = makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = CYAN }
    private val pLabel   = makePaint { typeface = Typeface.MONOSPACE; isFakeBoldText = true; color = CYAN; textAlign = Paint.Align.CENTER }
    private val pValue   = makePaint { typeface = Typeface.MONOSPACE; color = WHITE }
    private val pSub     = makePaint { typeface = Typeface.MONOSPACE; color = DIM }
    private val pVolDb   = makePaint { typeface = Typeface.MONOSPACE; color = WHITE; textAlign = Paint.Align.RIGHT }
    private val pMute    = makePaint { typeface = Typeface.MONOSPACE; isFakeBoldText = true; color = RED }
    private val pGreen   = makePaint { typeface = Typeface.MONOSPACE; color = GREEN }
    private val pChOnTxt = makePaint { typeface = Typeface.MONOSPACE; isFakeBoldText = true; color = CH_ON_FG; textAlign = Paint.Align.CENTER }
    private val pChOffTxt= makePaint { typeface = Typeface.MONOSPACE; color = CH_OFF_FG; textAlign = Paint.Align.CENTER }
    private val pChOnBg  = makePaint { style = Paint.Style.FILL; color = CH_ON_BG }
    private val pChOffBg = makePaint { style = Paint.Style.FILL; color = CH_OFF_BG }
    private val pChBd    = makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f) }

    private val rf = RectF()

    // ── Animation ─────────────────────────────────────────────────

    fun animateVolume(target: Float) {
        volAnim?.cancel()
        volAnim = ValueAnimator.ofFloat(animVol, target).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { animVol = it.animatedValue as Float; invalidate() }
        }.also { it.start() }
    }

    // ── Draw ──────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (showFull) drawFull(canvas)
        else          drawCompact(canvas)
    }

    // ── COMPACT ───────────────────────────────────────────────────

    private fun drawCompact(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(6f)

        // Background pill
        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad   = dp(14f)
        val iconW = dp(28f)
        val midY  = h / 2f

        // Speaker icon / MUTE label
        if (state.muted) {
            pMute.textSize = dp(13f)
            canvas.drawText("MUTED", pad, midY + dp(5f), pMute)
        } else {
            drawSpeaker(canvas, pad, midY, dp(16f), active = true)
        }

        // Volume bar
        val barX = pad + iconW + dp(8f)
        val barW = w - barX - dp(80f)
        val barH = dp(10f)
        val barY = midY - barH / 2f

        rf.set(barX, barY, barX + barW, barY + barH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = Color.parseColor("#18007070") })
        canvas.drawRect(rf, makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = CYAN_DIM })

        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 1f) {
            rf.set(barX, barY, barX + fillW, barY + barH)
            canvas.drawRect(rf, makePaint {
                style = Paint.Style.FILL
                shader = LinearGradient(barX, 0f, barX + fillW, 0f, CYAN_DIM, CYAN, Shader.TileMode.CLAMP)
            })
        }
        // Arrow marker
        makePaint { typeface = Typeface.MONOSPACE; color = CYAN; textSize = dp(11f) }.let {
            canvas.drawText("▶", barX + fillW + dp(1f), midY + dp(4f), it)
        }

        // dB
        pVolDb.textSize = dp(18f)
        canvas.drawText(state.volumeString, w - dp(6f), midY + dp(7f), pVolDb)
    }

    // ── FULL panel ────────────────────────────────────────────────

    private fun drawFull(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = dp(8f)

        rf.set(0f, 0f, w, h)
        canvas.drawRoundRect(rf, r, r, pBg)
        canvas.drawRoundRect(rf, r, r, pBorder)

        val pad  = dp(16f)
        val lw   = dp(84f)   // label box width
        val rowH = dp(28f)
        var y    = pad + dp(20f)

        // ── Info rows ─────────────────────────────────────────────
        fun infoRow(lbl: String, value: String, vp: Paint = pValue, vs: Float = dp(14.5f)) {
            drawLabelBox(canvas, pad, y - dp(18f), lw, dp(22f), lbl)
            vp.textSize = vs
            canvas.drawText(value, pad + lw + dp(10f), y + dp(1f), vp)
            y += rowH + dp(2f)
        }

        infoRow("SOURCE", state.sourceDisplay)
        infoRow("SOUND",  state.soundModeDisplay)
        infoRow("SIGNAL", state.signalInfo, pSub, dp(13f))

        // Audyssey row (multi-line)
        drawLabelBox(canvas, pad, y - dp(18f), lw, dp(22f), "AUDYSSEY")
        val ax = pad + lw + dp(10f)
        pValue.textSize = dp(13.5f)
        canvas.drawText("MultEQ XT : ${state.multEQDisplay}", ax, y, pValue)
        y += dp(18f)
        pSub.textSize = dp(12f)
        canvas.drawText("Dynamic EQ : ${if (state.dynEQ.equals("ON",true)) "Ein" else if (state.dynEQ.isBlank()) "—" else "Aus"}", ax, y, pSub)
        y += dp(16f)
        canvas.drawText("Dynamic Volume : ${when(state.dynVol.uppercase()) { "HEV"->"Heavy"; "MED"->"Medium"; "LIT"->"Light"; "OFF"->"Off"; else -> state.dynVol.ifBlank{"—"} }}", ax, y, pSub)
        y += rowH + dp(6f)

        // ── Speaker boxes ─────────────────────────────────────────
        val boxW = (w - pad * 2f - dp(10f)) / 2f
        val boxH = dp(95f)
        drawSpeakerBox(canvas, pad,                   y, boxW, boxH, "INPUT SIGNAL",
            listOf(
                listOf("FHL","LFE","EXT","FHR"),
                listOf("FWL","FL","C","FR","FWR"),
                listOf("SL","SR"),
                listOf("SBL","SB","SBR"),
            ), state.inputChannels)
        drawSpeakerBox(canvas, pad + boxW + dp(10f),  y, boxW, boxH, "ACTIVE SPEAKERS",
            listOf(
                listOf("FDL","SW","FDR"),
                listOf("FL","C","FR"),
                listOf("SL","SR"),
            ), state.activeChannels)
        y += boxH + dp(12f)

        // ── Volume row ────────────────────────────────────────────
        val barH = dp(10f)
        val iconW = dp(26f)
        val barX  = pad + iconW + dp(6f)
        val barW  = w - barX - dp(76f)
        val midY  = y + barH / 2f

        if (state.muted) {
            pMute.textSize = dp(11f)
            canvas.drawText("MUTED", pad, midY + dp(4f), pMute)
        } else {
            drawSpeaker(canvas, pad, midY, dp(14f), active = true)
        }

        rf.set(barX, y, barX + barW, y + barH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = Color.parseColor("#18007070") })
        canvas.drawRect(rf, makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = CYAN_DIM })

        val fillW = (barW * animVol).coerceAtLeast(0f)
        if (fillW > 1f) {
            rf.set(barX, y, barX + fillW, y + barH)
            canvas.drawRect(rf, makePaint {
                style = Paint.Style.FILL
                shader = LinearGradient(barX, 0f, barX + fillW, 0f, CYAN_DIM, CYAN, Shader.TileMode.CLAMP)
            })
        }
        makePaint { typeface = Typeface.MONOSPACE; color = CYAN; textSize = dp(10f) }.let {
            canvas.drawText("▶", barX + fillW + dp(1f), y + barH - dp(1f), it)
        }
        pVolDb.textSize = dp(18f)
        canvas.drawText(state.volumeString, w - pad, midY + dp(7f), pVolDb)

        y += dp(28f)

        // ── ECO row ───────────────────────────────────────────────
        val ecoBarW = dp(80f)
        val ecoBarH = dp(7f)
        val ecoX    = w - pad - ecoBarW - dp(48f)
        pGreen.textSize = dp(12f)
        canvas.drawText("🌿 ECO : ${state.ecoDisplay}", ecoX - dp(72f), y + dp(6f), pGreen)
        rf.set(ecoX, y, ecoX + ecoBarW, y + ecoBarH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = Color.parseColor("#1A301A") })
        canvas.drawRect(rf, makePaint { style = Paint.Style.STROKE; strokeWidth = dp(1f); color = Color.parseColor("#336633") })
        val ecoPct = when (state.ecoDisplay) { "ON" -> 0.30f; "OFF" -> 0.95f; else -> 0.60f }
        rf.set(ecoX, y, ecoX + ecoBarW * ecoPct, y + ecoBarH)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = GREEN })
        pSub.textSize = dp(11f)
        canvas.drawText("Energie", ecoX + ecoBarW + dp(5f), y + dp(6f), pSub)
    }

    // ── Speaker box ───────────────────────────────────────────────

    private fun drawSpeakerBox(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        title: String, rows: List<List<String>>, active: Set<String>,
    ) {
        rf.set(x, y, x + w, y + h)
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = Color.parseColor("#0C001818") })
        canvas.drawRect(rf, pBorder)

        pLabel.textSize = dp(10f)
        val tw = pLabel.measureText(title) + dp(10f)
        rf.set(x + dp(8f), y - dp(7f), x + dp(8f) + tw, y + dp(7f))
        canvas.drawRect(rf, makePaint { style = Paint.Style.FILL; color = BG })
        canvas.drawText(title, x + dp(8f) + tw / 2f, y + dp(4f), pLabel)

        val chipW = dp(28f); val chipH = dp(16f); val gap = dp(3f)
        var cy = y + dp(16f)

        for (row in rows) {
            val rowWidth = row.size * chipW + (row.size - 1) * gap
            var cx = x + (w - rowWidth) / 2f
            for (ch in row) {
                val on = active.contains(ch)
                rf.set(cx, cy, cx + chipW, cy + chipH)
                canvas.drawRect(rf, if (on) pChOnBg else pChOffBg)
                pChBd.color = if (on) CYAN else CH_OFF_FG
                canvas.drawRect(rf, pChBd)
                val tp = if (on) pChOnTxt else pChOffTxt
                tp.textSize = dp(9.5f)
                canvas.drawText(ch, cx + chipW / 2f, cy + chipH * 0.70f, tp)
                cx += chipW + gap
            }
            cy += chipH + gap
        }
    }

    // ── Label box ─────────────────────────────────────────────────

    private fun drawLabelBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, text: String) {
        rf.set(x, y, x + w, y + h)
        canvas.drawRect(rf, pLabelBg)
        canvas.drawRect(rf, pLabelBd)
        pLabel.textSize = dp(11f)
        canvas.drawText(text, x + w / 2f, y + h * 0.72f, pLabel)
    }

    // ── Speaker glyph ─────────────────────────────────────────────

    private fun drawSpeaker(canvas: Canvas, x: Float, cy: Float, size: Float, active: Boolean) {
        val col = if (active) CYAN else RED
        val p   = makePaint { color = col; style = Paint.Style.FILL }
        // Body
        rf.set(x, cy - size * 0.4f, x + size * 0.35f, cy + size * 0.4f)
        canvas.drawRect(rf, p)
        // Cone
        val path = Path().apply {
            moveTo(x + size * 0.35f, cy - size * 0.4f)
            lineTo(x + size * 0.35f, cy + size * 0.4f)
            lineTo(x + size * 0.65f, cy + size * 0.65f)
            lineTo(x + size * 0.65f, cy - size * 0.65f)
            close()
        }
        canvas.drawPath(path, p)
        if (active) {
            p.style = Paint.Style.STROKE; p.strokeWidth = size * 0.09f
            for (i in 1..2) {
                val r = size * (0.5f + i * 0.22f)
                canvas.drawArc(RectF(x + size*0.55f - r, cy - r, x + size*0.55f + r, cy + r), -50f, 100f, false, p)
            }
        }
    }
}
