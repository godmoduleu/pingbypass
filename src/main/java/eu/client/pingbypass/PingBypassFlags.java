package eu.client.pingbypass;

/**
 * Global flags for PingBypass proxy state.
 * Separated from mixins to avoid mixin field restrictions.
 */
public class PingBypassFlags {
    /**
     * When true, registry loading errors are tolerated (logged as warnings
     * instead of crashing). Set before the proxy sends ReadyS2CPacket,
     * auto-reset after first use.
     */
    public static volatile boolean tolerateRegistryErrors = false;

    /**
     * When true, UnsupportedOperationException from the Netty encoder (Lambda errors)
     * are suppressed instead of disconnecting. Set during PingBypass proxy connection.
     */
    public static volatile boolean suppressEncoderErrors = false;

    /**
     * When true, ALL disconnects are suppressed. Set during the initial proxy
     * connection handshake and cleared once the client is stable in PLAY state.
     */
    public static volatile boolean suppressAllDisconnects = false;

    /**
     * When true, the proxy is actively forwarding packets for a client.
     * The proxy's own movement/input processing should be suppressed.
     */
    public static volatile boolean proxyForwardingActive = false;
}
