# OpenOSD

**Android TV overlay app that shows a native 4K-sharp OSD whenever you change volume, source, or sound mode on your AV receiver.**

No more looking at the receiver's tiny display — OpenOSD draws a clean, hardware-accelerated overlay directly on your TV.

![OpenOSD Screenshot](docs/screenshot.png)

## Features

- Volume bar with dB display
- Source / sound mode info panel
- Audyssey status (MultEQ, Dynamic EQ, Dynamic Volume)
- ECO mode indicator
- Fully transparent Canvas rendering — crisp at 4K/1080p
- HTTP polling, no Telnet required
- Works while any other app is running

## Supported Hardware

**AV Receivers** (HTTP API via port 80):
- Denon AVR-X1200W and similar (tested ✓)
- Denon AVR-X series (X1xxx–X6xxx)
- Marantz SR/NR series (same HTTP API)

**TV / Display devices:**
- Android TV (minSdk 22) ✓
- Google TV ✓
- Amazon Fire TV (sideload) ✓

## Installation

### From GitHub Releases
Download the latest `OpenOSD.apk` from [Releases](../../releases) and sideload it.

### Build from source
```bash
git clone https://github.com/st0nebyte/OpenOSD.git
cd OpenOSD
./gradlew assembleDebug
```

## Setup

1. Install APK on your Android TV device
2. Grant overlay permission:
   - **Android TV / Google TV:** Settings → Apps → OpenOSD → Display over other apps
   - **Fire TV:** `adb shell appops set dev.st0nebyte.openosd SYSTEM_ALERT_WINDOW allow`
3. Open OpenOSD, enter your AVR's IP address, tap **Start OSD**
4. Change volume on your AVR → overlay appears

## How it works

OpenOSD polls the AVR's HTTP API every 400ms (status lite) and every 5s (full status). On state change it draws a Canvas overlay via `WindowManager` — no root, no special permissions beyond `SYSTEM_ALERT_WINDOW`.

## License

MIT License — see [LICENSE](LICENSE)
