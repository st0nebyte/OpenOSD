package dev.st0nebyte.openosd

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {

    private lateinit var etHost:        EditText
    private lateinit var spDisplayMode: Spinner
    private lateinit var spScale:       Spinner
    private lateinit var tvStatus:      TextView

    // ── Modern Light Colors (matching glassy OSD aesthetic) ──
    private val BG_MAIN    = Color.parseColor("#F5F7FA")     // Light grey background
    private val BG_CARD    = Color.parseColor("#FFFFFF")     // White cards
    private val ACCENT     = Color.parseColor("#88B4D0")     // Soft blue (lighter OSD blue)
    private val TEXT_MAIN  = Color.parseColor("#2C3E50")     // Dark grey text
    private val TEXT_DIM   = Color.parseColor("#7B8A98")     // Dimmed text
    private val BORDER     = Color.parseColor("#E1E8ED")     // Subtle border
    private val SUCCESS    = Color.parseColor("#4CAF50")     // Green for success
    private val WARNING    = Color.parseColor("#FF9800")     // Orange for warnings
    private val ERROR      = Color.parseColor("#F44336")     // Red for errors

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(BG_MAIN)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        // App Title
        root.addView(TextView(this).apply {
            text = "OpenOSD"
            textSize = 28f
            setTextColor(TEXT_MAIN)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        root.addView(TextView(this).apply {
            text = "Modern OSD for Denon AVR"
            textSize = 13f
            setTextColor(TEXT_DIM)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        root.addView(space(32))

        // IP Address Card
        root.addView(makeCard().apply {
            addView(TextView(this@MainActivity).apply {
                text = "AVR IP Address"
                textSize = 12f
                setTextColor(TEXT_DIM)
                setPadding(0, 0, 0, dp(8))
            })

            etHost = EditText(this@MainActivity).apply {
                id = View.generateViewId()
                hint = "192.168.1.x"
                textSize = 16f
                setTextColor(TEXT_MAIN)
                setHintTextColor(Color.parseColor("#B0BEC5"))
                background = makeRoundedBg(BG_CARD, BORDER, dp(8f))
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setSingleLine()
                imeOptions = EditorInfo.IME_ACTION_DONE
                isFocusable = true
                isFocusableInTouchMode = true
                setText("192.168.178.130")

                // Better focus indication for TV remote
                setOnFocusChangeListener { view, focused ->
                    (view.background as? GradientDrawable)?.setStroke(
                        dp(if (focused) 3 else 1),
                        if (focused) ACCENT else BORDER
                    )
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard(this); true
                    } else false
                }
            }
            addView(etHost, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })

        root.addView(space(16))

        // Display Mode Card
        root.addView(makeCard().apply {
            addView(TextView(this@MainActivity).apply {
                text = "Display Mode"
                textSize = 12f
                setTextColor(TEXT_DIM)
                setPadding(0, 0, 0, dp(8))
            })

            spDisplayMode = Spinner(this@MainActivity).apply {
                id = View.generateViewId()
                val modes = arrayOf("Standard", "Info", "Extended")
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, modes).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                background = makeRoundedBg(BG_CARD, BORDER, dp(8f))
                setPadding(dp(16), dp(14), dp(16), dp(14))

                // Better focus indication for TV remote
                setOnFocusChangeListener { view, focused ->
                    (view.background as? GradientDrawable)?.setStroke(
                        dp(if (focused) 3 else 1),
                        if (focused) ACCENT else BORDER
                    )
                }

                // Load saved preference
                val savedMode = prefs().getString(OSDService.KEY_DISPLAY_MODE, "STANDARD") ?: "STANDARD"
                setSelection(OSDDisplayMode.valueOf(savedMode).ordinal)

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val mode = OSDDisplayMode.values()[position]
                        prefs().edit().putString(OSDService.KEY_DISPLAY_MODE, mode.name).apply()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            addView(spDisplayMode, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })

        root.addView(space(16))

        // Scale Card
        root.addView(makeCard().apply {
            addView(TextView(this@MainActivity).apply {
                text = "Scale"
                textSize = 12f
                setTextColor(TEXT_DIM)
                setPadding(0, 0, 0, dp(8))
            })

            spScale = Spinner(this@MainActivity).apply {
                id = View.generateViewId()
                val scales = arrayOf("Small (75%)", "Medium (100%)", "Large (130%)")
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, scales).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                background = makeRoundedBg(BG_CARD, BORDER, dp(8f))
                setPadding(dp(16), dp(14), dp(16), dp(14))

                // Better focus indication for TV remote
                setOnFocusChangeListener { view, focused ->
                    (view.background as? GradientDrawable)?.setStroke(
                        dp(if (focused) 3 else 1),
                        if (focused) ACCENT else BORDER
                    )
                }

                // Load saved preference
                val savedScale = prefs().getString(OSDService.KEY_SCALE, "MEDIUM") ?: "MEDIUM"
                setSelection(OSDScale.valueOf(savedScale).ordinal)

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val scale = OSDScale.values()[position]
                        prefs().edit().putString(OSDService.KEY_SCALE, scale.name).apply()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            addView(spScale, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })

        root.addView(space(24))

        // Status indicator
        tvStatus = TextView(this).apply {
            text = "● Ready"
            textSize = 13f
            setTextColor(TEXT_DIM)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(tvStatus)

        // Action buttons
        root.addView(makeButton("Start OSD", ACCENT) { startOSD() })
        root.addView(space(12))
        root.addView(makeButton("Stop", Color.parseColor("#B0BEC5")) {
            OSDService.stop(this); setStatus(false)
        })
        root.addView(space(12))
        root.addView(makeButton("Grant Overlay Permission", Color.parseColor("#607D8B")) {
            handleOverlayPermission()
        })
        root.addView(space(12))
        root.addView(makeButton("Configure Source Names", Color.parseColor("#5E7A87")) {
            showSourceNameDialog()
        })

        root.addView(space(32))

        // Fire TV Hint Card
        root.addView(makeCard(Color.parseColor("#FFF8E1")).apply {
            addView(TextView(this@MainActivity).apply {
                text = "💡 Fire TV Setup"
                textSize = 12f
                setTextColor(Color.parseColor("#F57C00"))
                setPadding(0, 0, 0, dp(8))
            })
            addView(TextView(this@MainActivity).apply {
                text = "Overlay permission on Fire TV requires ADB:\n\nadb shell appops set dev.st0nebyte.openosd \\\n  SYSTEM_ALERT_WINDOW allow"
                textSize = 11f
                setTextColor(Color.parseColor("#E65100"))
                lineHeight = (textSize * 1.5f).toInt()
            })
        })

        setContentView(ScrollView(this).apply { addView(root) })

        // Set initial focus on IP address field for TV remote
        etHost.post { etHost.requestFocus() }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        tvStatus.text = when {
            !hasOverlay -> "⚠ Overlay permission required"
            else        -> "● Ready"
        }
        tvStatus.setTextColor(
            if (!hasOverlay) ERROR else SUCCESS)
    }

    private fun startOSD() {
        val host = etHost.text.toString().trim()
        if (host.isBlank()) {
            tvStatus.text = "⚠ Please enter IP address"
            tvStatus.setTextColor(ERROR)
            etHost.requestFocus()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            // On Fire TV: just try anyway, show ADB hint
            tvStatus.text = "⚠ Missing permission – trying anyway"
            tvStatus.setTextColor(WARNING)
        }
        OSDService.start(this, host)
        setStatus(true)
    }

    private fun setStatus(running: Boolean) {
        tvStatus.text = if (running) "● Running – Change volume to test" else "● Stopped"
        tvStatus.setTextColor(
            if (running) SUCCESS else TEXT_DIM)
    }

    private fun handleOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                // Fire TV doesn't have this settings page - show ADB command
                showAdbDialog()
            }
        } else {
            showAdbDialog()
        }
    }

    private fun showAdbDialog() {
        AlertDialog.Builder(this)
            .setTitle("Fire TV: ADB Required")
            .setMessage(
                "Fire TV has no overlay permission settings UI.\n\n" +
                "Run this command once via ADB on your PC:\n\n" +
                "adb connect FIRETV_IP:5555\n\n" +
                "adb shell appops set \\\n" +
                "  dev.st0nebyte.openosd \\\n" +
                "  SYSTEM_ALERT_WINDOW allow\n\n" +
                "Then press Start OSD."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSourceNameDialog() {
        val sourceCodes = SourceNameMapping.getAllSourceCodes()
        val sourceNames = sourceCodes.map { code ->
            "$code: ${SourceNameMapping.getDisplayName(this, code)}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Configure Source Names")
            .setItems(sourceNames) { _, which ->
                val selectedCode = sourceCodes[which]
                showEditSourceNameDialog(selectedCode)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEditSourceNameDialog(sourceCode: String) {
        val currentName = SourceNameMapping.getDisplayName(this, sourceCode)
        val input = EditText(this).apply {
            setText(currentName)
            hint = "Display name"
            setTextColor(TEXT_MAIN)
            setHintTextColor(TEXT_DIM)
            background = makeRoundedBg(BG_CARD, BORDER, dp(8f))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setSingleLine()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Source: $sourceCode")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    SourceNameMapping.setDisplayName(this, sourceCode, newName)
                    Toast.makeText(this, "Saved: $sourceCode → $newName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Reset to Default") { _, _ ->
                SourceNameMapping.resetToDefault(this, sourceCode)
                Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun makeCard(bgColor: Int = BG_CARD): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeCardBg(bgColor)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun makeButton(label: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        id = View.generateViewId()
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        background = makeButtonBg(color)
        setPadding(dp(24), dp(16), dp(24), dp(16))
        gravity = Gravity.CENTER_HORIZONTAL
        isFocusable = true
        isFocusableInTouchMode = false

        // Better focus indication for TV remote (brighter + scale)
        setOnFocusChangeListener { view, focused ->
            if (focused) {
                view.scaleX = 1.05f
                view.scaleY = 1.05f
                view.alpha = 1.0f
                (view.background as? GradientDrawable)?.let { bg ->
                    // Brighten color when focused
                    val focusColor = adjustBrightness(color, 1.2f)
                    bg.setColor(focusColor)
                }
            } else {
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                view.alpha = 0.95f
                (view.background as? GradientDrawable)?.setColor(color)
            }
        }
        setOnClickListener { onClick() }
    }

    private fun makeCardBg(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(12f)
        setStroke(dp(1), BORDER)
    }

    private fun makeButtonBg(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10f)
    }

    private fun makeRoundedBg(bgColor: Int, borderColor: Int, radius: Float) = GradientDrawable().apply {
        setColor(bgColor)
        cornerRadius = radius
        setStroke(dp(1), borderColor)
    }

    private fun hideKeyboard(v: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun space(dpVal: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(dpVal))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density)
    private fun prefs()    = getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
