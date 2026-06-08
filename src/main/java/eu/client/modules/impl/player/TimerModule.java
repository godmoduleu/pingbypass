package eu.client.modules.impl.player;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "Timer", description = "Makes your game run at a faster tick speed.", category = Module.Category.PLAYER)
public class TimerModule extends Module {
    public NumberSetting multiplier = new NumberSetting("Multiplier", "The multiplier that will be added to the game's speed.", 1.0f, 0.0f, 20.0f);

    @SubscribeEvent(priority = Integer.MIN_VALUE)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        EUClient.WORLD_MANAGER.setTimerMultiplier(multiplier.getValue().floatValue());
    }

    @Override
    public void onEnable() {
        EUClient.WORLD_MANAGER.setTimerMultiplier(multiplier.getValue().floatValue());
    }

    @Override
    public void onDisable() {
        EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f);
    }

    @Override
    public String getMetaData() {
        return String.valueOf(multiplier.getValue().floatValue());
    }
}
