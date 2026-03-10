package dev.st0nebyte.openosd

import org.junit.Assert.*
import org.junit.Test

class SourceNameSyncTest {

    @Test
    fun parseSourceNames_extractsAllMappings() {
        val html = """
            <TD><B>Fire TV</B></td>
            <TD><select name='listHdmiAssignMPLAY'>
            <TD><B>Unraid</B></td>
            <TD><select name='listHdmiAssignGAME'>
            <TD><B>TV Audio</B></td>
            <TD><select name='listDigitalAssignTV'>
        """.trimIndent()

        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, html) as Map<String, String>

        assertEquals(3, result.size)
        assertEquals("Fire TV", result["MPLAY"])
        assertEquals("Unraid", result["GAME"])
        assertEquals("TV Audio", result["TV"])
    }

    @Test
    fun parseSourceNames_skipsEmptyNames() {
        val html = """
            <TD><B></B></td>
            <TD><select name='listHdmiAssignBD'>
            <TD><B>---</B></td>
            <TD><select name='listHdmiAssignDVD'>
        """.trimIndent()

        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, html) as Map<String, String>

        assertEquals(0, result.size)
    }

    @Test
    fun parseSourceNames_handlesMultipleAssignTypes() {
        val html = """
            <TD><B>My Source</B></td>
            <TD><select name='listHdmiAssignAUX1'>
            <TD><select name='listDigitalAssignAUX1'>
            <TD><select name='listAnalogAssignAUX1'>
        """.trimIndent()

        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, html) as Map<String, String>

        // Should match first occurrence (HDMI)
        assertEquals(1, result.size)
        assertEquals("My Source", result["AUX1"])
    }

    @Test
    fun parseSourceNames_handlesRealDenonHTML() {
        val html = """
  <tr height="30">
	<TD><B>Fire TV</B></td>
    <TD><select size='1' name='listHdmiAssignMPLAY' onchange='listBoxHDMI()'><OPTION value='HD1'>1</OPTION><OPTION value='HD2'>2</OPTION><OPTION value='HD3' selected>3</OPTION><OPTION value='HD4'>4</OPTION><OPTION value='HD5'>5</OPTION><OPTION value='FRO'>Front</OPTION><OPTION value='OFF'>-</OPTION></SELECT></td>
	<TD><select size='1' name='listDigitalAssignMPLAY' onchange='listBoxDIGITAL()'><OPTION value='OP1'>OPT1</OPTION><OPTION value='OP2'>OPT2</OPTION><OPTION value='OFF' selected>-</OPTION></SELECT></td>
  </tr>
        """.trimIndent()

        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, html) as Map<String, String>

        assertEquals(1, result.size)
        assertEquals("Fire TV", result["MPLAY"])
    }

    @Test
    fun parseSourceNames_handlesSlashInCode() {
        val html = """
            <TD><B>Cable Box</B></td>
            <TD><select name='listHdmiAssignSAT/CBL'>
        """.trimIndent()

        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, html) as Map<String, String>

        assertEquals(1, result.size)
        assertEquals("Cable Box", result["SAT/CBL"])
    }

    @Test
    fun parseSourceNames_emptyHTML_returnsEmptyMap() {
        val method = SourceNameSync.javaClass.getDeclaredMethod("parseSourceNames", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(SourceNameSync, "") as Map<String, String>

        assertEquals(0, result.size)
    }
}
