package eu.client.modules.impl.core;

import eu.client.EUClient;
import eu.client.managers.ChatManager;
import eu.client.pingbypass.PingBypassConfig;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PingBypassModule.
 *
 * <p>Tests the server-mode guard (Requirement 2.3) and metadata format (Requirement 2.5).</p>
 */
class PingBypassModuleTest {

    private PingBypassModule module;
    private PingBypassConfig originalConfig;
    private ChatManager originalChatManager;

    @BeforeEach
    void setUp() throws Exception {
        // Save original static field values
        originalConfig = EUClient.PINGBYPASS_CONFIG;
        originalChatManager = EUClient.CHAT_MANAGER;

        // Create the module instance — constructor is safe because toggled=false
        // and EUClient.EVENT_HANDLER is initialized inline
        module = new PingBypassModule();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore original static fields
        setStaticField(EUClient.class, "PINGBYPASS_CONFIG", originalConfig);
        setStaticField(EUClient.class, "CHAT_MANAGER", originalChatManager);
    }

    /**
     * Test that enabling PingBypassModule in server mode refuses to enable
     * and keeps the module toggled off.
     *
     * <p><b>Validates: Requirement 2.3</b></p>
     */
    @Test
    void onEnable_serverMode_refusesToEnable() throws Exception {
        // Set up a PingBypassConfig that reports server mode
        PingBypassConfig serverConfig = mock(PingBypassConfig.class);
        when(serverConfig.isServer()).thenReturn(true);
        setStaticField(EUClient.class, "PINGBYPASS_CONFIG", serverConfig);

        // Set up a mock ChatManager so the error() call doesn't NPE
        ChatManager mockChatManager = mock(ChatManager.class);
        setStaticField(EUClient.class, "CHAT_MANAGER", mockChatManager);

        // Call onEnable directly — it should refuse and call setToggled(false, false)
        module.onEnable();

        // Verify the error message was sent
        verify(mockChatManager).error("Cannot enable PingBypass on a PingBypass server!");

        // The module should remain toggled off
        assertFalse(module.isToggled(), "Module should not be toggled on in server mode");
    }

    /**
     * Test that getMetaData() returns the proxyPing value followed by "ms".
     *
     * <p><b>Validates: Requirement 2.5</b></p>
     */
    @Test
    void getMetaData_returnsFormattedPing() {
        module.proxyPing = 42;
        assertEquals("42ms", module.getMetaData());
    }

    /**
     * Test that getMetaData() returns "0ms" when proxyPing is zero (default).
     *
     * <p><b>Validates: Requirement 2.5</b></p>
     */
    @Test
    void getMetaData_defaultPingIsZero() {
        assertEquals("0ms", module.getMetaData());
    }

    /**
     * Test that getMetaData() handles large ping values correctly.
     *
     * <p><b>Validates: Requirement 2.5</b></p>
     */
    @Test
    void getMetaData_largePingValue() {
        module.proxyPing = 9999;
        assertEquals("9999ms", module.getMetaData());
    }

    /**
     * Sets a static (potentially final) field on a class via reflection.
     */
    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
