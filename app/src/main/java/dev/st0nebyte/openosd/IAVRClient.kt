package dev.st0nebyte.openosd

/**
 * Common interface for AVR client implementations.
 * Allows switching between HTTP polling and Telnet push modes.
 */
interface IAVRClient {
    fun start()
    fun stop()
}
