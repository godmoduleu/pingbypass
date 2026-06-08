package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.UpdateMovementEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@RegisterModule(name = "Step", description = "Gives you the ability to instantly climb over a customizable amount of blocks.", category = Module.Category.MOVEMENT)
public class StepModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The mode that will be used to climb over blocks.", "Vanilla", new String[]{"Vanilla", "NCP"});
    public NumberSetting height = new NumberSetting("Height", "The maximum height at which blocks can be climbed over.", 2.0f, 0.0f, 12.0f);
    public BooleanSetting useTimer = new BooleanSetting("UseTimer", "Uses timer to slow you down while climbing.", new ModeSetting.Visibility(mode, "NCP"), true);

    private boolean resetTimer = false;

    @Override
    public void onDisable() {
        EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f);
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (resetTimer) {
            EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f);
            resetTimer = false;
        }
    }

    @SubscribeEvent
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.getValue().equalsIgnoreCase("NCP")) return;

        double stepHeight = mc.player.getY() - mc.player.prevY;
        if (stepHeight <= 0.75 || stepHeight > height.getValue().doubleValue())
            return;

        double[] offsets = getOffset(stepHeight);
        if (offsets != null && offsets.length > 1) {
            if (useTimer.getValue()) {
                EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f / offsets.length);
                resetTimer = true;
            }

            for (double offset : offsets) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.prevX, mc.player.prevY + offset, mc.player.prevZ, false, mc.player.horizontalCollision));
            }
        }
    }

    @Override
    public String getMetaData() {
        return String.valueOf(height.getValue().floatValue());
    }

    public double[] getOffset(double height) {
        return switch ((int) (height * 10000)) {
            case 7500, 10000 -> new double[]{0.42, 0.753};
            case 8125, 8750 -> new double[]{0.39, 0.7};
            case 15000 -> new double[]{0.42, 0.75, 1.0, 1.16, 1.23, 1.2};
            case 20000 -> new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
            case 250000 -> new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907};
            default -> null;
        };
    }
}
