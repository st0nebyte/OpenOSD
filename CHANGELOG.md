# Changelog

All notable changes to OpenOSD will be documented in this file.

## [0.8.0] - 2026-03-16

### ✨ New Features
- Audio format names now displayed in full
  - Decoder names expanded (e.g., "Dolby Digital Plus" instead of "DOLBY D+")
  - Upmixer names expanded (e.g., "Neural:X", "Dolby Surround")
  - Example: "DOLBY D+ +DS" → "Dolby Digital Plus + Dolby Surround"

### 🎯 Supported Audio Formats
- **Dolby:** Atmos, TrueHD, Digital Plus, Digital, Surround
- **DTS:** DTS:X Master, DTS-HD Master Audio, DTS-ES Matrix/Discrete, DTS 96/24
- **Upmixers:** Neural:X, Dolby Surround
- **Multi-Channel:** 7.1, Stereo, Multi-Channel In
- **Sound Fields:** Rock Arena, Jazz Club, Video Game, Mono Movie, Matrix, Virtual

### ✅ Testing
- Added AudioFormatMapper with comprehensive unit tests
- 11 new test cases covering all audio format mappings

### 📦 Build
- Version code: 12
- Version name: 0.8.0

## [0.7.0] - 2026-03-16

### 🐛 Bug Fixes
- Fixed OSD getting stuck in mute status when mute state becomes out of sync
  - Added 30s max timeout for persist mode to prevent infinite display
- Fixed memory leak in channel volume tracking (activeSpeakers list)
  - Now properly cleared on reconnect
- Fixed memory leaks in OSDService
  - Timer callbacks now properly canceled on service destroy
  - Fallback timer (Telnet→HTTP) now properly managed
- Fixed memory leak in MainActivity
  - CoroutineScope now properly canceled on activity destroy
- Fixed potential memory leak in OSDView
  - ValueAnimator now properly nulled before reassignment
- Fixed resource leak in SourceNameSync
  - HttpURLConnection now properly disconnected in finally block

### ✅ Testing
- Verified all telnet query commands (90.4% success rate)
- Added comprehensive unit tests for mute synchronization bug
- All existing unit tests pass

### 📦 Build
- Version code: 11
- Version name: 0.7.0
- Release APK size: ~1.0 MB
- Debug APK size: ~1.4 MB

## [0.6.0] - Previous Release

Initial release with:
- Telnet protocol support (0ms lag)
- HTTP fallback (500ms lag)
- Auto-protocol detection
- Glassmorphism OSD design
- Configurable source names
- Multiple display modes (Standard, Info, Extended)
- Scalable UI (Small, Medium, Large)
