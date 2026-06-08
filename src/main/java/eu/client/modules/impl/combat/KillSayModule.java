package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.system.FileUtils;
import eu.client.utils.system.MathUtils;
import eu.client.utils.system.Timer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@RegisterModule(name = "KillSay", description = "Automatically sends a message when you kill someone.", category = Module.Category.COMBAT)
public class KillSayModule extends Module {
    private final String[] KILL_MESSAGES = {"Sit [username], EUClient owns me and all!", "[username] I killed you with the power of EUClient!", "Sorry [username], get good get EUClient!", "I just killed [username] thanks to EUClient!"};

    public BooleanSetting kills = new BooleanSetting("Kills", "Sends a message when you kill someone.", true);
    public BooleanSetting pops = new BooleanSetting("Pops", "Sends a message when you pop someone.", false);
    public ModeSetting mode = new ModeSetting("Mode", "The mode for the messages.", "Default", new String[]{"Default", "Custom"});
    public StringSetting killFile = new StringSetting("KillFile", "The name of the file containing the kill messages.", "killfile.txt");
    public StringSetting popFile = new StringSetting("PopFile", "The name of the file containing the pop messages.", "popfile.txt");
    public NumberSetting delay = new NumberSetting("Delay", "The delay for the announcer.", 5, 0, 10);
    public BooleanSetting clientside = new BooleanSetting("Clientside", "Sends the messages only on your side.", false);
    public BooleanSetting greenText = new BooleanSetting("GreenText", "Makes your message green.", false);
    public BooleanSetting killStreak = new BooleanSetting("KillStreak", "Keeps track of your kills.", false);
    public BooleanSetting suicideIgnore = new BooleanSetting("SuicideIgnore", "Doesn't reset kill streak when you suicide.", false);

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private List<String> killMessages = new ArrayList<>();
    private List<String> popMessages = new ArrayList<>();
    private final Timer timer = new Timer();
    private String lastMessage;
    private int streak;
    private boolean suicide = false;

    @Override
    public void onEnable() {
        queue.clear();
        timer.reset();
        streak = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(getNull() || queue.isEmpty()) return;

        killMessages = FileUtils.readLines(new File(EUClient.MOD_NAME + "/Client/" + killFile.getValue()));
        popMessages = FileUtils.readLines(new File(EUClient.MOD_NAME + "/Client/" + popFile.getValue()));

        synchronized (queue) {
            if (timer.hasTimeElapsed(delay.getValue().intValue() * 1000)) {
                String message = queue.poll();
                if(!message.isEmpty()) {
                    if (clientside.getValue()) EUClient.CHAT_MANAGER.message(message);
                    else mc.player.networkHandler.sendChatMessage((greenText.getValue() ? "> " : "") + message);

                    lastMessage = message;
                }
                timer.reset();
            }
        }
    }

    @SubscribeEvent
    public void onTargetDeath(TargetDeathEvent event) {
        if(getNull() || event.getPlayer() == mc.player) return;
        streak++;

        synchronized (queue) {
            if (kills.getValue()) {
                queue.clear();
                String message = getKillMessage(event.getPlayer().getName().getString());
                queue.add(message);
            }
        }

        if(killStreak.getValue() && streak > 1) {
            EUClient.CHAT_MANAGER.message("You are on a " + streak + " kill streak.");
        }
    }

    @SubscribeEvent
    public void onPlayerPop(PlayerPopEvent event) {
        if(getNull() || event.getPlayer() == mc.player || !EUClient.TARGET_MANAGER.isTarget(event.getPlayer()) || !pops.getValue()) return;

        synchronized (queue) {
            String message = getPopMessage(event.getPlayer().getName().getString(), event.getPops());
            queue.add(message);
        }
    }

    @SubscribeEvent
    public void onCommandInput(CommandInputEvent event) {
        if(getNull()) return;

        if(event.getMessage().contains("/kill")) suicide = true;
    }

    @SubscribeEvent
    public void onChatInput(ChatInputEvent event) {
        if(getNull()) return;
        timer.reset();
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(event.getPlayer() == mc.player && (!suicideIgnore.getValue() || !suicide && !EUClient.MODULE_MANAGER.getModule(SuicideModule.class).isToggled())) {
            streak = 0;
        }

        suicide = false;
    }

    private String getPopMessage(String username, int pops) {
        String message;
        if(mode.getValue().equals("Custom") && !popMessages.isEmpty()) {
            do message = popMessages.get((int) MathUtils.random(popMessages.size(), 0)).replace("[username]", username);
            while (message.equals(lastMessage) && popMessages.size() > 1);
        } else {
            message = pops + (pops > 1 ? " pops " : " pop ") + username + "!";
        }

        return message;
    }

    private String getKillMessage(String username) {
        String message;
        if(mode.getValue().equals("Custom") && !killMessages.isEmpty()) {
            do message = killMessages.get((int) MathUtils.random(killMessages.size(), 0)).replace("[username]", username);
            while (message.equals(lastMessage) && killMessages.size() > 1);
        } else {
            do message = KILL_MESSAGES[(int) MathUtils.random(KILL_MESSAGES.length, 0)].replace("[username]", username);
            while (message.equals(lastMessage));
        }

        return message;
    }

    @Override
    public String getMetaData() {
        return streak + "";
    }
}
