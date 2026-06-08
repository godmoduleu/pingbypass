package eu.client.modules.impl.core;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.system.MathUtils;
import eu.client.utils.system.Timer;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

@RegisterModule(
        name = "RPC",
        description = "Enables Discord Rich Presence for the client.",
        category = Module.Category.CORE
)
public class RPCModule extends Module {

    public ModeSetting detailsMode = new ModeSetting(
            "Details",
            "The mode for the discord presence details.",
            "Random",
            new String[]{"Custom", "Random"}
    );

    public StringSetting customDetails = new StringSetting(
            "CustomDetails",
            "Custom RPC text.",
            new ModeSetting.Visibility(detailsMode, "Custom"),
            "margielaware.cc"
    );

    private final String[] DETAILS = {
            "my richness powered by EUClient"
    };

    private final RichPresence rpc = new RichPresence();
    private final Timer timer = new Timer();

    @Override
    public void onEnable() {
        DiscordIPC.start(1474637830906052631L, null);

        rpc.setStart(EUClient.UPTIME / 1000);

        // set initial text immediately
        rpc.setDetails(getDetails());

        DiscordIPC.setActivity(rpc);
    }

    @Override
    public void onDisable() {
        DiscordIPC.stop();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {

        // update text occasionally if random mode
        if (detailsMode.getValue().equals("Random")) {
            if (timer.hasTimeElapsed(300000)) {
                rpc.setDetails(getDetails());
                DiscordIPC.setActivity(rpc);
                timer.reset();
            }
        } else {
            rpc.setDetails(customDetails.getValue());
            DiscordIPC.setActivity(rpc);
        }
    }

    private String getDetails() {
        return DETAILS[(int) MathUtils.random(DETAILS.length, 0)];
    }
}