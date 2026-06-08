package eu.client.commands.impl;

import eu.client.Pingbypass;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.system.FileUtils;

import java.io.IOException;

@RegisterCommand(name = "config", tag = "Config", description = "Allows you to manage the client's configuration system.", syntax = "<load|save> <[name]> | <reload|save|current>")
public class ConfigCommand extends Command {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "load" -> {
                    if (!FileUtils.fileExists(Pingbypass.MOD_NAME + "/Configs/" + args[1] + ".json")) {
                        Pingbypass.CHAT_MANAGER.tagged("The specified configuration does not exist.", getTag(), getName());
                        return;
                    }

                    try {
                        Pingbypass.CONFIG_MANAGER.loadModules(args[1]);
                        Pingbypass.CHAT_MANAGER.tagged("Successfully loaded the " + ChatUtils.getPrimary() + args[1] + ChatUtils.getSecondary() + " configuration.", getTag(), getName());
                    } catch (IOException exception) {
                        Pingbypass.CHAT_MANAGER.tagged("Failed to load the " + ChatUtils.getPrimary() + args[1] + ChatUtils.getSecondary() + " configuration.", getTag(), getName());
                    }
                }
                case "save" -> {
                    try {
                        Pingbypass.CONFIG_MANAGER.saveModules(args[1]);
                        Pingbypass.CHAT_MANAGER.tagged("Successfully saved the configuration to " + ChatUtils.getPrimary() + args[1] + ".json" + ChatUtils.getSecondary() + ".", getTag(), getName());
                    } catch (IOException exception) {
                        Pingbypass.CHAT_MANAGER.tagged("Failed to save the " + ChatUtils.getPrimary() + args[1] + ChatUtils.getSecondary() + " configuration.", getTag(), getName());
                    }
                }
                default -> messageSyntax();
            }
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    Pingbypass.CONFIG_MANAGER.loadConfig();
                    Pingbypass.CHAT_MANAGER.tagged("Successfully reloaded the current configuration.", getTag(), getName());
                }
                case "save" -> {
                    Pingbypass.CONFIG_MANAGER.saveConfig();
                    Pingbypass.CHAT_MANAGER.tagged("Successfully saved the current configuration.", getTag(), getName());
                }
                case "current" -> Pingbypass.CHAT_MANAGER.tagged("The client is currently using the " + ChatUtils.getPrimary() + Pingbypass.CONFIG_MANAGER.getCurrentConfig() + ChatUtils.getSecondary() + " configuration.", getTag(), getName());
                default -> messageSyntax();
            }
        } else {
            messageSyntax();
        }
    }
}
