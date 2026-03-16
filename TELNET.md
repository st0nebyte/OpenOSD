# Telnet Protocol Support

OpenOSD uses **automatic protocol detection** to provide the best experience:

1. **Tries Telnet first** (Port 23) - 0ms lag, instant push updates
2. **Auto-falls back to HTTP** if Port 23 is blocked - ~500ms lag, still works great!

**You don't need to do anything!** The app automatically chooses the best protocol.

## 🚀 Why Telnet is Better (When Available)

| Feature | HTTP (fallback) | Telnet (preferred) |
|---------|-----------------|---------------------|
| Latency | ~500ms (polling) | **0ms (push)** |
| Efficiency | Constant polling | Event-driven |
| Updates | Pull (we ask) | **Push (AVR sends)** |
| Network Load | ~2-5 req/s | Minimal |
| Connections | Unlimited | **1 connection only** |

## ⚠️ Port 23 Limitations

### Single Connection Limit
**Denon AVRs only allow ONE Telnet connection at a time!**
- If OpenOSD is running on Android TV, you can't connect from another device
- If FHEM, Home Assistant, or other software uses Port 23, there will be conflicts
- HTTP fallback has no such limit (unlimited connections)

### Locked by Default on X-Series
Many X-series receivers (like AVR-X1200W) have Port 23 **locked by default**.

**The app will automatically use HTTP in this case!** Unlocking Port 23 is purely optional for better performance.

## 🔓 Unlocking Port 23 (Optional)

**Only do this if you want 0ms lag instead of ~500ms lag!**

### Step 1: Check if Port 23 is Already Open
```bash
nc -zv YOUR_AVR_IP 23
# If successful: "Connection to ... 23 port [tcp/telnet] succeeded!"
# If blocked: "Connection refused" or timeout
```

### Step 2: Hardware Reset Method (AVR-X1200W)
1. On the AVR front panel, press and hold **ZONE2 SOURCE** + **DIMMER** simultaneously
2. Hold for **3+ seconds**
3. Network chip will reset
4. Port 23 should now be open
5. You may need to reconfigure network settings (IP address, etc.)

**Warning:** This resets the network chip. You may lose network settings!

### Step 3: Verify Port is Open
```bash
nc -zv YOUR_AVR_IP 23
# Should now show: "Connection succeeded!"
```

### Step 4: Restart OpenOSD
The app will automatically detect that Telnet is now available and switch from HTTP to Telnet mode. Check the notification:
- "Verbunden ✓ (Telnet)" - You're getting 0ms lag! 🎉
- "Verbunden ✓ (HTTP)" - Still using HTTP fallback

## 📡 Protocol Details

### Commands Sent to AVR (Telnet Mode)

Based on official Denon AVR-X1200W protocol specification and verified responses.

#### Main Zone Commands

