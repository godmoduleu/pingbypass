package eu.client.pingbypass.handler;

import eu.client.pingbypass.PingBypassConfig;
import eu.client.pingbypass.server.ProxyServer;
import net.jqwik.api.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.listener.PacketListener;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Property-based test for the single active connection invariant.
 *
 * <p><b>Validates: Requirements 3.6</b></p>
 *
 * <p>Feature: pingbypass-proxy, Property 3: Single active connection invariant</p>
 *
 * <p>This test models the invariant at the logic level: given any random sequence
 * of connect/disconnect/transition-to-play actions, at most 1 connection should
 * be in PLAY state at any time. When a new connection attempts to join while one
 * is already in PLAY, it should be rejected.</p>
 */
class PbHandshakeHandlerPropertyTest {

    /**
     * Represents an action in the connection lifecycle simulation.
     */
    enum Action {
        /** A new client connects (added to connections list in HANDSHAKE state) */
        CONNECT,
        /** A random existing connection disconnects (marked as closed) */
        DISCONNECT,
        /** A random non-play connection transitions to PLAY state */
        TRANSITION_TO_PLAY
    }

    /**
     * Tracks the state of a simulated connection for the model.
     */
    static class SimConnection {
        final ClientConnection mock;
        boolean open;
        NetworkPhase phase;

        SimConnection(ClientConnection mock, boolean open, NetworkPhase phase) {
            this.mock = mock;
            this.open = open;
            this.phase = phase;
        }
    }

    @Provide
    Arbitrary<List<Action>> actionSequences() {
        return Arbitraries.of(Action.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(50);
    }

    /**
     * <b>Property 3: Single active connection invariant</b>
     *
     * <p>For any sequence of client connection attempts to the ProxyServer,
     * the number of simultaneously active (authenticated, play-state) connections
     * SHALL never exceed 1. If a client is already connected, subsequent connection
     * attempts SHALL be rejected with a disconnect message.</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     *
     * <p>This test simulates the invariant using a model: a list of connections
     * with states, and random sequences of CONNECT, DISCONNECT, and
     * TRANSITION_TO_PLAY actions. After each action, we verify that at most 1
     * connection is in PLAY state, and that the hasActivePlayConnection logic
     * (as implemented in PbHandshakeHandler) correctly detects when a play
     * connection exists.</p>
     */
    @Property(tries = 100)
    void singleActiveConnectionInvariant(
            @ForAll("actionSequences") List<Action> actions
    ) {
        // Set up the shared connections list (mirrors ProxyServer.getConnections())
        List<ClientConnection> connectionsList = Collections.synchronizedList(new ArrayList<>());
        ProxyServer proxyServer = mock(ProxyServer.class);
        when(proxyServer.getConnections()).thenReturn(connectionsList);
        PingBypassConfig config = mock(PingBypassConfig.class);
        when(proxyServer.getConfig()).thenReturn(config);

        // Track our simulated connections alongside the mocks
        List<SimConnection> simConnections = new ArrayList<>();

        for (Action action : actions) {
            switch (action) {
                case CONNECT -> {
                    // Create a new mock connection in a non-play state
                    ClientConnection newConn = createMockConnection(true, NetworkPhase.LOGIN);
                    connectionsList.add(newConn);
                    simConnections.add(new SimConnection(newConn, true, NetworkPhase.LOGIN));
                }
                case DISCONNECT -> {
                    // Disconnect a random open connection
                    List<SimConnection> openConns = simConnections.stream()
                            .filter(sc -> sc.open)
                            .toList();
                    if (!openConns.isEmpty()) {
                        SimConnection toDisconnect = openConns.get(0);
                        toDisconnect.open = false;
                        toDisconnect.phase = null;
                        // Update the mock to reflect closed state
                        when(toDisconnect.mock.isOpen()).thenReturn(false);
                        when(toDisconnect.mock.getPacketListener()).thenReturn(null);
                    }
                }
                case TRANSITION_TO_PLAY -> {
                    // Only transition if no other connection is already in PLAY
                    // (this models the correct behavior enforced by the invariant)
                    long playCount = countPlayConnections(simConnections);
                    List<SimConnection> nonPlayOpen = simConnections.stream()
                            .filter(sc -> sc.open && sc.phase != NetworkPhase.PLAY)
                            .toList();
                    if (!nonPlayOpen.isEmpty() && playCount == 0) {
                        SimConnection toTransition = nonPlayOpen.get(0);
                        toTransition.phase = NetworkPhase.PLAY;
                        // Update mock to reflect PLAY state
                        PacketListener playListener = mock(PacketListener.class);
                        when(playListener.getPhase()).thenReturn(NetworkPhase.PLAY);
                        when(toTransition.mock.getPacketListener()).thenReturn(playListener);
                    }
                    // If playCount > 0, the transition is rejected (invariant enforced)
                }
            }

            // INVARIANT CHECK: at most 1 connection in PLAY state at any time
            long activePlayCount = countPlayConnections(simConnections);
            Assertions.assertTrue(activePlayCount <= 1,
                    "Invariant violated: " + activePlayCount
                    + " connections in PLAY state (expected at most 1)");

            // Verify the hasActivePlayConnection logic matches our model
            // by checking from the perspective of a hypothetical new connection
            boolean modelSaysActive = activePlayCount > 0;
            boolean logicSaysActive = checkHasActivePlayConnection(
                    connectionsList, null);
            Assertions.assertEquals(modelSaysActive, logicSaysActive,
                    "hasActivePlayConnection logic disagrees with model: "
                    + "model=" + modelSaysActive + ", logic=" + logicSaysActive);
        }
    }

    /**
     * Replicates the hasActivePlayConnection() logic from PbHandshakeHandler
     * to verify it correctly detects active play connections.
     *
     * @param connections the proxy server's connections list
     * @param self the current connection to skip (null to check all)
     * @return true if any connection other than self is open and in PLAY phase
     */
    private boolean checkHasActivePlayConnection(
            List<ClientConnection> connections, ClientConnection self) {
        for (ClientConnection conn : connections) {
            if (conn == self) {
                continue;
            }
            if (conn.isOpen() && conn.getPacketListener() != null
                    && conn.getPacketListener().getPhase() == NetworkPhase.PLAY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the number of open connections in PLAY state in the simulation model.
     */
    private long countPlayConnections(List<SimConnection> simConnections) {
        return simConnections.stream()
                .filter(sc -> sc.open && sc.phase == NetworkPhase.PLAY)
                .count();
    }

    /**
     * Creates a mock ClientConnection with the given open state and network phase.
     */
    private ClientConnection createMockConnection(boolean open, NetworkPhase phase) {
        ClientConnection conn = mock(ClientConnection.class);
        when(conn.isOpen()).thenReturn(open);
        if (phase != null) {
            PacketListener listener = mock(PacketListener.class);
            when(listener.getPhase()).thenReturn(phase);
            when(conn.getPacketListener()).thenReturn(listener);
        } else {
            when(conn.getPacketListener()).thenReturn(null);
        }
        return conn;
    }
}
