package eu.client.modules.impl.miscellaneous;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.ChatInputEvent;
import eu.client.events.impl.PlayerConnectEvent;
import eu.client.events.impl.PlayerDisconnectEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.system.MathUtils;
import eu.client.utils.system.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.util.concurrent.ConcurrentLinkedQueue;

@RegisterModule(name = "Welcomer", description = "Sends a message when a player joins or leaves the server.", category = Module.Category.MISCELLANEOUS)
public class WelcomerModule extends Module {
    public BooleanSetting joins = new BooleanSetting("Joins", "Sends messages when a player joins the server", true);
    public BooleanSetting leaves = new BooleanSetting("Leaves", "Sends messages when a player leaves the server", true);
    public BooleanSetting clientside = new BooleanSetting("Clientside", "Sends the messages only on your side.", false);
    public BooleanSetting unicode = new BooleanSetting("Unicode", "Uses prettier unicode icons instead of normal arrows.", false);
    public BooleanSetting greenText = new BooleanSetting("GreenText", "Makes your message green.", false);
    public NumberSetting delay = new NumberSetting("Delay", "The delay for the announcer.", 5, 0, 30);

    private final String[] JOIN_MESSAGES = {"Hello, ", "Welcome to the server, ", "Good to see you, ", "Greetings, ", "Good evening, ", "Hey, "};
    private final String[] LEAVE_MESSAGES = {"Goodbye, ", "See you later, ", "Bye bye, ", "I hope you had a good time, ", "Farewell, ", "See you next time, "};

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final Timer messageTimer = new Timer();

    @Override
    public void onEnable() {
        queue.clear();
        messageTimer.reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(getNull()) return;

        if (messageTimer.hasTimeElapsed(delay.getValue().intValue() * 1000) && !queue.isEmpty() && !clientside.getValue()) {
            String message = queue.poll();

            if (clientside.getValue()) EUClient.CHAT_MANAGER.message(message);
            else mc.player.networkHandler.sendChatMessage((greenText.getValue() ? "> " : "") + message);

            messageTimer.reset();
        }
    }

    @SubscribeEvent
    public void onChatInput(ChatInputEvent event) {
        if(getNull()) return;
        messageTimer.reset();
    }

    @SubscribeEvent
    public void onPlayerConnect(PlayerConnectEvent event) {
        if(getNull()) return;

        PlayerEntity player = mc.world.getPlayerByUuid(event.getId());
        if(player != null && joins.getValue()) {
            if(clientside.getValue()) EUClient.CHAT_MANAGER.message(Formatting.DARK_GRAY + "[" + Formatting.GREEN + (unicode.getValue() ? "»" : ">") + Formatting.DARK_GRAY + "] " + Formatting.GRAY + player.getName().getString());
            else queue.add(JOIN_MESSAGES[(int) MathUtils.random(JOIN_MESSAGES.length, 0)] + player.getName().getString());
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if(getNull()) return;

        PlayerEntity player = mc.world.getPlayerByUuid(event.getId());
        if(player != null && leaves.getValue()) {
            if(clientside.getValue()) EUClient.CHAT_MANAGER.message(Formatting.DARK_GRAY + "[" + Formatting.RED + (unicode.getValue() ? "«" : "<")+ Formatting.DARK_GRAY + "] " + Formatting.GRAY + player.getName().getString());
            else queue.add(LEAVE_MESSAGES[(int) MathUtils.random(LEAVE_MESSAGES.length, 0)] + player.getName().getString());
        }
    }
}
