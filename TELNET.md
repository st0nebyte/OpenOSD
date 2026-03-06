# Telnet Implementation (Feature Branch)

This branch contains a **Telnet-based AVR client** with instant push updates (0ms lag).

## 🚀 Advantages over HTTP Polling

| Feature | HTTP (main) | Telnet (this branch) |
|---------|-------------|----------------------|
| Latency | 150-400ms (polling) | **0ms (push)** |
| Efficiency | Constant polling | Event-driven |
| Updates | Pull (we ask) | **Push (AVR sends)** |
| Network Load | ~5-7 req/s during volume | Minimal |

## ⚠️ Requirements

### 1. Enable Network Control on AVR
- Navigate to: `Setup → Network → Network Control → ON`
- Or perform **hardware reset** (see below)

### 2. Unlock Port 23 (X-Series Receivers)
Many X-series receivers (like AVR-X1200W) have Port 23 **disabled by default**.

**Hardware Reset Method:**
1. On the AVR front panel, press and hold **ZONE2 SOURCE** + **DIMMER** simultaneously
2. Hold for **3+ seconds**
3. Network chip will reset
4. Port 23 should now be open

**Verify Port is Open:**
```bash
nc -zv 192.168.178.130 23
# Should show: Connection to 192.168.178.130 23 port [tcp/telnet] succeeded!
```

### 3. Single Client Limitation
⚠️ **Denon AVRs only allow ONE Telnet connection at a time!**
- If FHEM, Home Assistant, or other software uses Port 23, there will be conflicts
- HTTP mode (main branch) supports unlimited clients

## 🔧 How to Use

### Switch to Telnet Client

In `OSDService.kt`, replace:
```kotlin
import dev.st0nebyte.openosd.AVRClient

// Inside onStartCommand:
client = AVRClient(host, ::onUpdate, ::onConnected).also { it.start() }
```

With:
```kotlin
import dev.st0nebyte.openosd.AVRClientTelnet

// Inside onStartCommand:
client = AVRClientTelnet(host, ::onUpdate, ::onConnected).also { it.start() }
```

That's it! The interface is identical.

## 📡 Protocol Details

### Commands Sent to AVR
```
PW?     → Query power status
MV?     → Query volume
MU?     → Query mute status
MVUP    → Volume up (not implemented yet, but easy to add)
MVDOWN  → Volume down
```

### Push Updates from AVR
The AVR sends **unsolicited updates** when state changes:
```
PWON         → Power turned on
PWSTANDBY    → Power turned off
MV50         → Volume changed to 50 (0-98 scale)
MV355        → Volume changed to 35.5
MUON         → Muted
MUOFF        → Unmuted
```

### Response Format
- All responses end with `\r` (carriage return)
- No delimiters between messages (streaming)
- Commands and responses use same format

## 🐛 Debugging

Enable verbose logging:
```bash
adb logcat | grep AVRClientTelnet
```

You'll see:
```
→ PW?           # Commands sent
← PWON          # Responses received
← MV50          # Push updates
```

## 🔄 Auto-Reconnect

The client automatically reconnects if:
- Initial connection fails
- Connection drops
- Network interruption

Reconnect delay: **5 seconds**

## 📊 Implementation Status

### ✅ Implemented
- TCP Socket connection to Port 23
- Send/receive text commands with `\r` delimiter
- Parse responses: PW (power), MV (volume), MU (mute)
- Push-based updates (instant notifications)
- Auto-reconnect on disconnect
- Convert Denon 0-98 scale to dB (-80 to +18)

### 🔜 Not Yet Implemented (easy to add)
- Source/Input queries (SI?)
- Sound mode queries (MS?)
- Volume up/down commands (MVUP/MVDOWN)
- Keep-alive pings for stability
- Connection timeout handling

## 🔙 Switch Back to HTTP

Simply checkout main branch:
```bash
git checkout main
```

The HTTP-based polling is more compatible but less responsive.

## 📚 References

- [FHEM Denon Module (Perl)](https://github.com/delMar43/FHEM/blob/master/70_DENON_AVR.pm)
- [IP-Symcon Denon Module (PHP)](https://github.com/Wolbolar/IPSymconDenon)
- [FHEM Forum Discussion](https://forum.fhem.de/index.php?topic=58452.345)

---

**Status:** Experimental - Ready for testing once Port 23 is unlocked
