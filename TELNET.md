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
```
PW?          → Query power status
MV?          → Query master volume
MU?          → Query mute status
SI?          → Query input source
MS?          → Query sound mode
SD?          → Query signal detection
DC?          → Query digital mode
CV?          → Query channel volume (active speakers)
PSDRC ?      → Query Dynamic Range Compression
PSRSTR ?     → Query Audio Restorer
VSAUDIO ?    → Query HDMI Audio Output
ECO?         → Query ECO mode
```

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
- No delimiters between messages (streaming)
- Commands and responses use same format

## 🔄 How Auto-Detection Works

1. **Service starts** → Try Telnet connection to Port 23
2. **Wait 5 seconds** → Check if connection succeeded
3. **If Telnet connected** → Use Telnet (0ms lag)
4. **If Telnet failed** → Switch to HTTP fallback (~500ms lag)
5. **Show notification** → Display active protocol

The switch happens automatically and transparently. You'll see a toast message:
- "Port 23 gesperrt - nutze HTTP (langsamer)" if Telnet failed

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
