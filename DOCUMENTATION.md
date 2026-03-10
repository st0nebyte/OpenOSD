# OpenOSD Documentation

## Overview

OpenOSD provides a modern, minimal on-screen display (OSD) overlay for Denon AVR receivers on Android TV. The OSD shows real-time volume, source, and audio information with automatic protocol detection (Telnet for 0ms lag, or HTTP fallback for maximum compatibility).

## Display Modes

### STANDARD Mode
Shows only essential volume information.

```
┌──────────────────────────────────────┐
│  VOL ▓▓▓▓▓▓▓▓▓▓░░░░░░░░  45         │
└──────────────────────────────────────┘
```

**Features:**
- Volume bar with animated fill
- Current volume level (0-98 scale)
- Mute indicator (shows "MUTE" in red when muted)

**Screen Coverage:** Minimal (40dp height × 240dp width)

---

### INFO Mode
Adds input source and sound mode information.

```
┌──────────────────────────────────────┐
│  VOL ▓▓▓▓▓▓▓▓▓▓░░░░░░░░  45         │
│                                      │
│        GAME • DIRECT                 │
└──────────────────────────────────────┘
```

**Features:**
- All STANDARD features
- Input source (e.g., GAME, DVD, TV, BLU-RAY)
- Sound mode (e.g., STEREO, DIRECT, DD, SURROUND)

**Screen Coverage:** Medium (60dp height × 240dp width)

**Common Sound Modes:**
- `STEREO` - 2-channel stereo
- `DIRECT` - Direct passthrough
- `DD` - Dolby Digital
- `SURROUND` - Dolby Surround
- `DTS` - DTS Surround

---

### EXTENDED Mode
Complete information including signal type, full audio format names, technical details, and **visual speaker layout**.

```
┌──────────────────────────────────────┐
│  VOL ▓▓▓▓▓▓▓▓▓▓░░░░░░░░  45         │
│                                      │
│   GAME • DOLBY ATMOS                 │
│                                      │
│   HDMI • AUTO • DRC:AUTO HDMI→AMP   │
│                                      │
│     TFL    TFR     ← Visual Layout   │
│                                      │
│   FL   C   FR      (Overhead View)   │
│                                      │
│   SL  ○  SR        ○ = Listener      │
│                                      │
│     SW                               │
└──────────────────────────────────────┘
```

**Features:**
- All INFO features
- **Custom source names** (e.g., "Fire TV" instead of "MPLAY")
- **Full audio format names** (no abbreviations):
  - `DOLBY ATMOS`, `DOLBY HD MSTR`, `DTS:X MSTR`
  - `DOLBY HD+DS` (Dolby TrueHD + Dolby Surround Upmix)
  - `DTS HD+NEURAL:X` (DTS-HD + Neural:X Upmix)
- Signal detection (HDMI, DIGITAL, ANALOG, ARC)
- Digital mode (AUTO, PCM, DTS)
- **Visual speaker layout** (overhead view):
  - Active speakers highlighted in color
  - Inactive speakers shown dimmed/transparent
  - Individual speaker positions (FL, FR, C, SW, SL, SR, etc.)
  - Supports Atmos height speakers (TFL, TFR, FHL, FHR)
  - Listener position marker
- Technical details:
  - Dynamic Range Compression (DRC: OFF/AUTO/LOW/MID/HI)
  - Audio Restorer (Restorer: OFF/LOW/MED/HI)
  - HDMI Audio Output routing (HDMI→AMP or HDMI→TV)
  - ECO Mode status (ECO:ON/AUTO/OFF)

**Screen Coverage:** Maximum (120dp height × 240dp width)

**Audio Format Examples:**
- `DOLBY DIGITAL` - Dolby Digital 5.1
- `DOLBY ATMOS` - Native Dolby Atmos object-based audio
- `DTS:X MSTR` - DTS:X Master Audio
- `DOLBY HD+DS` - Dolby TrueHD source + Dolby Surround upmixing
- `DTS+NEURAL:X` - DTS source + Neural:X upmixing
- `DIRECT` - Direct mode (no processing)