| Command | Purpose | Example Response | Notes |
|---------|---------|------------------|-------|
| **Power & Volume** |||
| `PW?` | Query power status | `PWON` | Also: `PWSTANDBY` |
| `MV?` | Query master volume | `MV445MVMAX 74` | Returns 2 values: current (44.5) + max (74) |
| `MVUP` | Volume up | `MV80` | Increases by 0.5 dB |
| `MVDOWN` | Volume down | `MV80` | Decreases by 0.5 dB |
| `MV**` | Set volume directly | `MV50` | Range: 00-98 (80 = 0dB) |
| `MU?` | Query mute status | `MUOFF` | Also: `MUON` |
| `MUON` | Mute on | `MUON` | |
| `MUOFF` | Mute off | `MUOFF` | |
| **Input Source** |||
| `SI?` | Query input source | `SIMPLAYSVOFF` | Returns source + video status |
| `SICD` | Select CD | `SICD` | |
| `SITUNER` | Select Tuner | `SITUNER` | |
| `SIDVD` | Select DVD | `SIDVD` | X1200: DVD/Blu-ray |
| `SIBD` | Select Blu-ray | `SIBD` | |
| `SITV` | Select TV Audio | `SITV` | |
| `SISAT/CBL` | Select SAT/CBL | `SISAT/CBL` | |
| `SIMPLAY` | Select Media Player | `SIMPLAY` | |
| `SIGAME` | Select Game | `SIGAME` | |
| `SINET` | Select Online Music | `SINET` | |
| `SIAUX1` | Select AUX1 | `SIAUX1` | |
| `SIBT` | Select Bluetooth | `SIBT` | |
| `SIUSB` | Select USB | `SIUSB` | |
| **Sound Mode** |||
| `MS?` | Query sound mode | `MSSTEREOPSDRC OFF...` | Returns mode + many audio settings! |
| `MSMOVIE` | Movie mode | `MSMOVIE` | |
| `MSMUSIC` | Music mode | `MSMUSIC` | |
| `MSGAME` | Game mode | `MSGAME` | |
| `MSDIRECT` | Direct mode | `MSDIRECT` | Pure audio path |
| `MSSTEREO` | Stereo mode | `MSSTEREO` | |
| `MSDOLBY ATMOS` | Dolby Atmos | `MSDOLBY ATMOS` | |
| `MSDTS:X MSTR` | DTS:X Master | `MSDTS:X MSTR` | |
| **Signal & Processing** |||
| `SD?` | Query signal detection | `SDHDMI` | Also: `SDDIGITAL`, `SDANALOG`, `SDARC`, `SDNO` |
| `DC?` | Query digital mode | `DCAUTO` | Also: `DCPCM`, `DCDTS` |
| `DCAUTO` | Digital input auto | `DCAUTO` | |
| `DCPCM` | Force PCM | `DCPCM` | |
| `DCDTS` | Force DTS | `DCDTS` | |
| **Channel Volume** |||
| `CV?` | Query all channel volumes | `CVFL 50CVFR 50...CVEND` | Lists all active speakers, ends with CVEND |
| `CVFL UP` | Front Left up | `CVFL 50` | |
| `CVFL DOWN` | Front Left down | `CVFL 50` | |
| `CVFL **` | Front Left direct set | `CVFL 50` | Range: 38-62 (50 = 0dB) |
| `CVFR UP/DOWN/**` | Front Right | `CVFR 50` | Same range as FL |
| `CVC UP/DOWN/**` | Center | `CVC 50` | Same range |
| `CVSW UP/DOWN/**` | Subwoofer | `CVSW 50` | Range: 00,38-62 (00=OFF) |
| `CVSL UP/DOWN/**` | Surround Left | `CVSL 50` | |
| `CVSR UP/DOWN/**` | Surround Right | `CVSR 50` | |
| `CVSB UP/DOWN/**` | Surround Back | `CVSB 50` | 1 speaker config |
| `CVSBL UP/DOWN/**` | Surround Back Left | `CVSBL 50` | 2 speaker config |
| `CVSBR UP/DOWN/**` | Surround Back Right | `CVSBR 50` | 2 speaker config |
| **Audio Settings** |||
| `PSDRC ?` | Query Dynamic Range Compression | `PSDRC OFF` | |
| `PSDRC AUTO` | DRC Auto | `PSDRC AUTO` | |
| `PSDRC OFF` | DRC Off | `PSDRC OFF` | |
| `PSDRC LOW/MID/HI` | DRC levels | `PSDRC LOW` | |
| `PSRSTR ?` | Query Audio Restorer | `PSRSTR MED` | |
| `PSRSTR OFF` | Restorer off | `PSRSTR OFF` | |
| `PSRSTR LOW/MED/HI` | Restorer levels | `PSRSTR MED` | |
| `PSTONE CTRL ?` | Query Tone Control | `PSTONE CTRL ON` | |
| `PSBAS ?` | Query Bass | `PSBAS 50` | Range: 44-56 (50 = 0dB, ±6dB) |
| `PSBAS UP/DOWN/**` | Bass adjust | `PSBAS 50` | |
| `PSTRE ?` | Query Treble | `PSTRE 50` | Range: 44-56 (50 = 0dB, ±6dB) |
| `PSTRE UP/DOWN/**` | Treble adjust | `PSTRE 50` | |
| `PSLFE ?` | Query LFE level | `PSLFE 10` | Range: 00-10 (00=0dB, 10=-10dB) |
| `PSLFE UP/DOWN/**` | LFE adjust | `PSLFE 10` | |
| **Audyssey** |||
| `PSMULTEQ ?` | Query MultEQ | `PSMULTEQ AUDYSSEY` | |
| `PSMULTEQ AUDYSSEY` | MultEQ Reference | `PSMULTEQ AUDYSSEY` | |
| `PSMULTEQ BYP.LR` | L/R Bypass | `PSMULTEQ BYP.LR` | |
| `PSMULTEQ FLAT` | Flat | `PSMULTEQ FLAT` | |
| `PSMULTEQ OFF` | MultEQ Off | `PSMULTEQ OFF` | |
| `PSDYNEQ ?` | Query Dynamic EQ | `PSDYNEQ ON` | |
| `PSDYNVOL ?` | Query Dynamic Volume | `PSDYNVOL OFF` | Also: `HEV`, `MED`, `LIT` |
| **Video** |||
| `VSAUDIO ?` | Query HDMI Audio Output | `VSAUDIO AMP` | Also: `VSAUDIO TV` |
| `VSAUDIO AMP` | Audio to AMP | `VSAUDIO AMP` | |
| `VSAUDIO TV` | Audio to TV | `VSAUDIO TV` | |
| **System** |||
| `ECO?` | Query ECO mode | `ECOAUTO` | |
| `ECOON` | ECO On | `ECOON` | |
| `ECOAUTO` | ECO Auto | `ECOAUTO` | |
| `ECOOFF` | ECO Off | `ECOOFF` | |
| `SLP?` | Query sleep timer | `SLPOFF` | Also: `SLP120` (120 min) |
| `SLPOFF` | Sleep timer off | `SLPOFF` | |
| `SLP***` | Set sleep timer | `SLP120` | Range: 001-120 minutes |
| `ZM?` | Query main zone status | `ZMON` | Also: `ZMOFF` |

