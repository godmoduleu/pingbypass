package eu.client.modules.impl.movement;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.KeyInputEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BindSetting;
import eu.client.settings.impl.BooleanSetting;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.system.Timer;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "HitboxDesync", description = "Precisely offsets your position to glitch out Minecraft hitbox calculations.", category = Module.Category.MOVEMENT)
public class HitboxDesyncModule extends Module {
    public BooleanSetting alternating = new BooleanSetting("Alternating", "Modifies your position in such a way that it messes with other clients.", false);
    public BooleanSetting minimal = new BooleanSetting("Minimal", "Makes alternating minimal, only required on certain servers.", new BooleanSetting.Visibility(alternating, true), false);
    public BooleanSetting specific = new BooleanSetting("Specific", "Specific alternating mode, only required against certain clients.", new BooleanSetting.Visibility(alternating, true), false);

    public BooleanSetting close = new BooleanSetting("Close", "Whether or not to have the surround place blocks on desynced positions.", false);

    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable", "Toggles off the module after having desynced your hitbox.", new BooleanSetting.Visibility(alternating, false), true);
    public BooleanSetting jumpDisable = new BooleanSetting("JumpDisable", "Toggles off the module whenever your Y level changes.", new BooleanSetting.Visibility(alternating, true), true);

    @Getter private final Timer timer = new Timer();
    private double prevY;

    @SubscribeEvent(priority = Integer.MAX_VALUE)
    public void onTick(TickEvent event) {
        if (getNull() || (jumpDisable.getValue() && mc.player.getY() != prevY)) {
            setToggled(false);
            return;
        }

        Vec3d vec3d = mc.player.getBlockPos().toCenterPos();
        double offset = minimal.getValue() ? 0.001 : 0.002;
        double timeout = specific.getValue() ? 500 : 1500;

        boolean flag = timer.hasTimeElapsed(timeout) && alternating.getValue() && !mc.player.isSneaking();
        boolean flagX = (vec3d.x - mc.player.getX()) > 0;
        boolean flagZ = (vec3d.z - mc.player.getZ()) > 0;

        double x = vec3d.x + ((flag ? offset : 0) * (flagX ? 1 : -1)) + 0.20000000009497754 * (flagX ? -1 : 1);
        double z = vec3d.z + ((flag ? offset : 0) * (flagZ ? 1 : -1)) + 0.2000000000949811 * (flagZ ? -1 : 1);

        mc.player.setPosition(x, mc.player.getY(), z);

        if (timer.hasTimeElapsed(timeout)) {
            timer.reset();
        }

        if (selfDisable.getValue() && !alternating.getValue()) setToggled(false);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        prevY = mc.player.getY();
    }
}
