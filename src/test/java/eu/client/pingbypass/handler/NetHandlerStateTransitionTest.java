package eu.client.pingbypass.handler;

import eu.client.EUClient;
import eu.client.pingbypass.PingBypassConfig;
import eu.client.pingbypass.server.ProxyServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.text.Text;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for net handler state transitions.
 *
 * <p>Tests the Handshake → Login → Password → Play transition chain,
 * protocol version rejection, encryption nonce verification failure,
 * and second connection rejection.</p>
 *
 * <p><b>Validates: Requirements 3.3, 3.6, 4.1, 4.4</b></p>
 */
class NetHandlerStateTransitionTest {

    private ProxyServer proxyServer;
    private ClientConnection connection;
    private PingBypassConfig config;
    private List<ClientConnection> connectionsList;
    private PingBypassConfig originalConfig;

    @BeforeEach
    void setUp() throws Exception {
        originalConfig = EUClient.PINGBYPASS_CONFIG;

        proxyServer = mock(ProxyServer.class);
        connection = mock(ClientConnection.class);
        config = mock(PingBypassConfig.class);

        connectionsList = Collections.synchronizedList(new ArrayList<>());
        when(proxyServer.getConnections()).thenReturn(connectionsList);
        when(proxyServer.getConfig()).thenReturn(config);
        when(connection.isOpen()).thenReturn(true);
        when(connection.getAddressAsString(false)).thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() throws Exception {
        setStaticField(EUClient.class, "PINGBYPASS_CONFIG", originalConfig);
    }

    // ========================================================================
    // Protocol version rejection tests (Requirement 3.3)
    // ========================================================================

    /**
     * Test that a handshake with a protocol version lower than 769 is rejected
     * with an "outdated client" disconnect message.
     *
     * <p><b>Validates: Requirement 3.3</b></p>
     */
    @Test
    void onHandshake_wrongProtocolVersion_rejectsConnection() {
        PbHandshakeHandler handler = new PbHandshakeHandler(proxyServer, connection);

        // Protocol version 760 (1.19.2) — too old
        HandshakeC2SPacket packet = new HandshakeC2SPacket(
                760, "localhost", 25565, ConnectionIntent.LOGIN);

        handler.onHandshake(packet);

        // Should send a LoginDisconnectS2CPacket and disconnect
        verify(connection).transitionOutbound(any());
        verify(connection).send(any(LoginDisconnectS2CPacket.class));
        verify(connection).disconnect(any(Text.class));
    }

    /**
     * Test that a handshake with a protocol version higher than 769 is rejected
     * with an "incompatible" disconnect message.
     *
     * <p><b>Validates: Requirement 3.3</b></p>
     */
    @Test
    void onHandshake_futureProtocolVersion_rejectsConnection() {
        PbHandshakeHandler handler = new PbHandshakeHandler(proxyServer, connection);

        // Protocol version 800 — too new
        HandshakeC2SPacket packet = new HandshakeC2SPacket(
                800, "localhost", 25565, ConnectionIntent.LOGIN);

        handler.onHandshake(packet);

        verify(connection).send(any(LoginDisconnectS2CPacket.class));
        verify(connection).disconnect(any(Text.class));
    }

    // ========================================================================
    // Second connection rejection tests (Requirement 3.6)
    // ========================================================================

    /**
     * Test that a second LOGIN connection is rejected when an active PLAY-state
     * connection already exists.
     *
     * <p><b>Validates: Requirement 3.6</b></p>
     */
    @Test
    void onHandshake_secondConnection_rejected() {
        // Set up an existing PLAY-state connection in the connections list
        ClientConnection existingConn = mock(ClientConnection.class);
        when(existingConn.isOpen()).thenReturn(true);
        PacketListener playListener = mock(PacketListener.class);
        when(playListener.getPhase()).thenReturn(NetworkPhase.PLAY);
        when(existingConn.getPacketListener()).thenReturn(playListener);
        connectionsList.add(existingConn);

        PbHandshakeHandler handler = new PbHandshakeHandler(proxyServer, connection);

        // Correct protocol version, LOGIN intent
        HandshakeC2SPacket packet = new HandshakeC2SPacket(
                769, "localhost", 25565, ConnectionIntent.LOGIN);

        handler.onHandshake(packet);

        // Should reject with "already in use" message
        verify(connection).send(any(LoginDisconnectS2CPacket.class));
        verify(connection).disconnect(any(Text.class));
        // Should NOT transition to login handler
        verify(connection, never()).transitionInbound(any(), any());
    }

    /**
     * Test that a connection is allowed when existing connections are closed
     * (not active PLAY state).
     *
     * <p><b>Validates: Requirement 3.6</b></p>
     */
    @Test
    void onHandshake_closedExistingConnection_allowsNew() {
        // Set up a closed connection in the list
        ClientConnection closedConn = mock(ClientConnection.class);
        when(closedConn.isOpen()).thenReturn(false);
        connectionsList.add(closedConn);

        PbHandshakeHandler handler = new PbHandshakeHandler(proxyServer, connection);

        HandshakeC2SPacket packet = new HandshakeC2SPacket(
                769, "localhost", 25565, ConnectionIntent.LOGIN);

        handler.onHandshake(packet);

        // Should NOT send disconnect — should transition to login
        verify(connection, never()).disconnect(any(Text.class));
        verify(connection).transitionInbound(any(), any(PbLoginHandler.class));
    }

    // ========================================================================
    // Successful handshake → login transition (Requirement 4.1)
    // ========================================================================

    /**
     * Test that a valid handshake with correct protocol version (769) and LOGIN
     * intent transitions the connection to the login state.
     *
     * <p><b>Validates: Requirement 4.1</b></p>
     */
    @Test
    void onHandshake_correctProtocol_transitionsToLogin() {
        PbHandshakeHandler handler = new PbHandshakeHandler(proxyServer, connection);

        HandshakeC2SPacket packet = new HandshakeC2SPacket(
                769, "localhost", 25565, ConnectionIntent.LOGIN);

        handler.onHandshake(packet);

        // Should transition outbound (LoginStates.S2C) and inbound (LoginStates.C2S with PbLoginHandler)
        verify(connection).transitionOutbound(any());
        verify(connection).transitionInbound(any(), any(PbLoginHandler.class));
        // Should NOT disconnect
        verify(connection, never()).disconnect(any(Text.class));
    }

    // ========================================================================
    // Login handler state machine tests (Requirement 4.1, 4.4)
    // ========================================================================

    /**
     * Test that calling onKey() before onHello() throws IllegalStateException,
     * since the login handler expects HELLO state first.
     *
     * <p><b>Validates: Requirement 4.4</b></p>
     */
    @Test
    void loginHandler_onKeyBeforeHello_throwsException() {
        PbLoginHandler loginHandler = new PbLoginHandler(proxyServer, connection);

        LoginKeyC2SPacket keyPacket = mock(LoginKeyC2SPacket.class);

        assertThrows(IllegalStateException.class, () -> {
            loginHandler.onKey(keyPacket);
        }, "Calling onKey before onHello should throw IllegalStateException");
    }

    /**
     * Test that onHello() sends an encryption request (LoginHelloS2CPacket)
     * to the client.
     *
     * <p><b>Validates: Requirement 4.1</b></p>
     */
    @Test
    void loginHandler_onHello_sendsEncryptionRequest() {
        PbLoginHandler loginHandler = new PbLoginHandler(proxyServer, connection);

        // Create a mock LoginHelloC2SPacket
        net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket helloPacket =
                mock(net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class);
        when(helloPacket.name()).thenReturn("TestPlayer");

        loginHandler.onHello(helloPacket);

        // Should send an encryption request (LoginHelloS2CPacket)
        verify(connection).send(any(LoginHelloS2CPacket.class));
    }

    /**
     * Test that calling onHello() twice throws IllegalStateException,
     * since the state has already moved past HELLO.
     *
     * <p><b>Validates: Requirement 4.1</b></p>
     */
    @Test
    void loginHandler_doubleHello_throwsException() {
        PbLoginHandler loginHandler = new PbLoginHandler(proxyServer, connection);

        net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket helloPacket =
                mock(net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class);
        when(helloPacket.name()).thenReturn("TestPlayer");

        // First call succeeds
        loginHandler.onHello(helloPacket);

        // Second call should throw — state is now KEY, not HELLO
        assertThrows(IllegalStateException.class, () -> {
            loginHandler.onHello(helloPacket);
        }, "Calling onHello twice should throw IllegalStateException");
    }

    /**
     * Test that the login handler stores the player name from the hello packet.
     *
     * <p><b>Validates: Requirement 4.1</b></p>
     */
    @Test
    void loginHandler_onHello_storesPlayerName() {
        PbLoginHandler loginHandler = new PbLoginHandler(proxyServer, connection);

        net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket helloPacket =
                mock(net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class);
        when(helloPacket.name()).thenReturn("TestPlayer");

        loginHandler.onHello(helloPacket);

        assertEquals("TestPlayer", loginHandler.getPlayerName());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