**Signal Types:**
- `HDMI` - HDMI digital input
- `DIGITAL` - S/PDIF digital input
- `ANALOG` - Analog audio input
- `ARC` - Audio Return Channel (TV)

**Speaker Configurations:**
- `2.0` - Stereo (FL, FR)
- `2.1` - Stereo with subwoofer (FL, FR, SW)
- `5.1` - 5.1 surround (FL, FR, C, SW, SL, SR)
- `7.1` - 7.1 surround (FL, FR, C, SW, SL, SR, SBL, SBR)
- `5.1.2` - 5.1 with 2 height speakers (Atmos)

---

## Scale Options

All display modes support three scaling levels to minimize screen coverage:

### SMALL (75%)
Minimal screen coverage for unobtrusive display.
- Width: 180dp
- Height: 30dp (STANDARD), 45dp (INFO), 60dp (EXTENDED)

### MEDIUM (100%) - Default
Balanced size for comfortable viewing.
- Width: 240dp
- Height: 40dp (STANDARD), 60dp (INFO), 80dp (EXTENDED)

### LARGE (130%)
Maximum visibility for larger screens.
- Width: 312dp
- Height: 52dp (STANDARD), 78dp (INFO), 156dp (EXTENDED)

**Note:** All UI elements maintain consistent center-bottom alignment regardless of scale.

---

## Speaker Layout Visualization

The EXTENDED mode includes an **overhead view** of your speaker configuration:

### Layout Features
- **Active Speakers:** Highlighted in color with labels (FL, FR, C, etc.)
- **Inactive Speakers:** Shown dimmed/transparent
- **Listener Position:** Center reference point (○)
- **Real-time Updates:** Layout updates when AVR configuration changes

### Supported Speaker Positions
- **Front:** FL (Front Left), FR (Front Right), C (Center)
- **Surround:** SL (Surround Left), SR (Surround Right)
- **Surround Back:** SB (Single), SBL/SBR (Dual)
- **Subwoofer:** SW, SW2 (Dual subs)
- **Front Height (Atmos):** FHL, FHR
- **Top Front (Atmos):** TFL, TFR
- **Top Middle (Atmos):** TML, TMR
- **Dolby Speakers:** FDL, FDR, SDL, SDR

### Example Configurations Visualized
- **2.0 Stereo:** FL, FR
- **2.1:** FL, FR, SW
- **5.1:** FL, FR, C, SW, SL, SR
- **7.1:** FL, FR, C, SW, SL, SR, SBL, SBR
- **5.1.2 (Atmos):** 5.1 + TFL, TFR or FHL, FHR
- **7.1.2 (Atmos):** 7.1 + TFL, TFR
- **Custom:** Any combination supported by AVR

**Note:** All UI elements maintain consistent center-bottom alignment regardless of scale.

---

## Visual Examples by Configuration

### Example 1: Gaming Setup - STANDARD MEDIUM
Perfect for minimal distraction during gameplay.

```
Display Mode: STANDARD
Scale: MEDIUM (100%)
Screen Coverage: ~2% (1080p), ~1% (4K)

┌─────────────────────────────┐
│  VOL ▓▓▓▓▓▓░░░░░░░░  35    │
└─────────────────────────────┘
```

---

### Example 2: Movie Watching - INFO SMALL
Shows source and audio format without blocking content.

```
Display Mode: INFO
Scale: SMALL (75%)
Screen Coverage: ~1.5% (1080p), ~0.8% (4K)

┌────────────────────────────┐
│  VOL ▓▓▓▓▓▓▓▓▓░░░░  52.5  │
│                            │
│    BLU-RAY • DTS           │
└────────────────────────────┘
```

---

### Example 3: Audiophile Setup - EXTENDED MEDIUM
Complete technical information with visual speaker layout for home theater enthusiasts.

