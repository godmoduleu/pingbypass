package eu.client.commands.impl;

import eu.client.EUClient;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import net.minecraft.util.Util;

import java.io.File;

@RegisterCommand(name = "folder", description = "Opens the clients folder.")
public class FolderCommand extends Command {
    @Override
    public void execute(String[] args) {
        File folder = new File(EUClient.MOD_NAME);
        if (folder.exists()) {
            Util.getOperatingSystem().open(folder);
        } else {
            EUClient.CHAT_MANAGER.info("Could not find the client's configuration folder.");
        }
    }
}
