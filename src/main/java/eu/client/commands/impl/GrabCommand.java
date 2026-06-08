package eu.client.commands.impl;

import eu.client.EUClient;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import eu.client.utils.chat.ChatUtils;
import net.minecraft.util.Util;

@RegisterCommand(name = "grab", description = "Lets you copy various things to your clipboard.", syntax = "<ip|coords|name>")
public class GrabCommand extends Command {
    @Override
    public void execute(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
                case "ip" -> copy(EUClient.SERVER_MANAGER.getServer());
                case "coords" -> copy("[" + (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ() + "]");
                case "name" -> copy(mc.player.getName().getString());
                default -> messageSyntax();
            }
        } else {
            messageSyntax();
        }
    }

    private void copy(String text) {
    mc.keyboard.setClipboard(text);
        EUClient.CHAT_MANAGER.tagged("Successfully copied " + ChatUtils.getPrimary() + text + ChatUtils.getSecondary() + " to your clipboard.", getTag(), getName());
    }
}
