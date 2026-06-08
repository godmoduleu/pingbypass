package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.NetworkUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "NoSlow", description = "Removes the slowness effect that you receive when doing certain actions.", category = Module.Category.MOVEMENT)
public class NoSlowModule extends Module {
    public BooleanSetting items = new BooleanSetting("Items", "Removes the slowness effect from eating or using items.", true);
    public BooleanSetting soulSand = new BooleanSetting("SoulSand", "Removes the slowness effect from walking on soul sand.", false);
    public BooleanSetting slimeBlocks = new BooleanSetting("SlimeBlocks", "Removes the slowness effect from walking on slime blocks.", false);
    public BooleanSetting honeyBlocks = new BooleanSetting("HoneyBlocks", "Removes the slowness effect from walking on honey blocks.", false);

    public BooleanSetting ncpStrict = new BooleanSetting("NCPStrict", "Makes use of ground bypasses for the NoCheatPlus anticheat.", false);
    public BooleanSetting airStrict = new BooleanSetting("AirStrict", "Makes use of air bypasses for the NoCheatPlus anticheat.", false);
    public BooleanSetting grimStrict = new BooleanSetting("GrimStrict", "Makes use of bypasses for the Grim anticheat.", false);

    private boolean sneaking = false;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (airStrict.getValue() && sneaking && !mc.player.isUsingItem()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            sneaking = false;
        }
    }

    @SubscribeEvent
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!grimStrict.getValue()) return;
        if (!mc.player.isUsingItem() || mc.player.isRiding() || mc.player.isGliding()) return;

        if (mc.player.getActiveHand() == Hand.OFF_HAND) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 8 + 1));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 7 + 2));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        } else {
            NetworkUtils.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (ncpStrict.getValue()) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket.Full || event.getPacket() instanceof PlayerMoveC2SPacket.PositionAndOnGround || event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround || event.getPacket() instanceof PlayerMoveC2SPacket.OnGroundOnly) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }

            if (event.getPacket() instanceof ClickSlotC2SPacket) {
                if (mc.player.isUsingItem()) mc.player.stopUsingItem();
                if (mc.player.isSprinting()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                if (mc.player.isSneaking()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
        }
    }

    @SubscribeEvent
    public void onChangeHand(ChangeHandEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (airStrict.getValue() && !sneaking && (!mc.player.isRiding() && !mc.player.isSneaking() && (mc.player.isUsingItem() && items.getValue() && !grimStrict.getValue()))) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            sneaking = true;
        }
    }

    @Override
    public void onDisable() {
        EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f);

        if (mc.player == null) return;

        if (airStrict.getValue() && sneaking) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        sneaking = false;
    }

    public boolean shouldSlow() {
        return grimStrict.getValue() && mc.player.getActiveHand() == Hand.MAIN_HAND && (mc.player.getOffHandStack().getComponents().contains(DataComponentTypes.FOOD) || mc.player.getOffHandStack().getItem() == Items.SHIELD);
    }
}
