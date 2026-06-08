package eu.client.modules.impl.combat;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.UpdateMovementEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.EntityUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.system.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "KillAura", description = "Automatically attacks entities if they meet specified requirements.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class KillAuraModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to a weapon.", "Require", new String[]{"None", "Normal", "Require"});
    public ModeSetting hitDelay = new ModeSetting("HitDelay", "The delay that will have to be waited out between attacks.", "Vanilla", new String[]{"None", "Vanilla", "Custom"});
    public NumberSetting speed = new NumberSetting("Speed", "The speed at which the target will be attacked.", new ModeSetting.Visibility(hitDelay, "Custom"), 20.0f, 0.1f, 20.0f);
    public ModeSetting rotate = new ModeSetting("Rotate", "Automatically rotates to the target whenever attacking.", "Hold", new String[]{"None", "Normal", "Hold", "Packet"});
    public ModeSetting swing = new ModeSetting("Swing", "", "Mainhand", new String[]{"None", "Packet", "Mainhand", "Offhand", "Both"});
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which entities can be at in order to be a valid target.", 6.0, 0.0, 12.0);
    public BooleanSetting raytrace = new BooleanSetting("Raytrace", "Only attacks entities that you can see and aren't going through walls.", false);
    public NumberSetting wallsRange = new NumberSetting("WallsRange", "The maximum distance through walls at which entities can be at in order to be a valid target.", new BooleanSetting.Visibility(raytrace, false), 5.0, 0.0, 12.0);
    public NumberSetting ticksExisted = new NumberSetting("TicksExisted", "The amount of ticks that entities have to have lived for before attacking.", 50, 0, 240);

    public CategorySetting entitiesCategory = new CategorySetting("Entities", "Contains all of the settings relating to which entities should be attacked.");
    public BooleanSetting players = new BooleanSetting("Players", "Automatically attacks player entities.", new CategorySetting.Visibility(entitiesCategory), true);
    public BooleanSetting friends = new BooleanSetting("Friends", "Automatically attacks player entities that you have friended.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting animals = new BooleanSetting("Animals", "Automatically attacks animal entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting hostiles = new BooleanSetting("Hostiles", "Automatically attacks hostile entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting passives = new BooleanSetting("Passives", "Automatically attacks passive entities.", new BooleanSetting.Visibility(hostiles, true), false);
    public BooleanSetting ambient = new BooleanSetting("Ambient", "Automatically attacks ambient entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting invisibles = new BooleanSetting("Invisibles", "Automatically attacks invisible entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting boats = new BooleanSetting("Boats", "Automatically attacks boat entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting shulkerBullets = new BooleanSetting("ShulkerBullets", "Automatically attacks shulker bullets.", new CategorySetting.Visibility(entitiesCategory), true);

    public CategorySetting renderCategory = new CategorySetting("Render", "Contains all of the settings relating to target rendering.");
    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to the target entity.", new CategorySetting.Visibility(renderCategory), "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(mode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(mode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    @Getter public Entity target;

    private final Timer timer = new Timer();

    private boolean attacking = false;
    private boolean shouldAttack = false;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        Iterable<Entity> entities = mc.world.getOtherEntities(mc.player, PositionUtils.getRadius(mc.player, Math.max(range.getValue().doubleValue(), wallsRange.getValue().doubleValue()) + 1.0), entity ->
                entity.isAlive()
                        && (!(entity instanceof LivingEntity livingEntity) || livingEntity.getHealth() > 0.0f)
                        && entity.age >= ticksExisted.getValue().intValue()
                        && entity.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) < MathHelper.square(range.getValue().doubleValue())
                        && mc.world.getWorldBorder().contains(entity.getBlockPos())
                        && (friends.getValue() || !EUClient.FRIEND_MANAGER.contains(entity.getName().getString()))
                        && isValidEntity(entity)
                        && (WorldUtils.canSee(entity) || (!raytrace.getValue() && entity.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) < MathHelper.square(wallsRange.getValue().doubleValue()))));

        Entity optimalEntity = null;
        for (Entity entity : entities) {
            if (optimalEntity == null) {
                optimalEntity = entity;
                continue;
            }

            if (mc.player.squaredDistanceTo(entity) < mc.player.squaredDistanceTo(optimalEntity)) {
                optimalEntity = entity;
            }
        }

        target = optimalEntity;

        attacking = false;
        shouldAttack = false;

        if (target == null) return;

        if (autoSwitch.getValue().equalsIgnoreCase("Require") && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) return;

        int slot = InventoryUtils.findBestSword(InventoryUtils.HOTBAR_START, InventoryUtils.HOTBAR_END);
        if (autoSwitch.getValue().equalsIgnoreCase("Normal") && slot == -1) return;

        attacking = true;

        // Skip actions on client when proxy handles them
        if (shouldSkipActions()) return;

        if (rotate.getValue().equalsIgnoreCase("Hold")) EUClient.ROTATION_MANAGER.rotate(RotationUtils.getRotations(target), this);

        if (hitDelay.getValue().equalsIgnoreCase("Vanilla") && mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (hitDelay.getValue().equalsIgnoreCase("Custom") && !timer.hasTimeElapsed(1000.0f - speed.getValue().floatValue() * 50.0f))
            return;

        if (rotate.getValue().equalsIgnoreCase("Normal")) EUClient.ROTATION_MANAGER.rotate(RotationUtils.getRotations(target), this);

        shouldAttack = true;
    }

    @SubscribeEvent
    public void onUpdateMovement$POST(UpdateMovementEvent.Post event) {
        if (shouldSkipActions()) return;
        if (mc.player == null || mc.world == null || !shouldAttack || !attacking || target == null) {
            shouldAttack = false;
            return;
        }

        if (rotate.getValue().equalsIgnoreCase("Packet")) EUClient.ROTATION_MANAGER.packetRotate(RotationUtils.getRotations(target));

        if (autoSwitch.getValue().equalsIgnoreCase("Normal")) InventoryUtils.switchSlot("Normal", InventoryUtils.findBestSword(InventoryUtils.HOTBAR_START, InventoryUtils.HOTBAR_END), mc.player.getInventory().selectedSlot);
        mc.interactionManager.attackEntity(mc.player, target);

        switch (swing.getValue()) {
            case "Packet" -> mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            case "Mainhand" -> mc.player.swingHand(Hand.MAIN_HAND);
            case "Offhand" -> mc.player.swingHand(Hand.OFF_HAND);
            case "Both" -> {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.swingHand(Hand.OFF_HAND);
            }
        }

        shouldAttack = false;
        timer.reset();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (target == null || !attacking) return;
        if (mode.getValue().equalsIgnoreCase("None")) return;

        Vec3d vec3d = EntityUtils.getRenderPos(target, event.getTickDelta());
        Box box = new Box(vec3d.x - target.getBoundingBox().getLengthX() / 2, vec3d.y, vec3d.z - target.getBoundingBox().getLengthZ() / 2, vec3d.x + target.getBoundingBox().getLengthX() / 2, vec3d.y + target.getBoundingBox().getLengthY(), vec3d.z + target.getBoundingBox().getLengthZ() / 2);

        if (mode.getValue().equalsIgnoreCase("Fill") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBox(event.getMatrices(), box, fillColor.getColor());
        if (mode.getValue().equalsIgnoreCase("Outline") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBoxOutline(event.getMatrices(), box, outlineColor.getColor());
    }

    @Override
    public String getMetaData() {
        return target == null ? "None" : target.getName().getString();
    }

    private boolean isValidEntity(Entity entity) {
        if (players.getValue() && entity.getType() == EntityType.PLAYER) return true;
        if (hostiles.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) {
            if(!passives.getValue() && entity instanceof EndermanEntity enderman && !enderman.isAngry()) return false;
            if(!passives.getValue() && entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) return false;
            return true;
        }
        if (animals.getValue() && (entity.getType().getSpawnGroup() == SpawnGroup.CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_AMBIENT || entity.getType().getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.AXOLOTLS))
            return true;
        if (ambient.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.AMBIENT) return true;
        if (invisibles.getValue() && entity.isInvisible()) return true;
        if (boats.getValue() && entity instanceof BoatEntity) return true;
        return shulkerBullets.getValue() && entity.getType() == EntityType.SHULKER_BULLET;
    }
}
