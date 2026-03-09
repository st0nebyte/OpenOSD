package dev.st0nebyte.openosd

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.parseColor("#080F18"))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(40), dp(32), dp(32))
        }

        root.addView(TextView(this).apply {
            text = "DENON OSD"; textSize = 22f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#00D8D8"))
            gravity = Gravity.CENTER_HORIZONTAL
        })
        root.addView(space(24))

        root.addView(TextView(this).apply {
            text = "AVR IP-Adresse"; textSize = 12f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#557788"))
            setPadding(0, 0, 0, dp(6))
        })

        etHost = EditText(this).apply {
            hint = "192.168.1.x"
            typeface = Typeface.MONOSPACE
            textSize = 18f
            setTextColor(Color.parseColor("#00D8D8"))
            setHintTextColor(Color.parseColor("#224444"))
            setBackgroundColor(Color.parseColor("#0C1820"))
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setSingleLine()
            imeOptions = EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            setText("192.168.178.130")

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(this); true
                } else false
            }
        }
        root.addView(etHost, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(space(16))

        root.addView(TextView(this).apply {
            text = "OSD Anzeigemodus"; textSize = 12f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#557788"))
            setPadding(0, 0, 0, dp(6))
        })

        spDisplayMode = Spinner(this).apply {
            val modes = arrayOf("Standard (nur Lautstärke)", "Info (mit Sound Mode + Input)", "Extended (mit Signal + Lautsprecher)")
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, modes).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setBackgroundColor(Color.parseColor("#0C1820"))
            setPadding(dp(12), dp(14), dp(12), dp(14))

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
        root.addView(spDisplayMode, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(space(16))

        root.addView(TextView(this).apply {
            text = "OSD Skalierung"; textSize = 12f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#557788"))
            setPadding(0, 0, 0, dp(6))
        })

        spScale = Spinner(this).apply {
            val scales = arrayOf("Klein (75%)", "Mittel (100%)", "Groß (130%)")
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, scales).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setBackgroundColor(Color.parseColor("#0C1820"))
            setPadding(dp(12), dp(14), dp(12), dp(14))

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
        root.addView(spScale, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(space(16))

        tvStatus = TextView(this).apply {
            text = "● Inaktiv"; textSize = 12f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#556666"))
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(tvStatus)

        root.addView(makeBtn("▶  OSD STARTEN")           { startOSD() })
        root.addView(space(8))
        root.addView(makeBtn("■  STOPPEN")                { OSDService.stop(this); setStatus(false) })
        root.addView(space(8))
        root.addView(makeBtn("⚙  OVERLAY-BERECHTIGUNG")  { handleOverlayPermission() })
        root.addView(space(24))

        root.addView(TextView(this).apply {
            text = "Tipp: Overlay-Berechtigung auf Fire TV\nnur per ADB möglich:\nadb shell appops set dev.st0nebyte.openosd\n  SYSTEM_ALERT_WINDOW allow"
            textSize = 11f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#3A5560"))
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        tvStatus.text = when {
            !hasOverlay -> "⚠ Overlay-Berechtigung fehlt!"
            else        -> "● Bereit"
        }
        tvStatus.setTextColor(
            if (!hasOverlay) Color.parseColor("#FF6644")
            else Color.parseColor("#40CC40"))
    }

    private fun startOSD() {
        val host = etHost.text.toString().trim()
        if (host.isBlank()) {
            tvStatus.text = "⚠ IP eingeben!"
            tvStatus.setTextColor(Color.parseColor("#FF6644"))
            etHost.requestFocus()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            // On Fire TV: just try anyway, show ADB hint
            tvStatus.text = "⚠ Overlay fehlt – starte trotzdem"
            tvStatus.setTextColor(Color.parseColor("#FFAA00"))
        }
        OSDService.start(this, host)
        setStatus(true)
    }

    private fun setStatus(running: Boolean) {
        tvStatus.text = if (running) "● Läuft – AVR Lautstärke ändern zum Testen" else "● Gestoppt"
        tvStatus.setTextColor(
            if (running) Color.parseColor("#40CC40") else Color.parseColor("#556666"))
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
            .setTitle("Fire TV: ADB erforderlich")
            .setMessage(
                "Fire TV hat kein Overlay-Einstellungsmenü.\n\n" +
                "Einmalig per ADB auf dem PC ausführen:\n\n" +
                "adb connect FIRETV_IP:5555\n\n" +
                "adb shell appops set \\\n" +
                "  dev.st0nebyte.openosd \\\n" +
                "  SYSTEM_ALERT_WINDOW allow\n\n" +
                "Danach OSD Starten drücken."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun makeBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 14f; typeface = Typeface.MONOSPACE
        setTextColor(Color.parseColor("#00D8D8"))
        setBackgroundColor(Color.parseColor("#0C1C2C"))
        setPadding(dp(20), dp(16), dp(20), dp(16))
        gravity = Gravity.CENTER_HORIZONTAL
        isFocusable = true
        isFocusableInTouchMode = false  // don't steal focus on touch

        setOnFocusChangeListener { _, focused ->
            setBackgroundColor(
                if (focused) Color.parseColor("#0D3050")
                else         Color.parseColor("#0C1C2C"))
        }
        setOnClickListener { onClick() }
    }

    private fun hideKeyboard(v: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun space(dpVal: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(dpVal))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs()    = getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE)
}
