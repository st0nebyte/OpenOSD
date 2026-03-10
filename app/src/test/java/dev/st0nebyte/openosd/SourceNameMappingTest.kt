package dev.st0nebyte.openosd

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SourceNameMappingTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences("openosd_prefs", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
    }

    @Test
    fun getDisplayName_noUserMapping_returnsDefaultMapping() {
        `when`(mockPrefs.getString("source_name_MPLAY", null)).thenReturn(null)

        val result = SourceNameMapping.getDisplayName(mockContext, "MPLAY")

        assertEquals("Media Player", result)
    }

    @Test
    fun getDisplayName_withUserMapping_returnsCustomName() {
        `when`(mockPrefs.getString("source_name_MPLAY", null)).thenReturn("My Custom Name")

        val result = SourceNameMapping.getDisplayName(mockContext, "MPLAY")

        assertEquals("My Custom Name", result)
    }

    @Test
    fun getDisplayName_unknownCode_returnsRawCode() {
        `when`(mockPrefs.getString("source_name_UNKNOWN", null)).thenReturn(null)

        val result = SourceNameMapping.getDisplayName(mockContext, "UNKNOWN")

        assertEquals("UNKNOWN", result)
    }

    @Test
    fun setDisplayName_savesToPreferences() {
        SourceNameMapping.setDisplayName(mockContext, "MPLAY", "Plex Server")

        verify(mockEditor).putString("source_name_MPLAY", "Plex Server")
        verify(mockEditor).apply()
    }

    @Test
    fun hasCustomName_withUserMapping_returnsTrue() {
        `when`(mockPrefs.contains("source_name_MPLAY")).thenReturn(true)

        val result = SourceNameMapping.hasCustomName(mockContext, "MPLAY")

        assertTrue(result)
    }

    @Test
    fun hasCustomName_noUserMapping_returnsFalse() {
        `when`(mockPrefs.contains("source_name_MPLAY")).thenReturn(false)

        val result = SourceNameMapping.hasCustomName(mockContext, "MPLAY")

        assertFalse(result)
    }

    @Test
    fun resetToDefault_removesFromPreferences() {
        SourceNameMapping.resetToDefault(mockContext, "MPLAY")

        verify(mockEditor).remove("source_name_MPLAY")
        verify(mockEditor).apply()
    }

    @Test
    fun getAllSourceCodes_returnsAllKnownSources() {
        val codes = SourceNameMapping.getAllSourceCodes()

        assertTrue(codes.contains("MPLAY"))
        assertTrue(codes.contains("BD"))
        assertTrue(codes.contains("DVD"))
        assertTrue(codes.contains("TV"))
        assertTrue(codes.contains("GAME"))
    }

    @Test
    fun getAllSourceCodes_returnsSortedList() {
        val codes = SourceNameMapping.getAllSourceCodes()

        val sorted = codes.sorted()
        assertEquals(sorted, codes)
    }

    @Test
    fun defaultMappings_bluRayFormat() {
        `when`(mockPrefs.getString("source_name_BD", null)).thenReturn(null)

        val result = SourceNameMapping.getDisplayName(mockContext, "BD")

        assertEquals("Blu-ray", result)
    }

    @Test
    fun defaultMappings_cableSatFormat() {
        `when`(mockPrefs.getString("source_name_SAT/CBL", null)).thenReturn(null)

        val result = SourceNameMapping.getDisplayName(mockContext, "SAT/CBL")

        assertEquals("Cable", result)
    }
}
