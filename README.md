# OpenOSD

**Android TV overlay app that shows a modern, minimal OSD whenever you change volume on your Denon AVR.**

No more looking at the receiver's tiny display — OpenOSD draws a clean, hardware-accelerated overlay directly on your TV with **instant 0ms lag** using Telnet push updates.

## Features

### Core Functionality
- **Instant Updates:** Telnet push-based updates (0ms lag, no polling)
- **Animated Volume Bar:** Smooth volume display with dB precision (0.5 dB steps)
- **Three Display Modes:**
  - **STANDARD:** Minimal volume-only display
  - **INFO:** Volume + input source + sound mode
  - **EXTENDED:** Complete info with signal type, digital format, and speaker configuration
- **Three Scale Options:** Small (75%), Medium (100%), Large (130%) for minimal screen coverage
- **Modern Design:** Glassy semi-transparent aesthetic with smooth animations
- **Consistent Alignment:** Center-bottom positioning regardless of scale
- **Mute Indicator:** Red text when muted

### Technical Features
- Fully hardware-accelerated Canvas rendering — crisp at 4K/1080p
- Telnet protocol (port 23) with push updates
- Speaker configuration detection (2.0, 2.1, 5.1, 7.1, 5.1.2 Atmos, etc.)
- Signal detection (HDMI, DIGITAL, ANALOG, ARC)
- Digital mode tracking (AUTO, PCM, DTS)
- Works while any other app is running
- Foreground service with persistent notification
- No root required

## Supported Hardware

**AV Receivers** (Telnet port 23):
- Denon AVR-X1200W (tested ✓)
- Denon AVR-X series with Telnet support
  - **Note:** Port 23 may require hardware reset to unlock on some X-series models
  - See [TELNET.md](TELNET.md) for setup instructions

**TV / Display devices:**
- Android TV (Android 6.0+) ✓
- Google TV ✓
- Amazon Fire TV (sideload + ADB) ✓

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

See [DOCUMENTATION.md](DOCUMENTATION.md) for detailed visual examples and technical specifications.

### STANDARD Mode
```
┌──────────────────────────┐
│  VOL ▓▓▓▓▓░░░░░  45     │
└──────────────────────────┘
```

### INFO Mode
```
┌──────────────────────────┐
│  VOL ▓▓▓▓▓░░░░░  45     │
│                          │
│    GAME • DIRECT         │
└──────────────────────────┘
```

### EXTENDED Mode
```
┌──────────────────────────┐
│  VOL ▓▓▓▓▓░░░░░  45     │
│                          │
│    GAME • DIRECT         │
│                          │
│  HDMI • AUTO • 5.1       │
└──────────────────────────┘
```

## How it Works

OpenOSD uses the Denon Telnet protocol (port 23) for instant push-based updates:

1. **Connection:** Opens TCP connection to AVR port 23
2. **Initial Query:** Requests current state (power, volume, mute, source, mode, signal, speakers)
3. **Push Updates:** AVR sends unsolicited updates whenever state changes (0ms lag)
4. **Parsing:** Text-based protocol with commands like `MV50` (volume 50), `PWON` (power on)
5. **Overlay:** Draws Canvas-based overlay via `WindowManager.TYPE_APPLICATION_OVERLAY`
6. **Animation:** Smooth volume bar animation with fade-out after 3 seconds

**Advantages over HTTP polling:**
- Instant response (0ms lag vs 150-400ms polling delay)
- No network overhead (push vs constant polling)
- More efficient battery usage
- Real-time synchronization

## Troubleshooting

### OSD doesn't appear
- Verify overlay permission is granted
- Check AVR IP address is correct
- Ensure AVR is powered on
- Check notification for connection status

### Port 23 connection fails
Some Denon X-series models have Telnet port 23 locked by default. See [TELNET.md](TELNET.md) for unlock instructions (requires hardware reset).

Alternatively, you can modify `OSDService.kt` line 62 to use HTTP polling instead:
```kotlin
// Use HTTP polling instead of Telnet:
client = AVRClient(host, ::onUpdate, ::onConnected).also { it.start() }
```

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
