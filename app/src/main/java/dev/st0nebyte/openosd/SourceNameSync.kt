package dev.st0nebyte.openosd

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "SourceNameSync"

/**
 * Auto-sync source names from Denon Web UI.
 *
 * Fetches custom source names from the receiver's web interface at:
 * http://IP/SETUP/INPUTS/INPUTASSIGN/d_InputAssign.asp
 *
 * Only updates names that haven't been manually customized by the user.
 */
object SourceNameSync {

    /**
     * Sync source names from receiver.
     * Safe to call on main thread - runs async internally.
     *
     * @param context Application context
     * @param host Receiver IP address
     * @param onComplete Callback with (success, count) when done
     */
    suspend fun sync(
        context: Context,
        host: String,
        onComplete: ((Boolean, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$host/SETUP/INPUTS/INPUTASSIGN/d_InputAssign.asp")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.w(TAG, "HTTP $responseCode from $url")
                onComplete?.invoke(false, 0)
                return@withContext
            }

            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val mappings = parseSourceNames(html)

            Log.d(TAG, "Parsed ${mappings.size} source names from receiver")

            // Only update names that user hasn't customized
            var updated = 0
            mappings.forEach { (code, name) ->
                if (!SourceNameMapping.hasCustomName(context, code)) {
                    SourceNameMapping.setDisplayName(context, code, name)
                    updated++
                    Log.d(TAG, "Auto-set: $code → $name")
                } else {
                    Log.d(TAG, "Skip (user customized): $code")
                }
            }

            Log.i(TAG, "Synced $updated/${mappings.size} source names")
            onComplete?.invoke(true, updated)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            onComplete?.invoke(false, 0)
        }
    }

    /**
     * Parse source names from Denon Web UI HTML.
     *
     * Looks for pattern:
     * <TD><B>Fire TV</B></td>
     * <TD><select name='listHdmiAssignMPLAY' ...>
     */
    private fun parseSourceNames(html: String): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        val lines = html.split("\r\n", "\n")

        var currentName: String? = null
        for (line in lines) {
            // Find source name in bold: <TD><B>Fire TV</B></td>
            val boldMatch = Regex("<TD><B>([^<]+)</B></td>").find(line)
            if (boldMatch != null) {
                currentName = boldMatch.groupValues[1].trim()
                continue
            }

            // Find source code in select: name='listHdmiAssignMPLAY'
            if (currentName != null) {
                val selectMatch = Regex("name='list\\w+Assign([A-Z0-9/]+)'").find(line)
                if (selectMatch != null) {
                    val code = selectMatch.groupValues[1]
                    // Skip empty names or placeholder "---"
                    if (currentName.isNotBlank() && currentName != "---") {
                        mappings[code] = currentName
                    }
                    currentName = null
                }
            }
        }

        return mappings
    }
}
