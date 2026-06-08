package eu.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.client.commands.CommandManager;
import eu.client.events.EventHandler;
import eu.client.managers.*;
import eu.client.modules.ModuleManager;
import eu.client.pingbypass.PingBypassConfig;
import eu.client.pingbypass.protocol.PbCustomPayload;
import eu.client.pingbypass.server.ProxyServer;
import eu.client.pingbypass.server.ProxyServerTickListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pingbypass implements ModInitializer {
	public static final String MOD_NAME = BuildConstants.MOD_NAME;
	public static final String MOD_ID = BuildConstants.MOD_ID;
	public static final String MOD_VERSION = BuildConstants.MOD_VERSION;
	public static final String MINECRAFT_VERSION = BuildConstants.MINECRAFT_VERSION;
	public static final String GIT_HASH = BuildConstants.GIT_HASH;
	public static final String GIT_REVISION = BuildConstants.GIT_REVISION;
	public static final long UPTIME = System.currentTimeMillis();

	public static final EventHandler EVENT_HANDLER = new EventHandler();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private static final ExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadExecutor();
	public static String UPDATE_STATUS = "none";

	private static final String VERSION_URL = "";
	private static final String SECRET_KEY = "";

	public static ChatManager CHAT_MANAGER;
	public static FriendManager FRIEND_MANAGER;
	public static WorldManager WORLD_MANAGER;
	public static PositionManager POSITION_MANAGER;
	public static RotationManager ROTATION_MANAGER;
	public static ServerManager SERVER_MANAGER;
	public static TargetManager TARGET_MANAGER;
	public static TaskManager TASK_MANAGER;

	public static ModuleManager MODULE_MANAGER;
	public static CommandManager COMMAND_MANAGER;

	public static ConfigManager CONFIG_MANAGER;

	public static PingBypassConfig PINGBYPASS_CONFIG;
	public static ProxyServer PROXY_SERVER;

	@Override
	public void onInitialize() {

		// Register PingBypass custom payload for both directions
		PayloadTypeRegistry.playC2S().register(PbCustomPayload.ID, PbCustomPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PbCustomPayload.ID, PbCustomPayload.CODEC);

		// Register Fabric receiver so the networking layer doesn't reject S2C payloads.
		// Actual handling is done via PacketReceiveEvent in PingBypassModule.
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				PbCustomPayload.ID, (payload, context) -> {
					// No-op: handled by PacketReceiveEvent mixin
				});

		PINGBYPASS_CONFIG = new PingBypassConfig(FabricLoader.getInstance().getGameDir());
		PINGBYPASS_CONFIG.load();
		LOGGER.info("PingBypass mode: {}", PINGBYPASS_CONFIG.isServer() ? "server" : "client");

		if (PINGBYPASS_CONFIG.isServer()) {
			try {
				PROXY_SERVER = new ProxyServer(PINGBYPASS_CONFIG);
				PROXY_SERVER.bind(
						InetAddress.getByName(PINGBYPASS_CONFIG.getIp()),
						PINGBYPASS_CONFIG.getPort());
				EVENT_HANDLER.subscribe(new ProxyServerTickListener(PROXY_SERVER));
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					if (PROXY_SERVER != null) {
						PROXY_SERVER.shutdown();
					}
				}, "PingBypass-Shutdown"));
				LOGGER.info("PingBypass proxy server started on {}:{}", PINGBYPASS_CONFIG.getIp(),
						PINGBYPASS_CONFIG.getPort());
				// Registry cache will be loaded lazily on first client connection
			} catch (IOException e) {
				LOGGER.error("Failed to start PingBypass proxy server", e);
			}
		}

		CHAT_MANAGER = new ChatManager();
		FRIEND_MANAGER = new FriendManager();
		WORLD_MANAGER = new WorldManager();
		POSITION_MANAGER = new PositionManager();
		ROTATION_MANAGER = new RotationManager();
		SERVER_MANAGER = new ServerManager();
		TARGET_MANAGER = new TargetManager();
		TASK_MANAGER = new TaskManager();

		MODULE_MANAGER = new ModuleManager();
		COMMAND_MANAGER = new CommandManager();

		// Initialize proxy modules now that MODULE_MANAGER is available
		if (PROXY_SERVER != null) {
			PROXY_SERVER.initModules();
		}
	}

	public static void onPostInitialize() {
		CONFIG_MANAGER = new ConfigManager();

		LOGGER.info("{} {} has been initialized.", MOD_NAME, MOD_VERSION);
	}

	public static void checkForUpdates() {
		UPDATE_EXECUTOR.submit(() -> {
			if (FabricLoader.getInstance().isDevelopmentEnvironment())
				return;
			if (!FabricLoader.getInstance().isModLoaded(MOD_ID + "-updater"))
				return;

			try {
				HttpURLConnection versionConnection = (HttpURLConnection) new URL(VERSION_URL + SECRET_KEY)
						.openConnection();
				versionConnection.setRequestMethod("GET");
				versionConnection.connect();

				if (versionConnection.getResponseCode() == 200) {
					InputStreamReader reader = new InputStreamReader(versionConnection.getInputStream());

					JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
					if (!jsonObject.has("version"))
						return;

					if (!MOD_VERSION.equalsIgnoreCase(jsonObject.get("version").getAsString())) {
						UPDATE_STATUS = "update-available";
					}
				} else {
					UPDATE_STATUS = "failed-connection";
				}
			} catch (IOException exception) {
				UPDATE_STATUS = "failed";
			}

			if (UPDATE_STATUS.equalsIgnoreCase("none"))
				UPDATE_STATUS = "up-to-date";
		});
	}
}