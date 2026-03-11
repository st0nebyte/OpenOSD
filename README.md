# OpenOSD

**Android TV overlay app that shows a modern, minimal OSD whenever you change volume on your Denon AVR.**

No more looking at the receiver's tiny display — OpenOSD draws a clean, hardware-accelerated overlay directly on your TV with **instant 0ms lag** using Telnet push updates (or automatic HTTP fallback if Port 23 is blocked).

## Features

### Core Functionality
- **Automatic Protocol Detection:**
  - Tries Telnet first (0ms lag, instant push updates)
  - Auto-falls back to HTTP if Port 23 blocked (~500ms polling)
  - Works out-of-the-box on all receivers!
- **Split OSD Layout:**
  - **Volume OSD:** Always bottom-center, compact (200x36dp)
  - **Info OSD:** Top-left, only in INFO/EXTENDED modes
  - No more giant info box obscuring the screen during volume changes!
- **Animated Volume Bar:** Smooth volume display with dB precision (0.5 dB steps)
- **Three Display Modes:**
  - **STANDARD:** Minimal volume-only display
  - **INFO:** Volume (bottom) + input source + sound mode (top-left)
  - **EXTENDED:** Complete info with **compact speaker layout**, full audio format names, and technical details
- **Three Scale Options:** Small (75%), Medium (100%), Large (130%) for minimal screen coverage
- **Custom Source Names:**
  - Auto-syncs from receiver's web UI (MPLAY → "Fire TV", etc.)
  - Manual configuration available
- **Full Audio Format Names:** No abbreviations - shows complete format like "DOLBY ATMOS", "DTS:X MSTR", "DOLBY HD+DS"
- **Compact Speaker Layout:** Box-style display (like original Denon OSD) showing active speakers grouped by position (EXTENDED mode)
- **Modern Design:** Glassy light theme with smooth animations
- **Mute Indicator:** Red text when muted

### Technical Features
- **Dual Protocol Support:**
  - Telnet (port 23): Push updates, 0ms lag, instant response
  - HTTP fallback: Polling mode, ~500ms lag, works when Port 23 blocked
  - Automatic detection and switching
- **Source Name Sync:**
  - Auto-fetches custom source names from receiver web UI
  - Manual configuration available
  - Persistent storage of custom names
- Fully hardware-accelerated Canvas rendering — crisp at 4K/1080p
- **Compact speaker layout** with box-style grouping (like original Denon OSD)
- **Full audio format names** (DOLBY ATMOS, DTS:X MSTR, DOLBY HD+DS, etc.)
- Speaker configuration detection (2.0, 2.1, 5.1, 7.1, 5.1.2 Atmos, etc.)
- Signal detection (HDMI, DIGITAL, ANALOG, ARC)
- Digital mode tracking (AUTO, PCM, DTS)
- Advanced audio features:
  - Dynamic Range Compression (DRC) status
  - Audio Restorer status
  - HDMI Audio Output routing (AMP/TV)
  - ECO mode indicator
- Works while any other app is running
- Foreground service with persistent notification
- No root required
- Seamless APK updates (same signing key across versions)

## Supported Hardware

**AV Receivers:**
- Denon AVR-X1200W (tested ✓ - Telnet + HTTP)
- Denon AVR-X series with network support
  - **Works out-of-the-box:** HTTP mode always works
  - **Optional 0ms lag:** Unlock Port 23 for Telnet (see [TELNET.md](TELNET.md))
  - **Single connection limit:** Only one Telnet client at a time (HTTP has no limit)

**TV / Display devices:**
- Android TV (Android 6.0+) ✓
- Google TV ✓
- Amazon Fire TV (sideload + ADB) ✓
- Any Android device with overlay permissions (phones, tablets for testing)

## Installation

### From GitHub Releases
Download the latest `OpenOSD.apk` from [Releases](../../releases) and sideload it.

