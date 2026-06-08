package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.*;
import eu.client.utils.miscellaneous.RenderPosition;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.ThreadExecutor;
import eu.client.utils.system.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "AutoBed", description = "Automatically places and breaks beds at enemies head.", category = Module.Category.COMBAT)
public class AutoBedModule extends Module {
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public BooleanSetting damageSync = new BooleanSetting("DamageSync", "Syncs the placing of the beds with the targets invincibility frames.", false);
    public NumberSetting speed = new NumberSetting("PlaceSpeed", "The speed at which beds will be placed.", new BooleanSetting.Visibility(damageSync, false), 10.0f, 0.1f, 10.0f);
    public NumberSetting hotbarSlot = new NumberSetting("HotbarSlot", "The slot to use for bed refilling.", 7, 0, 8);
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which beds will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which targets will be considered.", 8.0, 0.0, 16.0);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Rotates to the block you are placing the bed in.", true);
    public BooleanSetting airPlace = new BooleanSetting("AirPlace", "Lets you place beds on air.", false);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Checks if the target is in a hole or not before placing.", true);
    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private final Timer placeTimer = new Timer();
    private PlayerEntity target = null;
    private PlacePos placePos = null;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull() || mc.world.getDimension().bedWorks()) return;

        Runnable runnable = () -> {
            target = getTarget();
            if (target == null) return;

            placePos = getPlacePos(target);
            if (placePos == null) return;

            if (!damageSync.getValue() && !placeTimer.hasTimeElapsed(1000.0f - speed.getValue().floatValue() * 50.0f))
                return;
            if (damageSync.getValue() && target.hurtTime > 0) return;

            int bedSlot = findBed();
            boolean flag = mc.player.getInventory().getStack(hotbarSlot.getValue().intValue()).getItem() instanceof BedItem;

            if (bedSlot != -1 || flag) {
                Direction direction = WorldUtils.getDirection(placePos.pos, strictDirection.getValue());
                if (direction == null && !airPlace.getValue()) return;

                if (!flag) InventoryUtils.swap("Pickup", bedSlot, hotbarSlot.getValue().intValue());
                InventoryUtils.switchSlot("Normal", hotbarSlot.getValue().intValue(), hotbarSlot.getValue().intValue());

                placeBed(placePos.pos, direction, placePos.direction);

                placeTimer.reset();
            }
        };

        if (asynchronous.getValue()) ThreadExecutor.execute(runnable);
        else runnable.run();
    }

    private void placeBed(BlockPos pos, Direction direction, Direction rotation) {
        if(rotate.getValue()) EUClient.ROTATION_MANAGER.rotate(RotationUtils.getRotations(pos.toCenterPos()), this);

        EUClient.ROTATION_MANAGER.packetRotate(RotationUtils.getRotations(rotation));
        WorldUtils.placeBlock(pos, direction, Hand.MAIN_HAND, false, false, render.getValue());

        NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos, 1), Direction.DOWN, pos, false), sequence));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private PlacePos getPlacePos(PlayerEntity player) {
        PlacePos optimalPos = null;

        BlockPos playerPos = PositionUtils.getFlooredPosition(player).up();
        for(Direction direction : Direction.values()) {
            if(direction.getAxis().isVertical()) continue;
            BlockPos offsetPos = playerPos.offset(direction);

            if(mc.world.getBlockState(offsetPos).getBlock() == Blocks.AIR && !airPlace.getValue()) continue;
            if(!mc.world.getBlockState(offsetPos.up()).isReplaceable()) continue;
            if(mc.player.squaredDistanceTo(offsetPos.toCenterPos()) > MathHelper.square(range.getValue().doubleValue())) continue;

            if(optimalPos == null) {
                optimalPos = new PlacePos(offsetPos.up(), direction.getOpposite());
                continue;
            }

            if(mc.player.squaredDistanceTo(offsetPos.toCenterPos()) < mc.player.squaredDistanceTo(optimalPos.pos.toCenterPos())) {
                optimalPos = new PlacePos(offsetPos.up(), direction.getOpposite());
            }
        }

        return optimalPos;
    }

    private PlayerEntity getTarget() {
        PlayerEntity optimalTarget = null;
        for(PlayerEntity player : mc.world.getPlayers()) {
            if(player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue())) continue;
            if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (holeCheck.getValue() && !HoleUtils.isPlayerInHole(player)) continue;
            if(mc.world.getBlockState(PositionUtils.getFlooredPosition(player).up().up()).getBlock() != Blocks.AIR) continue;

            if(optimalTarget == null) {
                optimalTarget = player;
                continue;
            }

            if(mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget)) {
                optimalTarget = player;
            }
        }

        return optimalTarget;
    }

    private int findBed() {
        for(int i = 0; i < 36; i++) {
            if(mc.player.getInventory().getStack(i).isEmpty()) continue;
            Item item = mc.player.getInventory().getStack(i).getItem();
            if(item instanceof BedItem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getMetaData() {
        return target == null ? "None" : target.getName().getString();
    }

    private record PlacePos(BlockPos pos, Direction direction) { }
}