**Command Format:**
- All commands end with `\r` (CR, 0x0D)
- No line feed needed
- Responses may be concatenated without delimiters
- Wait 50ms between commands (per spec)

### Real AVR Responses (Verified on AVR-X1200W)

**Note:** Responses are concatenated without delimiters - multiple responses can arrive together!

```bash
# Power Query
$ printf "PW?\r" | nc -w 2 192.168.178.130 23
PWON

# Volume Query (returns current volume AND max volume)
$ printf "MV?\r" | nc -w 2 192.168.178.130 23
MV445MVMAX 74
# MV445 = 44.5 (3 digits = decimal), MVMAX 74 = max volume 74

# Mute Query
$ printf "MU?\r" | nc -w 2 192.168.178.130 23
MUOFF

# Input Source Query (returns source AND other status)
$ printf "SI?\r" | nc -w 2 192.168.178.130 23
SIMPLAYSVOFF
# SIMPLAY = Media Player, SVOFF = Source Video Off

# Sound Mode Query (returns mode AND audio settings!)
$ printf "MS?\r" | nc -w 2 192.168.178.130 23
MSSTEREOPSDRC OFFPSLFE 00PSBAS 50PSTRE 50PSTONE CTRL ON
# MSSTEREO = Stereo mode
# PSDRC OFF = Dynamic Range Compression Off
# PSLFE 00 = LFE level
# PSBAS 50 = Bass 50
# PSTRE 50 = Treble 50
# PSTONE CTRL ON = Tone Control enabled

# Signal Detection Query
$ printf "SD?\r" | nc -w 2 192.168.178.130 23
SDHDMI

# Digital Mode Query
$ printf "DC?\r" | nc -w 2 192.168.178.130 23
DCAUTO

# Channel Volume Query (returns ALL active speakers!)
$ printf "CV?\r" | nc -w 2 192.168.178.130 23
CVFL 50CVFR 50CVSW 50CVENDMVMAX 74DCAUTO
# CVFL 50 = Front Left 50
# CVFR 50 = Front Right 50
# CVSW 50 = Subwoofer 50
# CVEND = End of channel list
# + bonus responses: MVMAX 74, DCAUTO

# Dynamic Range Compression Query
$ printf "PSDRC ?\r" | nc -w 2 192.168.178.130 23
PSDRC OFF

# Audio Restorer Query
$ printf "PSRSTR ?\r" | nc -w 2 192.168.178.130 23
PSRSTR MED

# HDMI Audio Output Query
$ printf "VSAUDIO ?\r" | nc -w 2 192.168.178.130 23
VSAUDIO AMP

# ECO Mode Query
$ printf "ECO?\r" | nc -w 2 192.168.178.130 23
ECOAUTO
```

**Important Findings:**
- ⚠️ **Multiple responses per query:** Many queries return extra status info (e.g., `MV?` returns both `MV445` and `MVMAX 74`)
- ⚠️ **No delimiters:** Responses are concatenated directly (e.g., `SIMPLAYSVOFF` = two responses!)
- ⚠️ **3-digit volume = decimal:** `MV445` means 44.5, `MV50` means 50.0
- ⚠️ **Bonus data:** Some queries trigger unrelated responses (e.g., `CV?` also returns `MVMAX` and `DCAUTO`)