### Build from source
```bash
git clone https://github.com/st0nebyte/OpenOSD.git
cd OpenOSD
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Setup

### 1. Install APK
Sideload the APK to your Android TV device using ADB or a file manager app.

### 2. Grant Overlay Permission

**Android TV / Google TV:**
- Settings → Apps → OpenOSD → Permissions → Display over other apps → Allow

**Amazon Fire TV:**
Fire TV doesn't have a settings UI for overlay permissions. Use ADB:
```bash
adb connect YOUR_FIRETV_IP:5555
adb shell appops set dev.st0nebyte.openosd SYSTEM_ALERT_WINDOW allow
```

### 3. Configure AVR Connection
1. Open OpenOSD app
2. Enter your AVR's IP address (e.g., `192.168.1.100`)
3. Select display mode:
   - **Standard:** Minimal volume-only display
   - **Info:** Volume + source + sound mode
   - **Extended:** Complete technical information
4. Select scale:
   - **Klein (75%):** Minimal screen coverage
   - **Mittel (100%):** Balanced size (default)
   - **Groß (130%):** Maximum visibility
5. Tap **OSD STARTEN**

### 4. Test
Change the volume on your AVR — the OSD should appear instantly on your TV.

## Display Mode Examples

**Split OSD Layout - True to Scale:**
All examples below show **MEDIUM scale (100%)** on a **1920×1080 Full HD TV**. Position and size are pixel-accurate based on actual code.

See [DOCUMENTATION.md](DOCUMENTATION.md) for detailed visual examples and technical specifications.

---

### STANDARD Mode
Shows only volume bar at bottom-center (200×36dp):

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ 1920px
┃                                                                                 ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                          Your Content Here (1920×1080)                          ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                    ╭─────────────────────────────────╮                          ┃
┃                    │ VOL  ▰▰▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱▱▱    45 │ ← 200×36dp @ 80dp from  ┃
┃                    ╰─────────────────────────────────╯    bottom, centered     ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛ 1080px
```