```
Display Mode: EXTENDED
Scale: MEDIUM (100%)
Screen Coverage: ~4.5% (1080p), ~2.2% (4K)

┌──────────────────────────────────────┐
│  VOL ▓▓▓▓▓▓▓▓▓▓▓░░░░░  65           │
│                                      │
│    BLU-RAY • DOLBY ATMOS             │
│                                      │
│   HDMI • AUTO • DRC:AUTO HDMI→AMP   │
│                                      │
│      TFL         TFR                 │
│                                      │
│    FL    C    FR                     │
│                                      │
│   SL     ○     SR                    │
│                                      │
│   SW1   SB    SW2                    │
└──────────────────────────────────────┘

Active speakers (highlighted): FL, FR, C, SW1, SW2, SL, SR, SB, TFL, TFR
Configuration: 7.1.2 (Dolby Atmos)
```

---

### Example 4: Muted State - Any Mode
Red text indicates muted state.

```
Display Mode: STANDARD
Muted: Yes

┌─────────────────────────────┐
│  MUTE ▓▓▓▓▓▓░░░░░░░░  35   │  (MUTE shown in red)
└─────────────────────────────┘
```

---

## Visual Design

### Color Palette
OpenOSD uses a modern glassy aesthetic:

- **Background:** Semi-transparent dark blue-grey (80% opacity)
- **Border:** Subtle white outline (10% opacity)
- **Text:** Off-white (90% opacity) for values
- **Text Dim:** Dimmed white (50% opacity) for labels
- **Accent:** Soft blue-white (38% opacity) for highlights
- **Volume Bar:** Soft blue-white fill (56% opacity)
- **Mute Text:** Soft red (82% opacity)

### Alignment
All elements are consistently aligned:
- **Horizontal:** Center-aligned
- **Vertical:** Bottom-aligned
- **Position:** 60dp from bottom of screen
- **Consistent:** Scale changes maintain alignment

### Animation
- Volume bar animates smoothly with 120ms duration
- Fade-out transition after 3 seconds (350ms duration)
- Deceleration curve for natural feel

---

## Technical Specifications

### Connection Protocols

**Automatic Detection:**
- Tries Telnet first (Port 23) for instant 0ms lag
- Falls back to HTTP if Port 23 is blocked (~500ms lag)
- Works out-of-the-box on all receivers

**Telnet Protocol (Preferred):**
- **Port:** 23 (TCP)
- **Format:** ASCII text commands ending with `\r` (CR)
- **Updates:** Push-based (instant, 0ms lag)
- **Limitation:** Single connection only
- **Commands Used:**
  - `PW?` - Query power status
  - `MV?` - Query master volume
  - `MU?` - Query mute status
  - `SI?` - Query input source
  - `MS?` - Query sound mode (full format names)
  - `SD?` - Query signal detection
  - `DC?` - Query digital mode
  - `CV?` - Query channel volume (active speakers)
  - `PSDRC ?` - Query Dynamic Range Compression
  - `PSRSTR ?` - Query Audio Restorer
  - `VSAUDIO ?` - Query HDMI Audio Output routing
  - `ECO?` - Query ECO mode status

**HTTP Protocol (Fallback):**
- **Method:** XML polling
- **Endpoints:**
  - `/goform/formMainZone_MainZoneXmlStatus.xml` - Main zone status
  - `/goform/formNetAudio_StatusXml.xml` - Network audio status
  - `/SETUP/INPUTS/INPUTASSIGN/d_InputAssign.asp` - Source name mapping
- **Polling Rate:** ~2-5 requests/second during updates
- **Limitations:** None (unlimited connections)

### Source Name Mapping
- **Auto-sync:** Fetches custom source names from receiver's web UI
- **Manual Config:** User can configure names in app settings
- **Storage:** Persistent via SharedPreferences
- **Default Mappings:** Falls back to readable names (MPLAY → "Media Player")

### Volume Format
- **Range:** -80.0 to +18.0 dB (absolute)
- **Display:** 0-98 scale (relative, Denon standard)
- **Precision:** 0.5 dB steps supported
- **Examples:**
  - `MV50` = 50.0 (volume -30.0 dB)
  - `MV505` = 50.5 (volume -29.5 dB)

