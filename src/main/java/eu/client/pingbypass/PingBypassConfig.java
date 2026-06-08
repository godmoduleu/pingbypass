package eu.client.pingbypass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads and exposes PingBypass configuration from euclient/pingbypass.properties.
 * System properties (-Dpb.server=true etc.) override file values.
 */
public class PingBypassConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingBypassConfig.class);

    public static final String DEFAULT_SERVER = "false";
    public static final String DEFAULT_IP = "0.0.0.0";
    public static final String DEFAULT_PORT = "25565";
    public static final String DEFAULT_PASSWORD = "";

    private static final String KEY_SERVER = "pb.server";
    private static final String KEY_IP = "pb.ip";
    private static final String KEY_PORT = "pb.port";
    private static final String KEY_PASSWORD = "pb.password";

    private final Properties properties = new Properties();
    private final Path configPath;
    private boolean loaded;

    /**
     * Creates a PingBypassConfig that loads from the given run directory.
     *
     * @param runDirectory the Minecraft run directory
     */
    public PingBypassConfig(Path runDirectory) {
        this.configPath = runDirectory.resolve(Paths.get("euclient", "pingbypass.properties"));
    }

    /**
     * Loads properties from the config file, creating it with defaults if it does not exist.
     */
    public void load() {
        try {
            Path parentDir = configPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            if (!Files.exists(configPath)) {
                createDefaults();
            }

            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load PingBypass config from {}", configPath, e);
        } finally {
            loaded = true;
        }
    }

    private void createDefaults() throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty(KEY_SERVER, DEFAULT_SERVER);
        defaults.setProperty(KEY_IP, DEFAULT_IP);
        defaults.setProperty(KEY_PORT, DEFAULT_PORT);
        defaults.setProperty(KEY_PASSWORD, DEFAULT_PASSWORD);
        try (OutputStream out = Files.newOutputStream(configPath)) {
            defaults.store(out, "PingBypass configuration");
        }
    }

    /**
     * Returns whether this instance is configured as a server/proxy.
     */
    public boolean isServer() {
        return Boolean.parseBoolean(getProperty(KEY_SERVER, DEFAULT_SERVER));
    }

    /**
     * Returns the configured IP address.
     */
    public String getIp() {
        return getProperty(KEY_IP, DEFAULT_IP);
    }

    /**
     * Returns the configured port, falling back to 25565 on malformed values.
     */
    public int getPort() {
        String portStr = getProperty(KEY_PORT, DEFAULT_PORT);
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                LOGGER.error("PingBypass port out of range (1-65535): {}, falling back to default 25565", port);
                return 25565;
            }
            return port;
        } catch (NumberFormatException e) {
            LOGGER.error("PingBypass port is not a valid integer: '{}', falling back to default 25565", portStr);
            return 25565;
        }
    }

    /**
     * Returns the configured password.
     */
    public String getPassword() {
        return getProperty(KEY_PASSWORD, DEFAULT_PASSWORD);
    }

    /**
     * Returns true if a non-empty password is configured.
     */
    public boolean hasPassword() {
        String password = getPassword();
        return password != null && !password.isEmpty();
    }

    /**
     * Returns the value for the given property key. System properties override file values.
     *
     * @param key          the property key
     * @param defaultValue the default value if not found
     * @return the resolved property value
     */
    public String getProperty(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Serializes the current effective configuration to a Properties object.
     * System property overrides are reflected in the output.
     */
    public Properties toProperties() {
        Properties result = new Properties();
        result.setProperty(KEY_SERVER, String.valueOf(isServer()));
        result.setProperty(KEY_IP, getIp());
        result.setProperty(KEY_PORT, String.valueOf(getPort()));
        result.setProperty(KEY_PASSWORD, getPassword());
        return result;
    }

    /**
     * Returns whether the config has been loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }
}
