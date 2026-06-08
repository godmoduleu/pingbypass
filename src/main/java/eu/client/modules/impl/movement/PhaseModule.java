package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.MovementUtils;
import eu.client.utils.minecraft.NetworkUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.rotations.RotationUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@RegisterModule(name = "Phase", description = "Phases you inside of a block using pearls.", category = Module.Category.MOVEMENT)
public class PhaseModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used for phasing.", "Pearl", new String[]{"Pearl", "Teleport"});
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", new ModeSetting.Visibility(mode, "Pearl"), "Silent", InventoryUtils.SWITCH_MODES);
    public NumberSetting pitch = new NumberSetting("Pitch", "The pitch at which the pearl will be thrown.", new ModeSetting.Visibility(mode, "Pearl"), 85, 70, 90);

    public BooleanSetting fireCharge = new BooleanSetting("FireCharge", "Uses fire to bypass pearl distance checks.", false);
    public BooleanSetting alternative = new BooleanSetting("Alternative", "Uses a flint and steel rather than a fire charge to perform the phase bypass.", false);
    public ModeSetting fireSwitch = new ModeSetting("FireSwitch", "The mode that will be used for switching to a fire charge.", new BooleanSetting.Visibility(fireCharge, true), "Silent", new String[]{"Normal", "Silent", "AltPickup", "AltSwap"});
    public BooleanSetting autoRemove = new BooleanSetting("AutoRemove", "Automatically removes the fire once you have phased.", new BooleanSetting.Visibility(fireCharge, true), true);

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        if (mode.getValue().equalsIgnoreCase("Pearl")) {
            if (!mc.world.getBlockState(mc.player.getBlockPos()).isReplaceable()) {
                setToggled(false);
                return;
            }

            if (autoSwitch.getValue().equalsIgnoreCase("None") && mc.player.getMainHandStack().getItem() != Items.ENDER_PEARL) {
                EUClient.CHAT_MANAGER.tagged("You are currently not holding any pearls.", getName());
                setToggled(false);
                return;
            }

            if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
                setToggled(false);
                return;
            }

            int slot = InventoryUtils.find(Items.ENDER_PEARL, 0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            int previousSlot = mc.player.getInventory().selectedSlot;
            boolean didFireCharge = false;

            if (slot == -1) {
                EUClient.CHAT_MANAGER.tagged("No pearls could be found in your hotbar.", getName());
                setToggled(false);
                return;
            }

            float yaw = Math.round(RotationUtils.getRotations(new Vec3d(Math.floor(mc.player.getX()) + 0.5, 0, Math.floor(mc.player.getZ()) + 0.5))[0]) + 180;

            float prevYaw = mc.player.getYaw();
            float prevPitch = mc.player.getPitch();

            BlockPos downPosition = mc.player.getBlockPos().down();
            if (fireCharge.getValue() && mc.world.getBlockState(mc.player.getBlockPos()).isAir() && !(mc.world.getBlockState(downPosition).isReplaceable())) {
                int chargeSlot = InventoryUtils.find(alternative.getValue() ? Items.FLINT_AND_STEEL : Items.FIRE_CHARGE, InventoryUtils.HOTBAR_START, fireSwitch.getValue().equalsIgnoreCase("AltSwap") || fireSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);

                if (chargeSlot != -1) {
                    EUClient.ROTATION_MANAGER.packetRotate(yaw, 90);

                    InventoryUtils.switchSlot(fireSwitch.getValue(), chargeSlot, previousSlot);
                    NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(downPosition, 1), Direction.UP, downPosition, false), sequence));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    InventoryUtils.switchBack(fireSwitch.getValue(), chargeSlot, previousSlot);

                    didFireCharge = true;
                }
            }

            EUClient.ROTATION_MANAGER.packetRotate(yaw, pitch.getValue().intValue());

            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

            NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch.getValue().intValue()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

            if (didFireCharge && autoRemove.getValue()) {
                NetworkUtils.sendSequencedPacket(sequence -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mc.player.getBlockPos(), Direction.UP, sequence));
                NetworkUtils.sendSequencedPacket(sequence -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.getBlockPos(), Direction.UP, sequence));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(prevYaw, prevPitch, mc.player.isOnGround(), mc.player.horizontalCollision));

            setToggled(false);
        }

        if (mode.getValue().equalsIgnoreCase("Teleport")) {
            if (!mc.player.isOnGround()) {
                setToggled(false);
                return;
            }

            double[] diagonalOffset = MovementUtils.straightForward(0.44);
            boolean diagonal = mc.player.getYaw() % 90 > 35 && mc.player.getYaw() % 90 < 55;

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            if (diagonal) {
                double[] directionVec = MovementUtils.straightForward(0.51);

                int height = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), mc.player.getEyePos().add(diagonalOffset[0],0, diagonalOffset[1]), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType().equals(HitResult.Type.MISS) ? 1 : 2;

                mc.player.setPosition(mc.player.getX() + directionVec[0], mc.player.getY() + height, mc.player.getZ() + directionVec[1]);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));

                height = mc.world.isAir(BlockPos.ofFloored(mc.player.getPos().add(diagonalOffset[0], -2, diagonalOffset[1]))) ? 2 : 1;

                mc.player.setPosition(mc.player.getX() + directionVec[0], mc.player.getY() - height, mc.player.getZ() + directionVec[1]);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            } else {
                double[] directionVec = MovementUtils.straightForward(0.57);

                int height = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), mc.player.getEyePos().add(diagonalOffset[0],0, diagonalOffset[1]), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType().equals(HitResult.Type.MISS) ? 1 : 2;

                mc.player.setPosition(mc.player.getX() + directionVec[0], mc.player.getY() + height, mc.player.getZ() + directionVec[1]);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));

                mc.player.setPosition(mc.player.getX() + directionVec[0], mc.player.getY(), mc.player.getZ() + directionVec[1]);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));

                height = mc.world.isAir(BlockPos.ofFloored(mc.player.getPos().add(diagonalOffset[0], -2, diagonalOffset[1]))) ? 2 : 1;

                mc.player.setPosition(mc.player.getX() + directionVec[0], mc.player.getY() - height, mc.player.getZ() + directionVec[1]);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
            }

            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            setToggled(false);
        }
    }
}