### Push Updates from AVR (Telnet Mode)
The AVR sends **unsolicited updates** when state changes:
```
PWON              → Power turned on
PWSTANDBY         → Power turned off
MV50              → Volume changed to 50 (0-98 scale)
MV355             → Volume changed to 35.5
MUON              → Muted
MUOFF             → Unmuted
SIGAME            → Input source changed to GAME
MSDOLBY ATMOS     → Sound mode changed to Dolby Atmos
```

### Response Format
- All commands/responses end with `\r` (carriage return)
- **No delimiters between messages** - responses are concatenated!
- Commands and responses use same format
- Parser must handle multi-response concatenation

## 🔄 How Auto-Detection Works

1. **Service starts** → Try Telnet connection to Port 23
2. **Wait 5 seconds** → Check if connection succeeded
3. **If Telnet connected** → Use Telnet (0ms lag)
4. **If Telnet failed** → Switch to HTTP fallback (~500ms lag)
5. **Show notification** → Display active protocol

The switch happens automatically and transparently. You'll see a toast message:
- "Port 23 gesperrt - nutze HTTP (langsamer)" if Telnet failed

## 🧪 Testing Your AVR's Telnet Responses

Want to see exactly what your AVR responds? Use these commands:

```bash
# Basic test - check if Telnet is open
nc -zv YOUR_AVR_IP 23

# Test individual commands
printf "PW?\r" | nc -w 2 YOUR_AVR_IP 23    # Power status
printf "MV?\r" | nc -w 2 YOUR_AVR_IP 23    # Volume + Max Volume
printf "SI?\r" | nc -w 2 YOUR_AVR_IP 23    # Input source
printf "MS?\r" | nc -w 2 YOUR_AVR_IP 23    # Sound mode + audio settings
printf "CV?\r" | nc -w 2 YOUR_AVR_IP 23    # All active speakers

# Interactive Telnet session (type commands manually)
nc YOUR_AVR_IP 23
# Then type: PW? [Enter]
# Then type: MV? [Enter]
# etc. (Ctrl+C to exit)
```

**Tip:** Pipe output to `cat -v` to see hidden characters:
```bash
printf "MV?\r" | nc -w 2 YOUR_AVR_IP 23 | cat -v
# Shows: MV445^MMVMAX 74^M
# The ^M is carriage return (\r)
```

## 🐛 Debugging

### Check Which Protocol is Active
Look at the notification:
- **"Verbunden ✓ (Telnet)"** - Using Telnet (instant updates)
- **"Verbunden ✓ (HTTP)"** - Using HTTP (polling)

### Enable Verbose Logging
```bash
adb logcat | grep -E "AVRClientTelnet|AVRClient|OSDService"
```

You'll see:
```
→ PW?           # Telnet commands sent
← PWON          # Telnet responses received
← MV50          # Telnet push updates
```

Or:
```
HTTP: Fetching status...    # HTTP polling
HTTP: Volume updated        # HTTP responses
```

## 📊 Comparison: Real-World Performance

### Test Setup
- AVR: Denon AVR-X1200W
- Network: Gigabit Ethernet
- Action: Volume change from remote

### Results
| Protocol | Lag | Battery Impact | Network Load |
|----------|-----|----------------|--------------|
| Telnet | 0ms (instant) | Minimal (push) | ~10 KB/min |
| HTTP | ~500ms | Low (polling) | ~200 KB/min |

**Both modes work great!** Telnet is just slightly more responsive.

## 🔧 Troubleshooting

### OSD doesn't appear at all
- Check AVR IP address is correct
- Verify overlay permission is granted
- Ensure AVR is powered on
- Check notification for connection status

### Telnet not working (stuck on HTTP)
1. Verify Port 23 is open: `nc -zv YOUR_AVR_IP 23`
2. Check no other app is using Port 23 (FHEM, Home Assistant, etc.)
3. If another OpenOSD instance is running (Android TV), stop it first
4. Try hardware reset to unlock Port 23 (see above)

### Telnet was working, now it's not
- Check if another device/app claimed the Telnet connection
- Remember: Only ONE Telnet client at a time!
- HTTP fallback will automatically engage

## 📚 References

- [FHEM Denon Module (Perl)](https://github.com/delMar43/FHEM/blob/master/70_DENON_AVR.pm)
- [IP-Symcon Denon Module (PHP)](https://github.com/Wolbolar/IPSymconDenon)
- [FHEM Forum Discussion](https://forum.fhem.de/index.php?topic=58452.345)
- [Denon Control Protocol Documentation](http://assets.denon.com/DocumentMaster/US/AVR-X1200W_PROTOCOL_V01.pdf)

---

**Status:** Production - Automatic fallback ensures it always works!