### Response Parsing
- **Power:** `PWON` / `PWSTANDBY`
- **Volume:** `MV27` (27.0), `MV275` (27.5)
- **Mute:** `MUON` / `MUOFF`
- **Source:** `SIGAME`, `SIDVD`, `SIBD` (→ BLU-RAY)
- **Mode (Full Names):**
  - `MSSTEREO`, `MSDIRECT`
  - `MSDOLBY ATMOS`, `MSDOLBY HD MSTR`, `MSDOLBY HD+DS`
  - `MSDTS:X MSTR`, `MSDTS HD+NEURAL:X`, `MSDTS SURROUND`
- **Signal:** `SDHDMI`, `SDDIGITAL`, `SDANALOG`, `SDARC`
- **Format:** `DCAUTO`, `DCPCM`, `DCDTS`
- **Speakers:** `CVFL 50`, `CVFR 50`, ..., `CVEND`
- **DRC:** `PSDRC OFF`, `PSDRC AUTO`, `PSDRC LOW`, `PSDRC MID`, `PSDRC HI`
- **Restorer:** `PSRSTR OFF`, `PSRSTR LOW`, `PSRSTR MED`, `PSRSTR HI`
- **HDMI Output:** `VSAUDIO AMP`, `VSAUDIO TV`
- **ECO:** `ECOON`, `ECOAUTO`, `ECOOFF`

### Overlay Window
- **Type:** `TYPE_APPLICATION_OVERLAY` (Android 8+)
- **Flags:** Not focusable, not touchable, hardware accelerated
- **Format:** Translucent pixel format
- **Positioning:** Gravity-based (bottom center)

---

## Configuration Examples

### Minimal Coverage Gaming Setup
```
Display Mode: STANDARD
Scale: SMALL
Screen Coverage: ~0.5% (4K TV)
```
Best for: Competitive gaming, movie watching

### Balanced Home Theater
```
Display Mode: INFO
Scale: MEDIUM
Screen Coverage: ~1.5% (4K TV)
```
Best for: General use, casual viewing

### Audiophile Technical Display
```
Display Mode: EXTENDED
Scale: LARGE
Screen Coverage: ~2.5% (4K TV)
```
Best for: Audio enthusiasts, troubleshooting, setup testing

---

## Compatibility

### Tested Devices
- **AVR:** Denon AVR-X1200W (EU model)
- **Platform:** Android TV (Fire TV, Android TV boxes)
- **Resolution:** 1080p, 4K

### Requirements
- Android 6.0+ (Marshmallow)
- Overlay permission (granted via ADB on Fire TV)
- Network access to AVR
- Denon AVR with network control enabled
- Port 23 (Telnet) optional for 0ms lag - HTTP fallback always works!

### Fire TV Setup
Overlay permission must be granted via ADB:
```bash
adb connect FIRETV_IP:5555
adb shell appops set dev.st0nebyte.openosd SYSTEM_ALERT_WINDOW allow
```

---

## Usage Tips

1. **First Time Setup:**
   - Enter AVR IP address
   - Select display mode (start with INFO)
   - Select scale (start with MEDIUM)
   - Grant overlay permission
   - Start OSD service

2. **Adjusting Visibility:**
   - Use SMALL scale for minimal distraction
   - Use STANDARD mode during movies
   - Use EXTENDED mode for troubleshooting

3. **Performance:**
   - Telnet provides instant updates (0ms lag) when available
   - HTTP fallback still very responsive (~500ms)
   - Automatic protocol selection
   - Minimal battery impact

4. **Troubleshooting:**
   - Check AVR IP address is correct
   - Verify overlay permission is granted
   - See connection status and protocol in notification
   - Port 23 unlock optional for better performance (see TELNET.md)

---

## Version History

### v0.5.0 (Current)
- **Automatic protocol detection** (Telnet → HTTP fallback)
- **Custom source names** with auto-sync from receiver
- **Seamless APK updates** (fixed signing key)
- Telnet push updates (0ms lag when available)
- HTTP fallback for maximum compatibility
- Three display modes (STANDARD, INFO, EXTENDED)
- Three scale options (SMALL, MEDIUM, LARGE)
- Complete AVR state tracking
- Modern glassy UI design
- Consistent center-bottom alignment
- Animated volume bar
- Speaker configuration detection
- Split OSD layout (volume bottom, info top-left)
