package eu.client.commands.impl;

import eu.client.Pingbypass;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import eu.client.modules.Module;
import eu.client.settings.Setting;
import eu.client.settings.impl.BooleanSetting;
import eu.client.utils.chat.ChatUtils;
import net.minecraft.util.Formatting;

@RegisterCommand(name = "toggle", tag = "Toggle", description = "Toggles a specified module or a setting on and off.", syntax = "<[module]> | <[module]> <[setting]>", aliases = {"t"})
public class ToggleCommand extends Command {
    @Override
    public void execute(String[] args) {
        if (args.length == 1 || args.length == 2) {
            Module module = Pingbypass.MODULE_MANAGER.getModule(args[0]);
            if (module == null) {
                Pingbypass.CHAT_MANAGER.tagged("Could not find the module specified.", getTag(), getName());
                return;
            }

            if (args.length == 1) {
                if (module.isPersistent()) {
                    Pingbypass.CHAT_MANAGER.tagged("Cannot toggle a persistent module.", getTag(), getName());
                    return;
                }

                module.setToggled(!module.isToggled(), false);
                Pingbypass.CHAT_MANAGER.tagged(ChatUtils.getPrimary() + module.getName() + ChatUtils.getSecondary() + " has been toggled " + (module.isToggled() ? Formatting.GREEN + "on" : Formatting.RED + "off") + ChatUtils.getSecondary() + ".", getTag(), getName() + "-cmd-" + module.getName());
            }

            if (args.length == 2) {
                Setting setting = module.getSetting(args[1]);
                if (setting == null) {
                    Pingbypass.CHAT_MANAGER.tagged("Could not find the setting specified.", getTag(), getName());
                    return;
                }

                if (!(setting instanceof BooleanSetting booleanSetting)) {
                    Pingbypass.CHAT_MANAGER.tagged("This command only works for " + ChatUtils.getPrimary() + "boolean" + ChatUtils.getSecondary() + " settings.", getTag(), getName());
                    return;
                }

                booleanSetting.setValue(!booleanSetting.getValue());
                Pingbypass.CHAT_MANAGER.tagged(ChatUtils.getPrimary() + setting.getName() + ChatUtils.getSecondary() + " has been toggled " + (booleanSetting.getValue() ? Formatting.GREEN + "on" : Formatting.RED + "off") + ChatUtils.getSecondary() + " for " + ChatUtils.getPrimary() + module.getName() + ChatUtils.getSecondary() + ".", getTag(), getName() + "-cmd-" + module.getName() + "-" + setting.getName() );

            }
        } else {
            messageSyntax();
        }
    }
}
