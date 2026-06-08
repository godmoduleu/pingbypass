package eu.client.pingbypass;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.Assertions;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Property-based test for PingBypassConfig round-trip serialization.
 *
 * <p><b>Validates: Requirements 1.7</b></p>
 *
 * <p>Feature: pingbypass-proxy, Property 1: Configuration round-trip</p>
 */
class PingBypassConfigPropertyTest {

    private Path tempDir;

    @BeforeProperty
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("pb-config-test");
        // Clear system properties to avoid interference
        System.clearProperty("pb.server");
        System.clearProperty("pb.ip");
        System.clearProperty("pb.port");
        System.clearProperty("pb.password");
    }

    @AfterProperty
    void tearDown() throws Exception {
        // Clear system properties again after test
        System.clearProperty("pb.server");
        System.clearProperty("pb.ip");
        System.clearProperty("pb.port");
        System.clearProperty("pb.password");
        // Clean up temp directory
        if (tempDir != null) {
            Files.walk(tempDir)
                 .sorted(java.util.Comparator.reverseOrder())
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                 });
        }
    }

    @Provide
    Arbitrary<Boolean> serverValues() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<String> ipValues() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('.', ':')
                .ofMinLength(1)
                .ofMaxLength(45);
    }

    @Provide
    Arbitrary<Integer> portValues() {
        return Arbitraries.integers().between(1, 65535);
    }

    @Provide
    Arbitrary<String> passwordValues() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(64)
                .filter(s -> !s.contains("\n") && !s.contains("\r")
                          && !s.contains("\\") && !s.contains("#")
                          && !s.contains("!") && !s.contains("="));
    }

    @Provide
    Arbitrary<String> propertyKeys() {
        return Arbitraries.of("pb.server", "pb.ip", "pb.port", "pb.password");
    }

    @Provide
    Arbitrary<String> propertyValues() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    /**
     * <b>Property 2: System property override</b>
     *
     * <p>For any property key in {pb.server, pb.ip, pb.port, pb.password} and any two
     * distinct valid values A (in file) and B (as system property), calling
     * getProperty(key, default) SHALL return value B (the system property), not value A
     * (the file value).</p>
     *
     * <p><b>Validates: Requirements 1.6</b></p>
     */
    @Property(tries = 100)
    void systemPropertyOverride(
            @ForAll("propertyKeys") String key,
            @ForAll("propertyValues") String fileValue,
            @ForAll("propertyValues") String systemValue
    ) throws Exception {
        Assume.that(!fileValue.equals(systemValue));

        // 1. Create config file with value A
        Path runDir = Files.createTempDirectory(tempDir, "override");
        Path configDir = runDir.resolve("euclient");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("pingbypass.properties");

        Properties fileProps = new Properties();
        fileProps.setProperty(key, fileValue);
        try (OutputStream out = Files.newOutputStream(configFile)) {
            fileProps.store(out, "File config");
        }

        // 2. Set system property to value B
        System.setProperty(key, systemValue);
        try {
            // 3. Load config and verify getProperty returns B (system), not A (file)
            PingBypassConfig config = new PingBypassConfig(runDir);
            config.load();

            String result = config.getProperty(key, "default");
            Assertions.assertEquals(systemValue, result,
                    "getProperty('" + key + "') should return system property value '" + systemValue
                    + "' but got '" + result + "' (file value was '" + fileValue + "')");
            Assertions.assertNotEquals(fileValue, result,
                    "getProperty('" + key + "') should NOT return file value '" + fileValue + "'");
        } finally {
            // 4. Clean up system property
            System.clearProperty(key);
        }
    }

    /**
     * <b>Property 1: Configuration round-trip</b>
     *
     * <p>For any valid PingBypassConfig (with arbitrary boolean pb.server, string pb.ip,
     * integer pb.port in [1, 65535], and string pb.password), serializing the config to a
     * Properties object, writing it to a file, then loading a new PingBypassConfig from
     * that file SHALL produce a config where isServer(), getIp(), getPort(), and
     * getPassword() return equivalent values.</p>
     *
     * <p><b>Validates: Requirements 1.7</b></p>
     */
    @Property(tries = 100)
    void configRoundTrip(
            @ForAll("serverValues") boolean server,
            @ForAll("ipValues") String ip,
            @ForAll("portValues") int port,
            @ForAll("passwordValues") String password
    ) throws Exception {
        // 1. Create a config directory and write initial properties file
        Path runDir = Files.createTempDirectory(tempDir, "run");
        Path configDir = runDir.resolve("euclient");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("pingbypass.properties");

        Properties original = new Properties();
        original.setProperty("pb.server", String.valueOf(server));
        original.setProperty("pb.ip", ip);
        original.setProperty("pb.port", String.valueOf(port));
        original.setProperty("pb.password", password);
        try (OutputStream out = Files.newOutputStream(configFile)) {
            original.store(out, "Test config");
        }

        // 2. Load config from file
        PingBypassConfig config1 = new PingBypassConfig(runDir);
        config1.load();

        // 3. Serialize to Properties via toProperties()
        Properties serialized = config1.toProperties();

        // 4. Write serialized properties to a new file
        Path runDir2 = Files.createTempDirectory(tempDir, "run2");
        Path configDir2 = runDir2.resolve("euclient");
        Files.createDirectories(configDir2);
        Path configFile2 = configDir2.resolve("pingbypass.properties");
        try (OutputStream out = Files.newOutputStream(configFile2)) {
            serialized.store(out, "Serialized config");
        }

        // 5. Load a new config from the serialized file
        PingBypassConfig config2 = new PingBypassConfig(runDir2);
        config2.load();

        // 6. Assert equivalence
        Assertions.assertEquals(config1.isServer(), config2.isServer(),
                "isServer() mismatch after round-trip");
        Assertions.assertEquals(config1.getIp(), config2.getIp(),
                "getIp() mismatch after round-trip");
        Assertions.assertEquals(config1.getPort(), config2.getPort(),
                "getPort() mismatch after round-trip");
        Assertions.assertEquals(config1.getPassword(), config2.getPassword(),
                "getPassword() mismatch after round-trip");
    }
}
