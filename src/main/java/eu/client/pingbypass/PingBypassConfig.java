package eu.client.pingbypass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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

    public PingBypassConfig(Path runDirectory) {
        this.configPath = runDirectory.resolve(Paths.get("pingbypass", "pingbypass.properties"));
    }

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

    public boolean isServer() {
        return Boolean.parseBoolean(getProperty(KEY_SERVER, DEFAULT_SERVER));
    }

    public String getIp() {
        return getProperty(KEY_IP, DEFAULT_IP);
    }

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

    public String getPassword() {
        return getProperty(KEY_PASSWORD, DEFAULT_PASSWORD);
    }

    public boolean hasPassword() {
        String password = getPassword();
        return password != null && !password.isEmpty();
    }

    public String getProperty(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    public Properties toProperties() {
        Properties result = new Properties();
        result.setProperty(KEY_SERVER, String.valueOf(isServer()));
        result.setProperty(KEY_IP, getIp());
        result.setProperty(KEY_PORT, String.valueOf(getPort()));
        result.setProperty(KEY_PASSWORD, getPassword());
        return result;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
