package eu.client.commands.impl;

import eu.client.EUClient;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.input.KeyboardUtils;

import java.util.Arrays;

@RegisterCommand(name = "macro", tag = "Macro", description = "Allows you to manage the client's macro system.", syntax = "add <[key]> <[message]> | remove <[key]> | <clear|list>")
public class MacroCommand extends Command {
    @Override
    public void execute(String[] args) {
        if (args.length >= 3) {
            if (args[0].equalsIgnoreCase("add")) {
                int key = KeyboardUtils.getKeyNumber(args[1]);
                if (key == 0) {
                    EUClient.CHAT_MANAGER.tagged("The keybind specified is not valid.", getTag());
                    return;
                }

                StringBuilder builder = new StringBuilder();
                String[] array = Arrays.copyOfRange(args, 2, args.length);
                int index = 0;

                for (String str : array) {
                    index++;
                    builder.append(str).append(index == array.length ? "" : " ");
                }

                EUClient.MACRO_MANAGER.add(builder.toString(), key);
                EUClient.CHAT_MANAGER.tagged("Successfully added " + ChatUtils.getPrimary() + builder + ChatUtils.getSecondary() + " as a macro, bound to the " + ChatUtils.getPrimary() + KeyboardUtils.getKeyName(key) + ChatUtils.getSecondary() + " key.", getTag(), "command-" + getName());
            } else {
                messageSyntax();
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                int key = KeyboardUtils.getKeyNumber(args[1]);
                if (key == 0) {
                    EUClient.CHAT_MANAGER.tagged("The keybind specified is not valid.", getTag());
                    return;
                }

                if (!EUClient.MACRO_MANAGER.containsValue(key)) {
                    EUClient.CHAT_MANAGER.tagged("There is no macro with the " + ChatUtils.getPrimary() + KeyboardUtils.getKeyName(key) + ChatUtils.getSecondary() + " key.", getTag(), "command-" + getName());
                } else {
                    EUClient.CHAT_MANAGER.tagged("Successfully removed the " + ChatUtils.getPrimary() + EUClient.MACRO_MANAGER.getKey(key) + ChatUtils.getSecondary() + " macro.", getTag(), "command-" + getName());
                    EUClient.MACRO_MANAGER.remove(EUClient.MACRO_MANAGER.getKey(key));
                }
            } else {
                messageSyntax();
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("clear")) {
                EUClient.MACRO_MANAGER.clear();
                EUClient.CHAT_MANAGER.tagged("Successfully removed all macros.", getTag(), "command-" + getName());
            } else if (args[0].equalsIgnoreCase("list")) {
                if (EUClient.MACRO_MANAGER.getMacros().isEmpty()) {
                    EUClient.CHAT_MANAGER.tagged("There are currently no macros added.", getTag(), "command-" + getName() + "-list");
                } else {
                    StringBuilder builder = new StringBuilder();
                    int index = 0;

                    for (String message : EUClient.MACRO_MANAGER.getMacros().keySet()) {
                        index++;

                        int key = EUClient.MACRO_MANAGER.getValue(message);
                        builder.append(ChatUtils.getSecondary()).append(message)
                                .append(ChatUtils.getPrimary()).append(" [")
                                .append(ChatUtils.getSecondary()).append(KeyboardUtils.getKeyName(key))
                                .append(ChatUtils.getPrimary()).append("]")
                                .append(ChatUtils.getSecondary())
                                .append(index == EUClient.MACRO_MANAGER.getMacros().size() ? "" : ", ");
                    }

                    EUClient.CHAT_MANAGER.message("Macros " + ChatUtils.getPrimary() + "[" + ChatUtils.getSecondary() + EUClient.MACRO_MANAGER.getMacros().size() + ChatUtils.getPrimary() + "]: " + ChatUtils.getSecondary() + builder, "command-" + getName() + "-list");
                }
            } else {
                messageSyntax();
            }
        } else {
            messageSyntax();
        }
    }
}
