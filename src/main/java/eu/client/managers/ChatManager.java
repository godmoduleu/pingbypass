package eu.client.managers;

import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.TickEvent;
import eu.client.utils.IMinecraft;
import eu.client.utils.chat.ChatUtils;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatManager implements IMinecraft {
    private final List<String> awaitMessages = new ArrayList<>();

    public ChatManager() {
        Pingbypass.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null)
            return;
        if (awaitMessages.isEmpty())
            return;

        for (String message : new ArrayList<>(awaitMessages)) {
            addMessage(message);
            awaitMessages.remove(message);
        }
    }

    public void message(String message) {
        addMessage("[Pingbypass] " + message);
    }

    public void message(String message, String identifier) {
        addMessage("[Pingbypass] " + message);
    }

    public void tagged(String message, String tag) {
        addMessage("[Pingbypass] [" + tag + "]: " + message);
    }

    public void tagged(String message, String tag, String identifier) {
        addMessage("[Pingbypass] [" + tag + "]: " + message);
    }

    public void info$await(String message) {
        awaitMessages.add("[Pingbypass] [?] " + message);
    }

    public void info(String message) {
        addMessage("[Pingbypass] [?] " + message);
    }

    public void warn$await(String message) {
        awaitMessages.add("[Pingbypass] [!] " + message);
    }

    public void warn(String message) {
        addMessage("[Pingbypass] [!] " + message);
    }

    public void error$await(String message) {
        awaitMessages.add("[Pingbypass] [!!] " + message);
    }

    public void error(String message) {
        addMessage("[Pingbypass] [!!] " + message);
    }

    public void await(String message) {
        awaitMessages.add("[Pingbypass] " + message);
    }

    public void await(String message, String tag) {
        awaitMessages.add("[Pingbypass] [" + tag + "]: " + message);
    }

    public void addMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
        Pingbypass.LOGGER.info(message);
    }

    public void addMessage(String message, String identifier) {
        addMessage(message);
    }

    public static void deleteMessage(String identifier) {
        // Headless fallback
    }
}
