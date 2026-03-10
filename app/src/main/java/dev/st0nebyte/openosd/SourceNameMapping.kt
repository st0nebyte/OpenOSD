package dev.st0nebyte.openosd

import android.content.Context
import android.content.SharedPreferences

/**
 * User-configurable source name mapping.
 * Maps Denon internal codes (MPLAY, BD, DVD, etc.) to custom display names.
 */
object SourceNameMapping {
    private const val PREFS_NAME = "openosd_prefs"
    private const val KEY_PREFIX = "source_name_"

    // Default mappings for common sources (used if user hasn't customized)
    private val defaultMappings = mapOf(
        "MPLAY" to "Media Player",
        "BD" to "Blu-ray",
        "DVD" to "DVD",
        "TV" to "TV",
        "SAT/CBL" to "Cable",
        "GAME" to "Game",
        "GAME2" to "Game 2",
        "AUX1" to "AUX 1",
        "AUX2" to "AUX 2",
        "CD" to "CD",
        "PHONO" to "Phono",
        "TUNER" to "Tuner",
        "NET" to "Network",
        "USB/IPOD" to "USB",
        "BT" to "Bluetooth"
    )

    /**
     * Get the display name for a source code.
     * Returns user-defined name if set, otherwise default mapping, otherwise the raw code.
     */
    fun getDisplayName(context: Context, sourceCode: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Try user-defined mapping first
        val userDefined = prefs.getString("$KEY_PREFIX$sourceCode", null)
        if (userDefined != null) return userDefined

        // Fall back to default mapping
        return defaultMappings[sourceCode] ?: sourceCode
    }

    /**
     * Set a custom display name for a source code.
     */
    fun setDisplayName(context: Context, sourceCode: String, displayName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_PREFIX$sourceCode", displayName).apply()
    }

    /**
     * Get all known source codes (for configuration UI).
     */
    fun getAllSourceCodes(): List<String> {
        return defaultMappings.keys.sorted()
    }

    /**
     * Check if a source has a custom (user-defined) name.
     */
    fun hasCustomName(context: Context, sourceCode: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains("$KEY_PREFIX$sourceCode")
    }

    /**
     * Reset a source to its default name.
     */
    fun resetToDefault(context: Context, sourceCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_PREFIX$sourceCode").apply()
    }
}
