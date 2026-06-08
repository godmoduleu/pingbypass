package eu.client.modules.impl.movement;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.EntityUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "FastWeb", description = "Allows you to move quickly through webs.", category = Module.Category.MOVEMENT)
public class FastWebModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used to speed you through webs.", "Normal", new String[]{"Normal", "Ignore", "Strong"});
    public NumberSetting speed = new NumberSetting("Speed", "The speed at which you will fall through webs.", new ModeSetting.Visibility(mode, "Normal"), 3.0, 0.1, 10.0);
    public NumberSetting horizontal = new NumberSetting("Horizontal", "The speed at which you will move horizontally.", new ModeSetting.Visibility(mode, "Strong"), 2.5, 0.1, 5.0);
    public NumberSetting vertical = new NumberSetting("Vertical", "The speed at which you will move vertically.", new ModeSetting.Visibility(mode, "Strong"), 2.5, 0.1, 5.00);
    public BooleanSetting sneak = new BooleanSetting("Sneak", "Only bypasses web slowdown when sneaking.", new ModeSetting.Visibility(mode, "Normal"), false);
    public BooleanSetting grim = new BooleanSetting("Grim", "Includes bypasses for the Grim anticheat.", false);

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.sneakKey.isPressed() || !sneak.getValue()) {
            if (mode.getValue().equals("Normal") && !mc.player.isOnGround() && EntityUtils.isInWeb(mc.player)) {
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - speed.getValue().doubleValue(), mc.player.getVelocity().z);
            }

            if (grim.getValue()) {
                for (BlockPos position : getWebs()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, position, Direction.DOWN));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, Direction.DOWN));
                }
            }
        }
    }

    @Override
    public String getMetaData() {
        return mode.getValue();
    }

    public List<BlockPos> getWebs() {
        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = 2; x > -2; --x) {
            for (int y = 2; y > -2; --y) {
                for (int z = 2; z > -2; --z) {
                    BlockPos position = mc.player.getBlockPos().add(x, y, z);
                    if (mc.world.getBlockState(position).getBlock() instanceof CobwebBlock) {
                        blocks.add(position);
                    }
                }
            }
        }
        return blocks;
    }
}
