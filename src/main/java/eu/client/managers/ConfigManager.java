package eu.client.managers;

import com.google.gson.*;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import eu.client.Pingbypass;
import eu.client.modules.Module;
import eu.client.settings.Setting;
import eu.client.settings.impl.*;
import eu.client.utils.minecraft.IdentifierUtils;
import eu.client.utils.system.FileUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;

@Getter
@Setter
public class ConfigManager {
    private String currentConfig = "default";

    public ConfigManager() {
        loadConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig));
    }

    public void loadConfig() {
        try {
            FileUtils.createDirectory(Pingbypass.MOD_NAME);
            FileUtils.createDirectory(Pingbypass.MOD_NAME + "/Configs");
            FileUtils.createDirectory(Pingbypass.MOD_NAME + "/Client");

            loadGeneral();
            loadWaypoints();
            loadModules(currentConfig);
        } catch (IOException exception) {
            Pingbypass.LOGGER.error("Failed to load the client's configuration!", exception);
            Pingbypass.CHAT_MANAGER
                    .await("The configuration has not been loaded properly. Read the stacktrace for more information.");
        }
    }

    public void saveConfig() {
        try {
            FileUtils.createDirectory(Pingbypass.MOD_NAME);
            FileUtils.createDirectory(Pingbypass.MOD_NAME + "/Configs");
            FileUtils.createDirectory(Pingbypass.MOD_NAME + "/Client");

            saveGeneral();
            saveWaypoints();
            saveModules(currentConfig);
        } catch (IOException exception) {
            Pingbypass.LOGGER.error("Failed to save the client's configuration!", exception);
        }
    }

    public void loadWaypoints() throws IOException {
    }

    public void loadGeneral() throws IOException {
        if (!FileUtils.fileExists(Pingbypass.MOD_NAME + "/General.json"))
            return;
        @Cleanup
        InputStream stream = Files.newInputStream(Paths.get(Pingbypass.MOD_NAME + "/General.json"));

        JsonObject configObject;
        try {
            configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IllegalStateException exception) {
            Pingbypass.LOGGER.error("Failed to load the client's General configuration!", exception);
            Pingbypass.CHAT_MANAGER.await(
                    "The General configuration has not been loaded properly. Read the stacktrace for more information.");
            return;
        }

        if (configObject.has("Config"))
            currentConfig = configObject.get("Config").getAsString();
        if (configObject.has("Prefix"))
            Pingbypass.COMMAND_MANAGER.setPrefix(configObject.get("Prefix").getAsString());
        if (configObject.has("Friends")) {
            for (JsonElement element : configObject.get("Friends").getAsJsonArray()) {
                if (Pingbypass.FRIEND_MANAGER.contains(element.getAsString()))
                    continue;
                Pingbypass.FRIEND_MANAGER.add(element.getAsString());
            }
        }

    }

    public void saveGeneral() throws IOException {
        FileUtils.resetFile(Pingbypass.MOD_NAME + "/General.json");

        JsonObject configObject = new JsonObject();
        configObject.add("Config", new JsonPrimitive(currentConfig));
        configObject.add("Prefix", new JsonPrimitive(Pingbypass.COMMAND_MANAGER.getPrefix()));

        JsonArray friendsArray = new JsonArray();
        Pingbypass.FRIEND_MANAGER.getFriends().forEach(friendsArray::add);
        configObject.add("Friends", friendsArray);

        @Cleanup
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Pingbypass.MOD_NAME + "/General.json"),
                StandardCharsets.UTF_8);
        writer.write(
                new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));
    }

    public void saveWaypoints() throws IOException {
    }

    public void loadModules(String config) throws IOException {
        if (!FileUtils.fileExists(Pingbypass.MOD_NAME + "/Configs/" + config + ".json"))
            return;
        @Cleanup
        InputStream stream = Files.newInputStream(Paths.get(Pingbypass.MOD_NAME + "/Configs/" + config + ".json"));

        JsonObject configObject;
        try {
            configObject = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IllegalStateException exception) {
            Pingbypass.LOGGER.error("Failed to load the client's Module configuration!", exception);
            Pingbypass.CHAT_MANAGER.await(
                    "The configuration for the Modules has not been loaded properly. Read the stacktrace for more information.");
            return;
        }

        if (!configObject.has("Modules"))
            return;
        JsonObject modulesObject = configObject.get("Modules").getAsJsonObject();

        for (Module module : Pingbypass.MODULE_MANAGER.getModules()) {
            if (!modulesObject.has(module.getName())) {
                module.setToggled(false);
                module.resetValues();
                continue;
            }

            JsonObject moduleObject = modulesObject.get(module.getName()).getAsJsonObject();

            module.setToggled(moduleObject.has("Status") && moduleObject.get("Status").getAsBoolean(), false);

            if (!moduleObject.has("Settings")) {
                module.resetValues();
                continue;
            }

            JsonObject settingsObject = moduleObject.get("Settings").getAsJsonObject();

            for (Setting uncastedSetting : module.getSettings()) {
                JsonElement valueObject = settingsObject.get(uncastedSetting.getName());
                if (valueObject == null || !valueObject.isJsonPrimitive()) {
                    switch (uncastedSetting) {
                        case BooleanSetting setting -> setting.resetValue();
                        case NumberSetting setting -> setting.resetValue();
                        case ModeSetting setting -> setting.resetValue();
                        case StringSetting setting -> setting.resetValue();
                        case BindSetting setting -> setting.resetValue();
                        case ColorSetting setting -> setting.resetValue();
                        case WhitelistSetting setting -> setting.clear();
                        default -> {
                        }
                    }

                    continue;
                }

                switch (uncastedSetting) {
                    case BooleanSetting setting -> setting.setValue(valueObject.getAsBoolean());
                    case NumberSetting setting -> setting.setValue(valueObject.getAsNumber());
                    case ModeSetting setting -> setting.setValue(valueObject.getAsString());
                    case StringSetting setting -> setting.setValue(valueObject.getAsString());
                    case BindSetting setting -> setting.setValue(valueObject.getAsInt());
                    case ColorSetting setting -> {
                        String[] data = valueObject.getAsString().split(",");
                        if (data.length != 6)
                            continue;

                        setting.setColor(new Color(Math.clamp(Integer.parseInt(data[0]), 0, 255),
                                Math.clamp(Integer.parseInt(data[1]), 0, 255),
                                Math.clamp(Integer.parseInt(data[2]), 0, 255),
                                Math.clamp(Integer.parseInt(data[3]), 0, 255)));
                        setting.setSync(Boolean.parseBoolean(data[4]));
                        setting.setRainbow(Boolean.parseBoolean(data[5]));
                    }
                    case WhitelistSetting setting -> {
                        String[] data = valueObject.getAsString().split(",");
                        if (data.length == 0)
                            continue;

                        for (String object : data) {
                            if (setting.isWhitelistContains(object))
                                continue;

                            if (setting.getType() == WhitelistSetting.Type.ITEMS) {
                                Item item = IdentifierUtils.getItem(object);
                                if (item == null)
                                    continue;

                                setting.add(item);
                            } else if (setting.getType() == WhitelistSetting.Type.BLOCKS) {
                                Block block = IdentifierUtils.getBlock(object);
                                if (block == null)
                                    continue;

                                setting.add(block);
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        this.currentConfig = config;

    }

    public void saveModules(String config) throws IOException {
        FileUtils.resetFile(Pingbypass.MOD_NAME + "/Configs/" + config + ".json");

        JsonObject configObject = new JsonObject();
        configObject.add("Config", new JsonPrimitive(config));

        JsonObject modulesObject = new JsonObject();
        for (Module module : Pingbypass.MODULE_MANAGER.getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.add("Status", new JsonPrimitive(module.isToggled()));

            JsonObject settingsObject = new JsonObject();
            for (Setting uncastedSetting : module.getSettings()) {
                switch (uncastedSetting) {
                    case BooleanSetting setting ->
                        settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case NumberSetting setting ->
                        settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case ModeSetting setting ->
                        settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case StringSetting setting ->
                        settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case BindSetting setting ->
                        settingsObject.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    case ColorSetting setting -> settingsObject.add(setting.getName(),
                            new JsonPrimitive(setting.getValue().getColor().getRed() + ","
                                    + setting.getValue().getColor().getGreen() + ","
                                    + setting.getValue().getColor().getBlue() + ","
                                    + setting.getValue().getColor().getAlpha() + "," + setting.isSync() + ","
                                    + setting.isRainbow()));
                    case WhitelistSetting setting -> {
                        StringJoiner objects = new StringJoiner(",");
                        for (String id : setting.getWhitelistIds())
                            objects.add(id);
                        settingsObject.add(setting.getName(), new JsonPrimitive(objects.toString()));
                    }
                    default -> {
                    }
                }
            }

            moduleObject.add("Settings", settingsObject);
            modulesObject.add(module.getName(), moduleObject);
        }

        configObject.add("Modules", modulesObject);

        @Cleanup
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(Pingbypass.MOD_NAME + "/Configs/" + config + ".json"), StandardCharsets.UTF_8);
        writer.write(
                new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(configObject.toString())));

        this.currentConfig = config;
    }
}
