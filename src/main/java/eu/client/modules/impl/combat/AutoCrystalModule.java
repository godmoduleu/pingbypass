package eu.client.modules.impl.combat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.player.SpeedMineModule;
import eu.client.settings.impl.*;

import eu.client.utils.minecraft.DamageUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.Counter;
import eu.client.utils.system.Timer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RegisterModule(name = "AutoCrystal", description = "Automatically places and attacks crystals to annihilate your opponents.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class AutoCrystalModule extends Module {
    public CategorySetting attackCategory = new CategorySetting("Attack",
            "The category for settings related to attacking crystals.");
    public BooleanSetting attack = new BooleanSetting("Attack", "Enabled",
            "Automatically attacks crystals that are deemed safe.", new CategorySetting.Visibility(attackCategory),
            true);
    public NumberSetting attackSpeed = new NumberSetting("AttackSpeed", "Speed",
            "The speed at which crystals will be attacked.", new CategorySetting.Visibility(attackCategory), 20.0f,
            0.1f, 20.0f);
    public NumberSetting attackRange = new NumberSetting("AttackRange", "Range",
            "The maximum distance at which crystals will be attacked.", new CategorySetting.Visibility(attackCategory),
            4.5, 0.0, 8.0);
    public NumberSetting attackWallsRange = new NumberSetting("AttackWallsRange", "WallsRange",
            "The maximum distance at which crystals will be attacked through walls.",
            new CategorySetting.Visibility(attackCategory), 4.5, 0.0, 8.0);
    public ModeSetting antiWeakness = new ModeSetting("AntiWeakness", "Allows you to attack crystals when weaknessed.",
            new CategorySetting.Visibility(attackCategory), "None", new String[] { "None", "Normal", "Silent" });
    public BooleanSetting instant = new BooleanSetting("Instant", "Instantly attacks crystals once they spawn.",
            new CategorySetting.Visibility(attackCategory), true);
    public BooleanSetting inhibit = new BooleanSetting("Inhibit",
            "Prevents excessive attacks on crystals by blacklisting crystals when attacking them.",
            new CategorySetting.Visibility(attackCategory), true);

    public CategorySetting placeCategory = new CategorySetting("Place",
            "The category for settings related to placing crystals.");
    public BooleanSetting place = new BooleanSetting("Place", "Enabled",
            "Automatically places crystals on positions that are deemed safe and lethal enough.",
            new CategorySetting.Visibility(placeCategory), true);
    public NumberSetting placeSpeed = new NumberSetting("PlaceSpeed", "Speed",
            "The speed at which crystals will be placed.", new CategorySetting.Visibility(placeCategory), 20.0f, 0.1f,
            20.0f);
    public NumberSetting placeRange = new NumberSetting("PlaceRange", "Range",
            "The maximum distance at which positions will be placed on.", new CategorySetting.Visibility(placeCategory),
            4.5, 0.0, 8.0);
    public NumberSetting placeWallsRange = new NumberSetting("PlaceWallsRange", "WallsRange",
            "The maximum distance at which positions will be placed on through walls.",
            new CategorySetting.Visibility(placeCategory), 4.5, 0.0, 8.0);
    public ModeSetting placements = new ModeSetting("Placements",
            "The version of the game that will be used for crystal placement calculations.",
            new CategorySetting.Visibility(placeCategory), "Native", new String[] { "Native", "Protocol" });
    public BooleanSetting blockDestruction = new BooleanSetting("BlockDestruction",
            "Places crystals on top of mined blocks in order to damage opponents.",
            new CategorySetting.Visibility(placeCategory), true);
    public ModeSetting autoSwitch = new ModeSetting("Switch",
            "Automatically switches to a crystal if you aren't currently holding one.",
            new CategorySetting.Visibility(placeCategory), "None",
            new String[] { "None", "Normal", "Silent", "AltSwap" });

    public CategorySetting miscellaneousCategory = new CategorySetting("Miscellaneous",
            "The category for all miscellaneous settings.");
    public ModeSetting sequential = new ModeSetting("Sequential",
            "The sequence that the module's processes will be run in.",
            new CategorySetting.Visibility(miscellaneousCategory), "Strong",
            new String[] { "None", "Strict", "Strong" });
    public ModeSetting rotate = new ModeSetting("Rotate",
            "Automatically rotates to the crystal whenever attacking or placing.",
            new CategorySetting.Visibility(miscellaneousCategory), "Normal",
            new String[] { "None", "Normal", "Packet" });
    public ModeSetting swing = new ModeSetting("Swing", "The hand that will be used for swinging.",
            new CategorySetting.Visibility(miscellaneousCategory), "Default",
            new String[] { "Default", "None", "Packet", "Mainhand", "Offhand", "Both" });
    public BooleanSetting yawStep = new BooleanSetting("YawStep", "Performs your rotations over multiple ticks.",
            new CategorySetting.Visibility(miscellaneousCategory), false);
    public NumberSetting yawStepThreshold = new NumberSetting("YawStepThreshold", "Threshold",
            "The threshold in order for yaw to be modified.", new BooleanSetting.Visibility(yawStep, true), 75, 1, 180);
    public BooleanSetting raytrace = new BooleanSetting("Raytrace",
            "Avoids attacking or placing any crystals through walls.",
            new CategorySetting.Visibility(miscellaneousCategory), false);
    public NumberSetting extrapolation = new NumberSetting("Extrapolation",
            "Extrapolates the target's position to calculate positions ahead of time.",
            new CategorySetting.Visibility(miscellaneousCategory), 0, 0, 20);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange",
            "The maximum distance at which enemies can be at.", new CategorySetting.Visibility(miscellaneousCategory),
            10.0, 0.0, 24.0);
    public BooleanSetting chestBreak = new BooleanSetting("ChestBreak",
            "Prevents other players from getting obsidian from ender chests by destroying the dropped items.",
            new CategorySetting.Visibility(miscellaneousCategory), false);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous",
            "Performs calculations on separate threads.", new CategorySetting.Visibility(miscellaneousCategory), true);
    public BooleanSetting gameLoop = new BooleanSetting("GameLoop", "Runs the module on loop instead of ticks.",
            new CategorySetting.Visibility(miscellaneousCategory), false);
    public NumberSetting loopDelay = new NumberSetting("LoopDelay",
            "The delay that has to be waited out before running the module again.",
            new BooleanSetting.Visibility(gameLoop, true), 50, 0, 1000);
    public ModeSetting whileEating = new ModeSetting("WhileEating",
            "Places and attacks crystal while eating or using items.",
            new CategorySetting.Visibility(miscellaneousCategory), "Both",
            new String[] { "None", "Attack", "Place", "Both" });

    public CategorySetting predictionCategory = new CategorySetting("Prediction",
            "The category for settings related to attack prediction.");
    public BooleanSetting godSync = new BooleanSetting("GodSync",
            "Makes the attacking way faster by predicting entity IDs.",
            new CategorySetting.Visibility(predictionCategory), false);
    public NumberSetting predictions = new NumberSetting("Predictions",
            "The amount of predictions that will be done after placing.",
            new CategorySetting.Visibility(predictionCategory), 10, 1, 20);
    public NumberSetting offset = new NumberSetting("Offset", "The amount that the last entity ID should be offset by.",
            new CategorySetting.Visibility(predictionCategory), 0, 0, 2);
    public ModeSetting godSwing = new ModeSetting("GodSwing", "Swing",
            "The swinging that will be done for each predicted attack.",
            new CategorySetting.Visibility(predictionCategory), "Normal", new String[] { "None", "Normal", "Strict" });
    public BooleanSetting fast = new BooleanSetting("Fast",
            "Improves the speed of the prediction calculations at the cost of stability.",
            new CategorySetting.Visibility(predictionCategory), false);
    public BooleanSetting antiKick = new BooleanSetting("AntiKick",
            "Prevents you from getting kicked by attacking invalid entity IDs.",
            new CategorySetting.Visibility(predictionCategory), false);
    public NumberSetting kickThreshold = new NumberSetting("KickThreshold", "Threshold",
            "The tick threshold for the kick prevention.", new BooleanSetting.Visibility(antiKick, true), 5, 1, 10);

    public CategorySetting facePlaceCategory = new CategorySetting("Faceplace",
            "The category for settings relating to faceplacing.");
    public ModeSetting facePlaceMode = new ModeSetting("FaceplaceMode", "Mode",
            "The checks that will be done in order to faceplace.", new CategorySetting.Visibility(facePlaceCategory),
            "Dynamic", new String[] { "None", "Dynamic", "Always" });
    public ModeSetting facePlaceSpeed = new ModeSetting("FaceplaceSpeed", "Speed",
            "The speed that players will be faceplaced at.", new CategorySetting.Visibility(facePlaceCategory),
            "Normal", new String[] { "Normal", "Custom" });
    public NumberSetting facePlaceDelay = new NumberSetting("FaceplaceDelay", "Delay",
            "The ticks that have to be waited for before faceplacing again.",
            new ModeSetting.Visibility(facePlaceSpeed, "Custom"), 11, 0, 20);
    public BooleanSetting healthPlace = new BooleanSetting("HealthPlace",
            "Whether or not to faceplace when the target's health is low.",
            new ModeSetting.Visibility(facePlaceMode, "Dynamic"), true);
    public NumberSetting health = new NumberSetting("Health",
            "The health that the target needs to be at in order for the module to start faceplacing.",
            new BooleanSetting.Visibility(healthPlace, true), 8.0f, 0.0f, 36.0f);
    public BooleanSetting armorPlace = new BooleanSetting("ArmorPlace",
            "Whether or not to faceplace when the target's armor is low on durability.",
            new ModeSetting.Visibility(facePlaceMode, "Dynamic"), true);
    public NumberSetting percentage = new NumberSetting("Percentage",
            "The percentage that one of the target's armor pieces need to be at in order to start faceplacing.",
            new BooleanSetting.Visibility(armorPlace, true), 10, 1, 100);

    public CategorySetting damageCategory = new CategorySetting("Damage",
            "The category for settings related to damage calculations.");
    public NumberSetting minimumDamage = new NumberSetting("MinimumDamage", "Minimum",
            "The minimum damage that has to be dealt to enemies.", new CategorySetting.Visibility(damageCategory), 6.0,
            0.0, 36.0);
    public NumberSetting maximumSelfDamage = new NumberSetting("MaximumSelfDamage", "MaximumSelf",
            "The maximum damage that can be dealt to you by crystals.", new CategorySetting.Visibility(damageCategory),
            10.0, 0.0, 36.0);
    public NumberSetting lethalMultiplier = new NumberSetting("LethalMultiplier",
            "The amount of crystals that the target has to be killed by in order to ignore minimum damage.",
            new CategorySetting.Visibility(damageCategory), 1.5f, 0.0f, 4.0f);
    public BooleanSetting antiSuicide = new BooleanSetting("AntiSuicide",
            "Prevents crystals from accidentally killing you when you're low on health.",
            new CategorySetting.Visibility(damageCategory), true);
    public BooleanSetting ignoreTerrain = new BooleanSetting("IgnoreTerrain",
            "Ignores terrain that can be destroyed when calculating damage.",
            new CategorySetting.Visibility(damageCategory), true);

    public CategorySetting renderCategory = new CategorySetting("Render",
            "Contains all of the settings relating to position rendering.");
    public ModeSetting animationMode = new ModeSetting("Animation",
            "The animation that will be applied to the rendering.", new CategorySetting.Visibility(renderCategory),
            "Static", new String[] { "Static", "Slide" });
    public ModeSetting mode = new ModeSetting("Mode", "The mode for the auto crystal render.",
            new CategorySetting.Visibility(renderCategory), "Fade", new String[] { "Fade", "Shrink" });
    public NumberSetting duration = new NumberSetting("Duration", "The duration for the place render.",
            new CategorySetting.Visibility(renderCategory), 300, 0, 1000);
    public NumberSetting slideSmoothness = new NumberSetting("Smoothness",
            "The smoothness for the slide while place position is changing.",
            new CategorySetting.Visibility(renderCategory), 1, 0, 20);
    public ModeSetting renderMode = new ModeSetting("RenderMode",
            "The rendering that will be applied to the target position.",
            new CategorySetting.Visibility(renderCategory), "Both", new String[] { "None", "Fill", "Outline", "Both" });
    public BooleanSetting renderDamage = new BooleanSetting("RenderDamage", "Damage",
            "Renders the damage that the position will do to the opponent.",
            new CategorySetting.Visibility(renderCategory), false);
    public BooleanSetting icon = new BooleanSetting("Icon",
            "Renders a customizable crystal icon on the rendered position.",
            new CategorySetting.Visibility(renderCategory), false);
    public NumberSetting iconScale = new NumberSetting("IconScale",
            "The scaling that will be applied to the crystal icon rendering.",
            new BooleanSetting.Visibility(icon, true), 3, 1, 5);
    public NumberSetting iconRadius = new NumberSetting("IconRadius",
            "The difference between the outer circle and the inner circle.", new BooleanSetting.Visibility(icon, true),
            2.0f, 0.0f, 5.0f);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Runnable attackRunnable = null;
    private Runnable placeRunnable = null;

    private final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> placedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> countedCrystals = new ConcurrentHashMap<>();

    private final Timer attackTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer facePlaceTimer = new Timer();
    private final Timer loopTimer = new Timer();

    private boolean sequenceAttack = false;
    private boolean sequencePlace = true;

    private boolean attackedSequentially = false;
    private boolean placedSequentially = false;

    @Getter
    private PlayerEntity target = null;

    private EndCrystalEntity attackTarget = null;
    private PlaceTarget placeTarget = null;
    private PlaceTarget mineTarget = null;

    private String calculationTime = "0.00ms";
    private int calculationCount = 0;
    @Getter
    private String calculationDamage = "0.00";

    private final Counter crystalCounter = new Counter();
    private int crystalsPerSecond = 0;

    private int highestID = -100000;
    private int kickTicks = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        attackedCrystals.entrySet().removeIf(
                entry -> System.currentTimeMillis() - entry.getValue() > Pingbypass.SERVER_MANAGER.getPing() * 2L);
        placedCrystals.entrySet().removeIf(
                entry -> System.currentTimeMillis() - entry.getValue() > Pingbypass.SERVER_MANAGER.getPing() * 2L
                        + (20 - attackSpeed.getValue().floatValue()) * 50L);
        countedCrystals.entrySet().removeIf(
                entry -> System.currentTimeMillis() - entry.getValue() > Pingbypass.SERVER_MANAGER.getPing() * 2L);

        crystalsPerSecond = crystalCounter.getCount();

        Runnable runnable = () -> {
            long startTime = System.nanoTime();

            attackTarget = calculateCrystals();
            placeTarget = calculatePlacements(null);

            calculationTime = new DecimalFormat("0.00").format((System.nanoTime() - startTime) / 1000000.0) + "ms";
            calculationCount = placeTarget == null ? 0 : placeTarget.getCalculations();
            calculationDamage = placeTarget == null ? "0.00"
                    : new DecimalFormat("0.00").format(placeTarget.getDamage());

            target = placeTarget == null ? null : placeTarget.getPlayer();

            if (blockDestruction.getValue() && asynchronous.getValue()) {
                SpeedMineModule module = Pingbypass.MODULE_MANAGER.getModule(SpeedMineModule.class);
                BlockPos position = null;

                if (module.getPrimary() != null && module.getPrimary().isMining())
                    position = module.getPrimary().getPosition();
                if (position != null)
                    mineTarget = calculatePlacements(position);
            }
        };

        if (asynchronous.getValue())
            executor.submit(runnable);
        else
            runnable.run();

        if (gameLoop.getValue())
            return;

        run();
    }

    @SubscribeEvent
    public void onGameLoop(GameLoopEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;
        if (!gameLoop.getValue())
            return;
        if (!loopTimer.hasTimeElapsed(loopDelay.getValue().longValue()))
            return;

        loopTimer.reset();
        run();

        if (attackRunnable != null) {
            attackRunnable.run();
            attackRunnable = null;
        }

        if (placeRunnable != null) {
            placeRunnable.run();
            placeRunnable = null;
        }
    }

    private void run() {
        attackRunnable = null;
        placeRunnable = null;

        if (sequential.getValue().equalsIgnoreCase("None")) {
            if (sequenceAttack) {
                sequenceAttack = false;
                sequencePlace = true;

                attackCrystals();
                return;
            }

            if (sequencePlace) {
                sequenceAttack = true;
                sequencePlace = false;

                placeCrystals(false);
            }
        } else {
            if (attack.getValue())
                attackCrystals();
            if (place.getValue())
                placeCrystals(false);
        }
    }

    @SubscribeEvent
    public void onUpdateMovement$POST(UpdateMovementEvent.Post event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (attackRunnable != null)
            attackRunnable.run();
        if (placeRunnable != null)
            placeRunnable.run();
    }

    @SubscribeEvent
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (!attack.getValue() || !instant.getValue())
            return;
        if (!attackTimer.hasTimeElapsed(1000.0f - attackSpeed.getValue().floatValue() * 50.0f))
            return;

        if (!(event.getEntity() instanceof EndCrystalEntity crystal))
            return;

        if (inhibit.getValue() && attackedCrystals.containsKey(crystal.getId()))
            return;
        if (!placedCrystals.containsKey(crystal.getBlockPos().down()))
            return;
        if (crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                .square(attackRange.getValue().doubleValue()))
            return;
        if (!mc.world.getWorldBorder().contains(crystal.getBlockPos()))
            return;
        if (!WorldUtils.canSee(crystal)
                && (raytrace.getValue() || crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                        .square(attackWallsRange.getValue().doubleValue())))
            return;

        if (rotate.getValue().equalsIgnoreCase("Packet"))
            Pingbypass.ROTATION_MANAGER
                    .packetRotate(RotationUtils.getRotations(Vec3d.ofCenter(crystal.getBlockPos(), 0)));
        if (rotate.getValue().equalsIgnoreCase("Normal"))
            Pingbypass.ROTATION_MANAGER.rotate(calculateRotations(Vec3d.ofCenter(crystal.getBlockPos(), 0)),
                    Pingbypass.ROTATION_MANAGER.getModulePriority(this));

        attack(crystal);

        attackedSequentially = true;
        if (sequential.getValue().equalsIgnoreCase("Strong")) {
            placeCrystals(true);
        }
    }

    @SubscribeEvent
    public void onDestroyBlock(DestroyBlockEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        kickTicks = 0;

        if (!blockDestruction.getValue())
            return;
        if (!placeTimer.hasTimeElapsed(1000.0f - placeSpeed.getValue().floatValue() * 50.0f))
            return;

        BlockPos minedPosition = event.getPosition();
        if (minedPosition == null)
            return;

        int slot = InventoryUtils.findHotbar(Items.END_CRYSTAL);
        int previousSlot = mc.player.getInventory().selectedSlot;
        boolean switched = false;

        if (!autoSwitch.getValue().equalsIgnoreCase("None") && slot == -1
                && (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
                        && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL))
            return;

        PlaceTarget mineTarget = this.mineTarget == null ? null : this.mineTarget.clone();
        if (mineTarget == null
                || (mineTarget.getPosition() != null && !minedPosition.equals(mineTarget.getException())))
            mineTarget = calculatePlacements(minedPosition);
        if (mineTarget == null || mineTarget.getPosition() == null) {

            return;
        }

        BlockPos position = mineTarget.getPosition();

        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                .square(placeRange.getValue().doubleValue()))
            return;
        if (!WorldUtils.canSee(position) && (raytrace.getValue()
                || mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                        .square(placeWallsRange.getValue().doubleValue())))
            return;

        if (rotate.getValue().equalsIgnoreCase("Normal"))
            Pingbypass.ROTATION_MANAGER.rotate(calculateRotations(Vec3d.ofCenter(position, 1)), this,
                    Pingbypass.ROTATION_MANAGER.getModulePriority(this) + 1);
        if (!rotate.getValue().equalsIgnoreCase("None"))
            Pingbypass.ROTATION_MANAGER.packetRotate(RotationUtils.getRotations(Vec3d.ofCenter(position, 1)));

        for (Entity entity : mc.world.getOtherEntities(null, new Box(position.up())).stream()
                .filter(entity -> entity instanceof EndCrystalEntity).toList()) {
            if (!rotate.getValue().equalsIgnoreCase("None"))
                Pingbypass.ROTATION_MANAGER.packetRotate(RotationUtils.getRotations(entity));

            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            break;
        }

        SpeedMineModule module = Pingbypass.MODULE_MANAGER.getModule(SpeedMineModule.class);
        boolean flag = module.switchReset.getValue() && (module.switchMode.getValue().equalsIgnoreCase("Normal")
                || module.switchMode.getValue().equalsIgnoreCase("AltSwap")
                || module.switchMode.getValue().equalsIgnoreCase("AltPickup"));

        if (!autoSwitch.getValue().equalsIgnoreCase("None")
                && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
                && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            InventoryUtils.switchSlot(flag ? "AltSwap" : autoSwitch.getValue(), slot, previousSlot);
            switched = true;
        }

        place(position);

        if (switched) {
            InventoryUtils.switchBack(flag ? "AltSwap" : autoSwitch.getValue(), slot, previousSlot);
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (event.getPacket() instanceof EntitySpawnS2CPacket packet) {
            if (packet.getEntityId() > highestID)
                highestID = packet.getEntityId();

            BlockPos position = BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ()).add(0, -1, 0);
            if (countedCrystals.containsKey(position)) {
                if (facePlaceTimer.hasTimeElapsed(facePlaceDelay.getValue().longValue() * 50L))
                    facePlaceTimer.reset();

                countedCrystals.remove(position);

                crystalCounter.increment();
                crystalsPerSecond = crystalCounter.getCount();
            }
        }

        if (event.getPacket() instanceof ExperienceOrbSpawnS2CPacket packet) {
            if (packet.getEntityId() > highestID) {
                highestID = packet.getEntityId();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (shouldRunOnProxy())
            return;
        kickTicks = 0;
    }

    @SubscribeEvent
    public void onClientConnect(ClientConnectEvent event) {
        if (shouldRunOnProxy())
            return;
        highestID = -100000;
    }

    @Override
    public void onDisable() {
        attackRunnable = null;
        placeRunnable = null;

        attackedCrystals.clear();
        placedCrystals.clear();
        countedCrystals.clear();

        attackedSequentially = false;
        placedSequentially = false;

        target = null;
        placeTarget = null;
        mineTarget = null;

        calculationTime = "0.00ms";
        calculationCount = 0;
        calculationDamage = "0.00";

        crystalCounter.reset();

        highestID = -100000;
    }

    @Override
    public String getMetaData() {
        return calculationTime + ", " + calculationCount + ", " + calculationDamage + ", " + crystalsPerSecond;
    }

    private void attackCrystals() {
        EndCrystalEntity overrideCrystal = null;

        PlaceTarget pt = this.placeTarget;
        boolean flag = pt != null && pt.obstructions != null && !pt.obstructions.isEmpty();
        for (Entity entity : flag ? pt.obstructions : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal))
                continue;
            if (!crystal.isAlive())
                continue;
            if (inhibit.getValue() && attackedCrystals.containsKey(entity.getId()))
                continue;
            if (!flag && !placedCrystals.containsKey(crystal.getBlockPos().down()))
                continue;
            if (crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                    .square(attackRange.getValue().doubleValue()))
                continue;
            if (!mc.world.getWorldBorder().contains(crystal.getBlockPos()))
                continue;
            if (!WorldUtils.canSee(crystal) && (raytrace.getValue()
                    || crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                            .square(attackWallsRange.getValue().doubleValue())))
                continue;

            overrideCrystal = crystal;
            break;
        }

        EndCrystalEntity crystal = overrideCrystal == null ? attackTarget : overrideCrystal;
        if (crystal == null)
            return;

        if (rotate.getValue().equalsIgnoreCase("Normal"))
            Pingbypass.ROTATION_MANAGER.rotate(calculateRotations(Vec3d.ofCenter(crystal.getBlockPos(), 0)),
                    Pingbypass.ROTATION_MANAGER.getModulePriority(this));

        if (!attackTimer.hasTimeElapsed(1000.0f - attackSpeed.getValue().floatValue() * 50.0f)
                || attackedSequentially) {
            if (attackedSequentially)
                attackedSequentially = false;
            return;
        }

        Entity entity = mc.world.getEntityById(crystal.getId());
        if (entity == null)
            return;

        if (!(entity instanceof EndCrystalEntity endCrystal))
            return;
        if (!endCrystal.isAlive())
            return;
        if (inhibit.getValue() && attackedCrystals.containsKey(entity.getId()))
            return;
        if (endCrystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                .square(attackRange.getValue().doubleValue()))
            return;
        if (!mc.world.getWorldBorder().contains(endCrystal.getBlockPos()))
            return;
        if (!WorldUtils.canSee(endCrystal) && (raytrace.getValue()
                || endCrystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                        .square(attackWallsRange.getValue().doubleValue())))
            return;

        attackRunnable = () -> {
            if (rotate.getValue().equalsIgnoreCase("Packet"))
                Pingbypass.ROTATION_MANAGER
                        .packetRotate(RotationUtils.getRotations(Vec3d.ofCenter(crystal.getBlockPos(), 0)));

            attack(crystal);
        };
    }

    private void placeCrystals(boolean sequential) {
        PlaceTarget placeTarget = this.placeTarget == null ? null : this.placeTarget.clone();
        if (placeTarget == null || placeTarget.getPosition() == null) {

            return;
        }

        int slot = InventoryUtils.findHotbar(Items.END_CRYSTAL);
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (!autoSwitch.getValue().equalsIgnoreCase("None") && slot == -1
                && (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
                        && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL))
            return;

        BlockPos position = placeTarget.getPosition();

        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                .square(placeRange.getValue().doubleValue()))
            return;
        if (!mc.world.getWorldBorder().contains(position))
            return;
        if (mc.world.getBlockState(position).getBlock() != Blocks.OBSIDIAN
                && mc.world.getBlockState(position).getBlock() != Blocks.BEDROCK)
            return;
        if (!mc.world.getBlockState(position.add(0, 1, 0)).isAir()
                || (placements.getValue().equalsIgnoreCase("Protocol")
                        && !mc.world.getBlockState(position.add(0, 2, 0)).isAir()))
            return;
        if (!WorldUtils.canSee(position) && (raytrace.getValue()
                || mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                        .square(placeWallsRange.getValue().doubleValue())))
            return;
        if (mc.world.getOtherEntities(null, new Box(position.add(0, 1, 0))).stream().anyMatch(entity -> entity.isAlive()
                && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof EndCrystalEntity)))
            return;

        if (rotate.getValue().equalsIgnoreCase("Normal"))
            Pingbypass.ROTATION_MANAGER.rotate(calculateRotations(Vec3d.ofCenter(position, 1)),
                    Pingbypass.ROTATION_MANAGER.getModulePriority(this));

        if (!placeTimer.hasTimeElapsed(1000.0f - placeSpeed.getValue().floatValue() * 50.0f))
            return;
        if (!sequential && placedSequentially) {
            placedSequentially = false;
            return;
        }

        placeRunnable = () -> {
            boolean switched = false;

            if (rotate.getValue().equalsIgnoreCase("Packet"))
                Pingbypass.ROTATION_MANAGER.packetRotate(RotationUtils.getRotations(Vec3d.ofCenter(position, 1)));

            if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
                    && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);
                switched = true;
            }

            place(position);

            if (switched) {
                InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);
            }

            if (godSync.getValue()) {
                boolean flag = !antiKick.getValue()
                        || !(mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem)
                                && !(mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem);
                if ((!antiKick.getValue() || kickTicks > kickThreshold.getValue().intValue()) && flag) {
                    if (!fast.getValue()) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity.getId() <= highestID)
                                continue;
                            highestID = entity.getId();
                        }
                    }

                    for (int i = 1 - offset.getValue().intValue(); i < predictions.getValue().intValue(); ++i) {
                        Entity entity = mc.world.getEntityById(highestID);
                        if (entity == null || entity instanceof EndCrystalEntity) {
                            int id = highestID + i;

                            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(mc.player,
                                    mc.player.isSneaking());
                            try {
                                Field field = PlayerInteractEntityC2SPacket.class.getDeclaredField(
                                        FabricLoader.getInstance().isDevelopmentEnvironment() ? "entityId"
                                                : "field_12870");
                                field.setAccessible(true);
                                field.setInt(packet, id);
                            } catch (Exception ignored) {
                            }

                            mc.getNetworkHandler().sendPacket(packet);
                            if (godSwing.getValue().equals("Strict"))
                                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                            attackedCrystals.put(id, System.currentTimeMillis());
                        }
                    }

                    if (godSwing.getValue().equals("Normal"))
                        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }

                kickTicks++;
            }
        };

        if (sequential) {
            placeRunnable.run();
            placeRunnable = null;

            placedSequentially = true;
        }
    }

    private EndCrystalEntity calculateCrystals() {
        if (!attack.getValue())
            return null;
        if (shouldPause("Attack"))
            return null;

        List<PlayerEntity> players = getPlayers();
        if (players.isEmpty())
            return null;

        EndCrystalEntity optimalCrystal = null;
        float optimalDamage = 0.0f;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal))
                continue;
            if (!crystal.isAlive())
                continue;
            if (inhibit.getValue() && attackedCrystals.containsKey(entity.getId()))
                continue;
            if (crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                    .square(attackRange.getValue().doubleValue()))
                continue;
            if (!mc.world.getWorldBorder().contains(crystal.getBlockPos()))
                continue;
            if (!WorldUtils.canSee(crystal) && (raytrace.getValue()
                    || crystal.getBoundingBox().squaredMagnitude(mc.player.getEyePos()) > MathHelper
                            .square(attackWallsRange.getValue().doubleValue())))
                continue;

            float selfDamage = DamageUtils.getCrystalDamage(mc.player, null, crystal, ignoreTerrain.getValue());
            if (selfDamage > maximumSelfDamage.getValue().floatValue())
                continue;
            if (antiSuicide.getValue() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount())
                continue;

            boolean override = false;
            for (PlayerEntity player : players) {
                float damage = DamageUtils.getCrystalDamage(player,
                        PositionUtils.extrapolate(player, extrapolation.getValue().intValue()), crystal,
                        ignoreTerrain.getValue());
                if (damage < getMinimumDamage(player, minimumDamage.getValue().floatValue())
                        && damage < player.getHealth() + player.getAbsorptionAmount()
                        && !(damage * (1.0f + lethalMultiplier.getValue().floatValue()) >= player.getHealth()
                                + player.getAbsorptionAmount()))
                    continue;

                if (damage > optimalDamage || damage > player.getHealth() + player.getAbsorptionAmount()) {
                    optimalCrystal = crystal;
                    optimalDamage = damage;

                    if (damage > player.getHealth() + player.getAbsorptionAmount()) {
                        override = true;
                        break;
                    }
                }
            }

            if (override)
                break;
        }

        return optimalCrystal;
    }

    private PlaceTarget calculatePlacements(BlockPos exception) {
        if (!place.getValue())
            return null;

        if (shouldPause("Place") || ((autoSwitch.getValue().equalsIgnoreCase("None")
                || InventoryUtils.findHotbar(Items.END_CRYSTAL) == -1)
                && (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL
                        && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL)))
            return null;

        List<PlayerEntity> players = getPlayers();
        if (players.isEmpty())
            return null;

        BlockPos optimalPosition = null;
        PlayerEntity optimalPlayer = null;
        List<Entity> obstructions = new ArrayList<>();
        float optimalDamage = 0.0f;

        int calculations = 0;

        for (int i = 0; i < Pingbypass.WORLD_MANAGER.getRadius(
                Math.max(placeRange.getValue().doubleValue(), placeWallsRange.getValue().doubleValue())); i++) {
            BlockPos position = mc.player.getBlockPos().add(Pingbypass.WORLD_MANAGER.getOffset(i));

            if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                    .square(placeRange.getValue().doubleValue()))
                continue;
            if (!mc.world.getWorldBorder().contains(position))
                continue;
            if (mc.world.getBlockState(position).getBlock() != Blocks.OBSIDIAN
                    && mc.world.getBlockState(position).getBlock() != Blocks.BEDROCK)
                continue;
            if (!mc.world.getBlockState(position.add(0, 1, 0)).isAir()
                    || (placements.getValue().equalsIgnoreCase("Protocol")
                            && !mc.world.getBlockState(position.add(0, 2, 0)).isAir()))
                continue;

            if (!WorldUtils.canSee(position) && (raytrace.getValue()
                    || mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                            .square(placeWallsRange.getValue().doubleValue())))
                continue;
            if (mc.world.getOtherEntities(null, new Box(position.add(0, 1, 0))).stream()
                    .anyMatch(entity -> entity.isAlive() && !(entity instanceof ExperienceOrbEntity)
                            && !(entity instanceof EndCrystalEntity)))
                continue;

            List<Entity> obstructingCrystals = mc.world.getOtherEntities(null, new Box(position.add(0, 1, 0))).stream()
                    .filter(entity -> entity instanceof EndCrystalEntity crystal
                            && crystal.age >= (20 - attackSpeed.getValue().intValue()) + 15)
                    .toList();

            float selfDamage = DamageUtils.getCrystalDamage(mc.player, null, position, exception,
                    ignoreTerrain.getValue());
            if (selfDamage > maximumSelfDamage.getValue().floatValue())
                continue;
            if (antiSuicide.getValue() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount())
                continue;

            boolean override = false;
            for (PlayerEntity player : players) {
                calculations++;

                float damage = DamageUtils.getCrystalDamage(player,
                        PositionUtils.extrapolate(player, extrapolation.getValue().intValue()), position, exception,
                        ignoreTerrain.getValue());
                if (damage < getMinimumDamage(player, minimumDamage.getValue().floatValue())
                        && damage < player.getHealth() + player.getAbsorptionAmount()
                        && !(damage * (1.0f + lethalMultiplier.getValue().floatValue()) >= player.getHealth()
                                + player.getAbsorptionAmount()))
                    continue;

                if (exception == null && !obstructingCrystals.isEmpty()) {
                    obstructions.add(obstructingCrystals.getFirst());
                    break;
                }

                if (damage > optimalDamage || damage > player.getHealth() + player.getAbsorptionAmount()) {
                    optimalPosition = position;
                    optimalPlayer = player;
                    optimalDamage = damage;

                    if (damage > player.getHealth() + player.getAbsorptionAmount()) {
                        override = true;
                        break;
                    }
                }
            }

            if (override)
                break;
        }

        if (optimalPosition == null)
            return new PlaceTarget(null, null, obstructions, null, 0.0f, calculations);
        return new PlaceTarget(optimalPosition, optimalPlayer, obstructions, exception, optimalDamage, calculations);
    }

    private float[] calculateRotations(Vec3d vec3d) {
        float[] rotations = RotationUtils.getRotations(vec3d);

        if (yawStep.getValue()) {
            float yaw;

            float difference = Pingbypass.ROTATION_MANAGER.getServerYaw() - rotations[0];
            if (Math.abs(difference) > 180.0f)
                difference += difference > 0.0f ? -360.0f : 360.0f;

            float deltaYaw = (difference > 0.0f ? -1 : 1) * yawStepThreshold.getValue().floatValue();

            if (Math.abs(difference) > yawStepThreshold.getValue().floatValue())
                yaw = Pingbypass.ROTATION_MANAGER.getServerYaw() + deltaYaw;
            else
                yaw = rotations[0];

            rotations[0] = yaw;
        }

        return rotations;
    }

    private void attack(EndCrystalEntity crystal) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        boolean switched = false;

        if (!antiWeakness.getValue().equalsIgnoreCase("None") && mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int slot = InventoryUtils.findBestSword(InventoryUtils.HOTBAR_START, InventoryUtils.HOTBAR_END);
            if (slot != -1) {
                InventoryUtils.switchSlot(antiWeakness.getValue(), slot, previousSlot);
                switched = true;
            }
        }

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (switched) {
            InventoryUtils.switchBack(antiWeakness.getValue(), 0, previousSlot);
        }

        attackedCrystals.put(crystal.getId(), System.currentTimeMillis());
        attackTimer.reset();
    }

    private void place(BlockPos position) {
        Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Direction direction = WorldUtils.getClosestDirection(position, true);

        eu.client.utils.minecraft.NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(hand,
                new BlockHitResult(WorldUtils.getHitVector(position, direction), direction, position, false),
                sequence));

        switch (swing.getValue()) {
            case "Default" -> mc.player.swingHand(hand);
            case "Packet" -> mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            case "Mainhand" -> mc.player.swingHand(Hand.MAIN_HAND);
            case "Offhand" -> mc.player.swingHand(Hand.OFF_HAND);
            case "Both" -> {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.swingHand(Hand.OFF_HAND);
            }
        }

        placedCrystals.put(position, System.currentTimeMillis());
        countedCrystals.put(position, System.currentTimeMillis());
        placeTimer.reset();
    }

    private List<PlayerEntity> getPlayers() {
        List<PlayerEntity> players = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player)
                continue;
            if (!player.isAlive())
                continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue()))
                continue;
            if (Pingbypass.FRIEND_MANAGER.contains(player.getName().getString()))
                continue;

            players.add(player);
        }

        return players;
    }

    private boolean shouldPause(String process) {
        boolean eatingFlag = (whileEating.getValue().equalsIgnoreCase("None")
                || (process.equalsIgnoreCase("Attack") && whileEating.getValue().equalsIgnoreCase("Place"))
                || (process.equalsIgnoreCase("Place") && whileEating.getValue().equalsIgnoreCase("Attack")));
        return eatingFlag && mc.player.isUsingItem();
    }

    private float getMinimumDamage(PlayerEntity player, float minimumDamage) {
        if (player == null)
            return minimumDamage;

        if (chestBreak.getValue()
                && mc.world.getOtherEntities(null, new Box(player.getBlockPos()).expand(1)).stream()
                        .anyMatch(entity -> entity instanceof ItemEntity item
                                && item.getStack().getItem() == Items.OBSIDIAN && item.getStack().getCount() >= 8
                                && item.age <= 2 + Pingbypass.SERVER_MANAGER.getPingDelay()
                                        + (20 - placeSpeed.getValue().intValue()))
                && !mc.world.getOtherEntities(null, new Box(mc.player.getBlockPos()).expand(1)).stream()
                        .anyMatch(entity -> entity instanceof ItemEntity item
                                && item.getStack().getItem() == Items.OBSIDIAN && item.getStack().getCount() >= 8
                                && item.age <= 2 + Pingbypass.SERVER_MANAGER.getPingDelay()
                                        + (20 - placeSpeed.getValue().intValue())))
            return 2.0f;
        if (facePlaceMode.getValue().equalsIgnoreCase("None"))
            return minimumDamage;

        if (facePlaceSpeed.getValue().equalsIgnoreCase("Normal")
                || facePlaceTimer.hasTimeElapsed(facePlaceDelay.getValue().longValue() * 50L)) {
            if (facePlaceMode.getValue().equalsIgnoreCase("Always"))
                return Math.min(minimumDamage, 2.0f);
            if (facePlaceMode.getValue().equalsIgnoreCase("Dynamic") && healthPlace.getValue()
                    && (player.getHealth() + player.getAbsorptionAmount()) <= health.getValue().floatValue())
                return Math.min(minimumDamage, 2.0f);
            if (facePlaceMode.getValue().equalsIgnoreCase("Dynamic") && armorPlace.getValue()) {
                for (ItemStack stack : player.getArmorItems()) {
                    if (stack == null || !(stack.getItem() instanceof ArmorItem))
                        continue;
                    if (Math.round(((stack.getMaxDamage() - stack.getDamage()) * 100.0)
                            / stack.getMaxDamage()) <= percentage.getValue().intValue()) {
                        return Math.min(minimumDamage, 2.0f);
                    }
                }
            }
        }

        return minimumDamage;
    }

    @Getter
    @AllArgsConstructor
    public static class PlaceTarget {
        private BlockPos position;
        private PlayerEntity player;
        private List<Entity> obstructions;
        private BlockPos exception;
        private float damage;
        private int calculations;

        public PlaceTarget clone() {
            return new PlaceTarget(position, player, obstructions, exception, damage, calculations);
        }
    }
}
