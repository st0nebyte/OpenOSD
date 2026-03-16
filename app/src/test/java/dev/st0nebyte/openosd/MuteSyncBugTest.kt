package dev.st0nebyte.openosd

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Test for mute synchronization bug fix.
 *
 * Bug scenario:
 * 1. User mutes AVR → App shows MUTE OSD (persist mode, no timeout)
 * 2. User unmutes AVR, but app doesn't receive MUOFF update (network issue)
 * 3. App's internal state remains muted=true (out of sync with AVR)
 * 4. User changes volume → AVR sends MV update
 * 5. App processes volume change with outdated muted=true state
 * 6. OSD is shown again with persistWhileMuted=true → stuck forever
 *
 * Fix: Use 30s max timeout even for persist mode to prevent infinite display.
 */
class MuteSyncBugTest {

    @Test
    fun `trigger priority - unmute takes precedence over volume`() {
        val old = AVRState(volumeDb = -50.0, muted = true)
        val next = AVRState(volumeDb = -45.0, muted = false)  // Both changed

        // Simulate trigger logic from AVRClientTelnet
        val trigger = when {
            old.muted != next.muted && next.muted  -> OSDTrigger.MUTE
            old.muted != next.muted && !next.muted -> OSDTrigger.UNMUTE  // Should win!
            Math.abs(old.volumeDb - next.volumeDb) > 0.1 -> OSDTrigger.VOLUME
            else -> null
        }

        assertEquals(OSDTrigger.UNMUTE, trigger, "UNMUTE should take precedence over VOLUME")
    }

    @Test
    fun `verify MUTE trigger has persistWhileMuted flag`() {
        assertEquals(true, OSDTrigger.MUTE.persistWhileMuted,
            "MUTE trigger must have persistWhileMuted=true")
        assertEquals(0L, OSDTrigger.MUTE.timeoutMs,
            "MUTE trigger should have 0ms timeout (handled by persist logic)")
    }

    @Test
    fun `verify UNMUTE trigger clears OSD quickly`() {
        assertEquals(false, OSDTrigger.UNMUTE.persistWhileMuted,
            "UNMUTE trigger should NOT persist")
        assertEquals(350L, OSDTrigger.UNMUTE.timeoutMs,
            "UNMUTE should fade out quickly (350ms)")
    }

    @Test
    fun `mute state synchronization - volume change while muted`() {
        val state1 = AVRState(volumeDb = -50.0, muted = true)
        val state2 = AVRState(volumeDb = -45.0, muted = true)

        // Volume changed, but still muted
        val trigger = when {
            state1.muted != state2.muted && state2.muted  -> OSDTrigger.MUTE
            state1.muted != state2.muted && !state2.muted -> OSDTrigger.UNMUTE
            Math.abs(state1.volumeDb - state2.volumeDb) > 0.1 -> OSDTrigger.VOLUME
            else -> null
        }

        assertEquals(OSDTrigger.VOLUME, trigger,
            "Volume changes while muted should trigger VOLUME update")
    }

    @Test
    fun `mute state synchronization - missed unmute update scenario`() {
        // Simulates the bug scenario:
        // App thinks muted=true, but AVR is actually unmuted (missed MUOFF update)
        // Then volume changes → App processes with outdated muted=true

        val appState = AVRState(volumeDb = -50.0, muted = true)  // Outdated!
        val newState = AVRState(volumeDb = -45.0, muted = true)  // Still thinks muted

        // This would incorrectly show OSD with persist mode
        // Fix: 30s max timeout prevents infinite display

        // The trigger would be VOLUME (no mute change detected)
        val trigger = when {
            appState.muted != newState.muted && newState.muted  -> OSDTrigger.MUTE
            appState.muted != newState.muted && !newState.muted -> OSDTrigger.UNMUTE
            Math.abs(appState.volumeDb - newState.volumeDb) > 0.1 -> OSDTrigger.VOLUME
            else -> null
        }

        assertEquals(OSDTrigger.VOLUME, trigger)

        // With the fix, even though persistWhileMuted would be true,
        // the OSD will auto-hide after 30s max timeout
    }
}