**Colors:** Semi-transparent dark background (#CC0A0F14) • Blue volume bar (#90B0D0F0) • White text (#E5FFFFFF)

---

### INFO Mode
Adds info box at top-left (280×70dp):

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ ╭────────────────────────────────────────╮                                     ┃
┃ │ Fire TV • DIRECT                       │ ← 280×70dp @ 24dp from top-left     ┃
┃ │ HDMI • AUTO                            │                                     ┃
┃ ╰────────────────────────────────────────╯                                     ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                          Your Content Here                                      ┃
┃                                                                                 ┃
┃                                                                                 ┃
┃                    ╭─────────────────────────────────╮                          ┃
┃                    │ VOL  ▰▰▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱▱▱    45 │                          ┃
┃                    ╰─────────────────────────────────╯                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

**Info Shows:** Custom source name (synced from receiver) • Sound mode • Signal/format info

---

### EXTENDED Mode
Complete info with compact speaker layout (280×140dp):

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ ╭────────────────────────────────────────╮                                     ┃
┃ │ Fire TV • DOLBY ATMOS                  │ ← 280×140dp @ 24dp from top-left    ┃
┃ │ HDMI • AUTO • DRC:AUTO                 │                                     ┃
┃ │                                        │                                     ┃
┃ │ FRONT SPEAKERS                         │                                     ┃
┃ │ ┌────┐ ┌────┐ ┌────┐                  │   Compact speaker boxes             ┃
┃ │ │ FL │ │ C  │ │ FR │                  │   (grouped by position)             ┃
┃ │ └────┘ └────┘ └────┘                  │                                     ┃
┃ │ ┌────┐ ┌────┐ ┌────┐                  │                                     ┃
┃ │ │ SL │ │ SR │ │ SW │                  │                                     ┃
┃ │ └────┘ └────┘ └────┘                  │                                     ┃
┃ ╰────────────────────────────────────────╯                                     ┃
┃                                                                                 ┃
┃                          Your Content Here                                      ┃
┃                                                                                 ┃
┃                    ╭─────────────────────────────────╮                          ┃
┃                    │ VOL  ▰▰▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱▱▱    45 │                          ┃
┃                    ╰─────────────────────────────────╯                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

**Extended Shows:**
- **Full audio format names:** DOLBY ATMOS, DTS:X MSTR, DOLBY HD+DS (no abbreviations!)
- **Compact speaker boxes:** Grouped by position (Front/Height/Surround) like Denon's original OSD
- **Signal info:** HDMI, DIGITAL, ANALOG, ARC
- **Technical details:** DRC, Audio Restorer, HDMI routing (→TV), ECO mode

---

**Scale Options:**
- **SMALL (75%):** Volume: 150×27dp | Info: 210×52dp (INFO) / 210×105dp (EXTENDED)
- **MEDIUM (100%):** Volume: 200×36dp | Info: 280×70dp (INFO) / 280×140dp (EXTENDED) ← shown above
- **LARGE (130%):** Volume: 260×47dp | Info: 364×91dp (INFO) / 364×182dp (EXTENDED)

**Actual Colors (from code):**
- Background: `#CC0A0F14` (80% opaque dark blue-grey)
- Border: `#18FFFFFF` (9% white)
- Volume Bar Fill: `#90B0D0F0` (56% light blue)
- Text: `#E5FFFFFF` (90% white)
- Mute Text: `#D0FF6060` (82% red)

## How it Works

OpenOSD automatically detects the best connection method for your receiver:

**Automatic Protocol Detection:**
1. **Try Telnet first** (port 23) - Instant push-based updates with 0ms lag
2. **Auto-fallback to HTTP** if Port 23 is blocked - Polling mode with ~500ms lag
3. **Works out-of-the-box** - No manual configuration needed!

**Telnet Mode (Preferred):**
1. **Connection:** Opens TCP connection to AVR port 23
2. **Initial Query:** Requests current state (power, volume, mute, source, mode, signal, speakers)
3. **Push Updates:** AVR sends unsolicited updates whenever state changes (0ms lag)
4. **Parsing:** Text-based protocol with commands like `MV50` (volume 50), `PWON` (power on)
5. **Overlay:** Draws Canvas-based overlay via `WindowManager.TYPE_APPLICATION_OVERLAY`
6. **Animation:** Smooth volume bar animation with fade-out after 3 seconds

**HTTP Mode (Fallback):**
- Polls AVR XML status endpoints every 150-400ms
- More compatible (works even if Port 23 is locked)
- No single-connection limit
- Still provides smooth OSD experience

**Protocol Status:** Check the notification to see which protocol is active: "Verbunden ✓ (Telnet)" or "Verbunden ✓ (HTTP)"

## Troubleshooting

### OSD doesn't appear
- Verify overlay permission is granted
- Check AVR IP address is correct
- Ensure AVR is powered on
- Check notification for connection status

### Port 23 locked (Telnet unavailable)
**The app automatically handles this!** If Port 23 is blocked, OpenOSD will:
1. Try Telnet for 5 seconds
2. Automatically fall back to HTTP polling
3. Show a toast: "Port 23 gesperrt - nutze HTTP (langsamer)"
4. Continue working with ~500ms lag instead of 0ms

**Want 0ms lag?** Some Denon X-series models have Port 23 locked by default. See [TELNET.md](TELNET.md) for optional unlock instructions (requires hardware reset). This is purely for performance optimization - the app works fine without it!

### Fire TV overlay permission
Fire TV requires ADB to grant overlay permission. Make sure you run:
```bash
adb shell appops set dev.st0nebyte.openosd SYSTEM_ALERT_WINDOW allow
```

## Contributing

Contributions welcome! Please open an issue or PR.

### Development
- Language: Kotlin
- Min SDK: 22 (Android 5.1)
- Target SDK: 34 (Android 14)
- Architecture: Single-activity with foreground service

## License

MIT License — see [LICENSE](LICENSE)

## Documentation

- [DOCUMENTATION.md](DOCUMENTATION.md) - Detailed visual examples and technical specifications
- [TELNET.md](TELNET.md) - Telnet setup guide for Denon X-series AVRs
- [X1200W_EU_Commands.csv](X1200W_EU_Commands.csv) - Complete command reference for AVR-X1200W EU model
