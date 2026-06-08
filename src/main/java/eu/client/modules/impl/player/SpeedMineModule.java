package eu.client.modules.impl.player;

import lombok.Getter;
import lombok.Setter;
import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.NetworkUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.Timer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "SpeedMine", description = "Automatically mines blocks at a faster speed using packets.", category = Module.Category.PLAYER, proxyEnhanced = true)
public class SpeedMineModule extends Module {
    public ModeSetting switchMode = new ModeSetting("Switch",
            "The mode that will be used for automatically switching to the fastest item.", "Silent",
            InventoryUtils.SWITCH_MODES);
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which blocks will be mined.", 6.0,
            0.0, 8.0);
    public NumberSetting speed = new NumberSetting("Speed", "The speed at which the module will mine blocks.", 1.0, 0.7,
            1.0);
    public ModeSetting rotate = new ModeSetting("Rotate", "Automatically rotates to the block when mining it.",
            "Packet", new String[] { "None", "Normal", "Packet" });

    public BooleanSetting auto = new BooleanSetting("Auto",
            "Automatically mines blocks deemed optimal for defeating your opponents.", false);
    public BooleanSetting cityOnly = new BooleanSetting("CityOnly", "Only mines the target's city positions.",
            new BooleanSetting.Visibility(auto, true), false);
    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Only mine the player in hole.",
            new BooleanSetting.Visibility(auto, true), false);
    public BooleanSetting switchReset = new BooleanSetting("SwitchReset", "Resets the mining when switching slots.",
            new ModeSetting.Visibility(switchMode, "None", "AltSwap", "AltPickup"), true);
    public BooleanSetting doubleMine = new BooleanSetting("Double", "Allows the mining of 2 blocks at the same time.",
            false);
    public ModeSetting sequence = new ModeSetting("Sequence", "Sequence of mining for double mine",
            new BooleanSetting.Visibility(doubleMine, true), "Surround", new String[] { "Surround", "Phase" });
    public BooleanSetting instant = new BooleanSetting("Instant",
            "Instantly mines blocks once they have been replaced.", false);
    public NumberSetting instantDelay = new NumberSetting("InstantDelay",
            "The amount of time that has to pass before instantly mining blocks.",
            new BooleanSetting.Visibility(instant, true), 0, 0, 20);
    public NumberSetting instantTimeout = new NumberSetting("InstantTimeout",
            "The amount of time that cancel instantly mine while no block to mine.",
            new BooleanSetting.Visibility(instant, true), 60, 0, 100);
    public BooleanSetting grim = new BooleanSetting("Grim", "Adds a bypass catered to the Grim anticheat.", false);
    public BooleanSetting clientRemove = new BooleanSetting("ClientRemove",
            "Removes the block client-side immediately for instant visual feedback.", true);
    public BooleanSetting strict = new BooleanSetting("Strict",
            "Waits for the server to tick you before switching back.", false);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Mines blocks while eating.", true);
    public WhitelistSetting whitelist = new WhitelistSetting("Whitelist",
            "Mines only the blocks that are on this list. If empty, every block will be mined.",
            WhitelistSetting.Type.BLOCKS);

    // Render settings removed for proxy headless mode

    @Getter
    private Action primary = null;
    @Getter
    private Action secondary = null;

    private SwitchAction switchAction = null;

    private final Timer instantTimer = new Timer();
    private final Timer mineTimer = new Timer();

    /**
     * When true, SpeedMine pauses mining to let the client eat/interact.
     * Set by PbPlayHandler when the client sends an interact packet,
     * cleared when the client sends RELEASE_USE_ITEM.
     */
    @Getter
    private volatile boolean interactPaused = false;
    private boolean needsRestart = false;

    public void setInteractPaused(boolean paused) {
        this.interactPaused = paused;
        if (!paused) {
            // When unpausing, flag that mining needs to restart
            // (re-send pickaxe + START_DESTROY to the server)
            this.needsRestart = true;
        }
    }

    // Proxy-synced render state for client-side rendering
    public volatile BlockPos proxyPrimaryPos = null;
    public volatile float proxyPrimaryProgress = 0;
    public volatile BlockPos proxySecondaryPos = null;
    public volatile float proxySecondaryProgress = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (doubleMine.getValue() && secondary != null && secondary.process())
            secondary = null;
        if (primary != null && primary.process())
            primary = null;

        // Sync mining state to the client for rendering
        if (isRunningOnProxy() && Pingbypass.PROXY_SERVER != null) {
            syncMiningStateToClient();
        }

        if (!auto.getValue())
            return;
        if ((primary != null && primary.getPriority() > 0 && !WorldUtils.isReplaceable(primary.getPosition()))
                || (secondary != null && secondary.getPriority() > 0
                        && !WorldUtils.isReplaceable(secondary.getPosition())))
            return;

        Target target = getTarget();

        if (doubleMine.getValue()) {
            if (!mineTimer.hasTimeElapsed(350L))
                return;

            if (mc.player.isCrawling()) {
                BlockPos position;
                BlockPos playerPosition = mc.player.getBlockPos();

                if (WorldUtils.canBreak(playerPosition.down()) && !WorldUtils.isReplaceable(playerPosition.down())
                        && (!WorldUtils.isReplaceable(playerPosition.down(2))
                                || HoleUtils.getSingleHole(playerPosition.down(2), 1, false) != null)) {
                    position = playerPosition.down();
                } else {
                    position = playerPosition.up();
                }

                if (isValid(position) && !isOutOfRange(position)) {
                    if (!isInvalid(position))
                        handle(position, 0);
                    return;
                }
            }

            if ((primary != null && primary.isInstantMine()
                    && !instantTimer.hasTimeElapsed(instantTimeout.getValue().longValue() * 50L)
                    && primary.getAttempts() != 0) || secondary != null)
                return;

            if (target != null) {
                Runnable inside = () -> {
                    List<BlockPos> insidePositions = HoleUtils.getInsidePositions(target.player()).stream()
                            .filter(insidePosition -> !mc.world.getBlockState(insidePosition).isReplaceable()).toList();
                    ;
                    for (BlockPos position : insidePositions) {
                        if (primary != null && secondary != null)
                            break;
                        if (isInvalid(position) || isOutOfRange(position))
                            continue;
                        handle(position, 0);
                    }
                };
                Runnable outside = () -> {
                    List<BlockPos> surroundPositions = HoleUtils.getFeetPositions(target.player(), true, false, true)
                            .stream().filter(pos -> !mc.world.getBlockState(pos).isReplaceable()).toList();
                    if (HoleUtils.isPlayerInHole(target.player()) || !holeCheck.getValue()) {
                        for (BlockPos position : surroundPositions) {
                            if (primary != null && secondary != null)
                                break;
                            if (isMining(position))
                                continue;
                            if (isInvalid(position) || isOutOfRange(position))
                                continue;
                            handle(position, 0);
                        }
                    }
                };
                if (sequence.getValue().equals("Surround")) {
                    outside.run();
                    inside.run();
                } else if (sequence.getValue().equals("Phase")) {
                    inside.run();
                    outside.run();
                }
            }
        } else {
            BlockPos position = null;

            if (target == null) {
                return;
            } else {
                if (!WorldUtils.isReplaceable(target.player.getBlockPos())
                        && !WorldUtils.getBlock(target.player().getBlockPos()).equals(Blocks.COBWEB)) {
                    position = target.player().getBlockPos();
                } else if (HoleUtils.isPlayerInHole(target.player()) || !holeCheck.getValue()) {
                    position = target.position();
                }
            }
            if (position == null)
                return;
            if (primary != null && position.equals(primary.getPosition()))
                return;

            handle(position, 0);
        }
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE)
    public void onTick(TickEvent event) {
        if (shouldRunOnProxy())
            return;
        if (switchAction == null)
            return;
        if (System.currentTimeMillis() - switchAction.time() < 100L)
            return;

        if (mc.player != null && mc.world != null && (switchAction.slot() != -1 && switchAction.previousSlot() != -1)) {
            InventoryUtils.switchBack(switchMode.getValue(), switchAction.slot(), switchAction.previousSlot());
        }

        switchAction = null;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        if (shouldRunOnProxy()) {
            // Client side: render using proxy-synced state
            renderProxyState(event.getMatrices());
            return;
        }

        if (doubleMine.getValue() && secondary != null)
            secondary.render(event.getMatrices());
        if (primary != null)
            primary.render(event.getMatrices());
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent.Post event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket && switchReset.getValue()
                && (switchMode.getValue().equalsIgnoreCase("AltSwap")
                        || switchMode.getValue().equalsIgnoreCase("AltPickup"))) {
            if (secondary != null) {
                secondary.cancel();
                secondary.start();
            }

            if (primary != null) {
                primary.cancel();
                primary.start();
            }
        }
    }

    @SubscribeEvent
    public void onAttackBlock(AttackBlockEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;

        if (handle(event.getPosition(), 1)) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getMetaData() {
        String primaryProgress = primary == null ? "0.0"
                : new DecimalFormat("0.0").format(primary.getProgress() / primary.getSpeed());
        String secondaryProgress = secondary == null || !doubleMine.getValue() ? ""
                : ", " + new DecimalFormat("0.0").format(secondary.getProgress() / secondary.getSpeed());
        return primaryProgress + secondaryProgress;
    }

    private boolean handle(BlockPos position, int priority) {
        if (mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE
                || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
            return false;
        if (mc.world.getBlockState(position).getBlock().getHardness() == -1)
            return false;
        if (!whitelist.getWhitelist().isEmpty()
                && !whitelist.isWhitelistContains(mc.world.getBlockState(position).getBlock()))
            return false;
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                .square(range.getValue().doubleValue()))
            return false;

        if ((primary != null && primary.getPosition().equals(position))
                || (secondary != null && secondary.getPosition().equals(position)))
            return true;

        if (doubleMine.getValue()) {
            if (secondary != null) {
                primary = new Action(position, priority);
            } else {
                if (primary != null) {
                    if (!primary.isInstantMine())
                        secondary = primary;
                    primary = new Action(position, priority);
                } else {
                    primary = new Action(position, priority);
                }
            }
        } else {
            if (primary != null)
                primary.cancel();
            primary = new Action(position, priority);
        }

        return true;
    }

    private boolean isInvalid(BlockPos position) {
        if (!isValid(position))
            return true;
        return isMining(position);
    }

    private boolean isValid(BlockPos position) {
        if (position == null)
            return false;
        if (mc.world.getBlockState(position).getBlock().getHardness() == -1)
            return false;
        return !mc.world.getBlockState(position).getBlock().equals(Blocks.COBWEB);
    }

    private boolean isMining(BlockPos position) {
        if (position == null)
            return true;
        if (primary != null && primary.getPosition().equals(position))
            return true;
        return secondary != null && secondary.getPosition().equals(position);
    }

    private boolean isOutOfRange(BlockPos position) {
        if (position == null)
            return true;
        return mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper
                .square(range.getValue().doubleValue());
    }

    private Target getTarget() {
        Target optimalTarget = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player)
                continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f)
                continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(range.getValue().doubleValue() + 2.0))
                continue;
            if (Pingbypass.FRIEND_MANAGER.contains(player.getName().getString()))
                continue;

            List<Position> feetPositions = getPositions(player);
            BlockPos position = getTargetPosition(feetPositions);

            if (!doubleMine.getValue()) {
                if (feetPositions.isEmpty())
                    continue;
                if (position == null)
                    continue;
            }

            if (optimalTarget == null) {
                optimalTarget = new Target(player, feetPositions, position);
                continue;
            }

            if (mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget.player())) {
                optimalTarget = new Target(player, feetPositions, position);
            }
        }

        return optimalTarget;
    }

    private BlockPos getTargetPosition(List<Position> positions) {
        BlockPos optimalPosition = null;
        double optimalScore = 0.0;

        for (Position position : positions) {
            if ((doubleMine.getValue() || cityOnly.getValue()) && !position.feetPosition())
                continue;
            if (!isValidPosition(position.position()))
                continue;
            if (HoleUtils.isPlayerInHole(mc.player)
                    && HoleUtils.getFeetPositions(mc.player, true, false, true).contains(position.position()))
                continue;

            double score = 0.0;

            if (position.feetPosition()) {
                score += 0.05;

                if (mc.world.getBlockState(position.position()).getBlock() == Blocks.ENDER_CHEST)
                    score += 0.95;
                else if (WorldUtils.isCrystalPlaceable(position.position().add(0, 1, 0)))
                    score += 0.35;
                if (hasCityPosition(position.position()))
                    score += 0.6;
            } else {
                if (mc.world.getBlockState(position.position()).getBlock() == Blocks.ENDER_CHEST) {
                    score -= 2.0;
                } else {
                    if (WorldUtils.isCrystalPlaceable(position.position().add(0, 1, 0)))
                        score += 0.75;
                    else
                        score -= 2.0;
                }
            }

            if (score >= optimalScore) {
                optimalPosition = position.position();
                optimalScore = score;
            }
        }

        return optimalPosition;
    }

    private List<Position> getPositions(PlayerEntity player) {
        List<Position> positions = new ArrayList<>();

        for (BlockPos position : HoleUtils.getFeetPositions(player, true, false, true)) {
            positions.add(new Position(position, true));
            if (!doubleMine.getValue())
                positions.add(new Position(position.add(0, 1, 0), false));
        }

        if (!doubleMine.getValue())
            positions.add(new Position(player.getBlockPos().add(0, 2, 0), false));
        return positions;
    }

    private boolean isValidPosition(BlockPos position) {
        if (mc.world.getBlockState(position).isReplaceable())
            return false;
        if (mc.world.getBlockState(position).getBlock().getHardness() == -1)
            return false;
        return !isOutOfRange(position);
    }

    private boolean hasCityPosition(BlockPos position) {
        Vec3i[] offsets = new Vec3i[] { new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1),
                new Vec3i(0, 0, -1) };

        for (Vec3i vec3i : offsets) {
            BlockPos offsetPosition = position.add(vec3i);
            if (WorldUtils.isPlaceable(offsetPosition))
                return true;
        }

        return false;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Proxy-sided packet helpers.
    // When the proxy is forwarding for a client, SpeedMine's packets
    // are sent DIRECTLY to the server connection, completely bypassing
    // mc.getNetworkHandler().sendPacket(). This means:
    // - The proxy's local mc.player state is never touched
    // - mc.player.getInventory().selectedSlot stays in sync with the client
    // - The client never sees slot switches, arm swings, or rotations
    // - The server sees the atomic switch→mine→switchback burst
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Returns true when this module is executing on the proxy server.
     * On the proxy, we send packets directly to the server connection
     * to avoid touching the proxy's local player state.
     */
    private boolean isProxyActive() {
        return isRunningOnProxy() && Pingbypass.PROXY_SERVER != null;
    }

    /**
     * Sends the current mining state (positions + progress) to the connected
     * client so it can render mining progress boxes.
     */
    private void syncMiningStateToClient() {
        var packet = new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                eu.client.pingbypass.protocol.PbCustomPayload.fromPacket(
                        new eu.client.pingbypass.protocol.packets.S2CMiningStatePacket(
                                primary != null ? primary.getPosition() : null,
                                primary != null ? primary.getProgress() / primary.getSpeed() : 0,
                                secondary != null ? secondary.getPosition() : null,
                                secondary != null ? secondary.getProgress() / secondary.getSpeed() : 0)));
        for (net.minecraft.network.ClientConnection conn : Pingbypass.PROXY_SERVER.getConnections()) {
            if (conn.isOpen())
                conn.send(packet);
        }
    }

    /**
     * Renders mining progress on the client using proxy-synced state.
     */
    private void renderProxyState(MatrixStack matrices) {
        if (proxyPrimaryPos != null)
            renderProxyBlock(matrices, proxyPrimaryPos, proxyPrimaryProgress);
        if (proxySecondaryPos != null && doubleMine.getValue())
            renderProxyBlock(matrices, proxySecondaryPos, proxySecondaryProgress);
    }

    private void renderProxyBlock(MatrixStack matrices, BlockPos pos, float progress) {
    }

    /**
     * Sends a packet directly to the real server connection when on the proxy,
     * bypassing the proxy's local ClientPlayNetworkHandler. Falls back to
     * normal send when running locally.
     */
    private void serverSend(net.minecraft.network.packet.Packet<?> packet) {
        if (isProxyActive()) {
            var serverConn = Pingbypass.PROXY_SERVER.getServerConnection();
            if (serverConn != null && serverConn.isOpen()) {
                eu.client.pingbypass.server.ProxyServerTickListener.allowSend(() -> serverConn.send(packet));
                return;
            }
        }
        mc.getNetworkHandler().sendPacket(packet);
    }

    /**
     * Sends a sequenced packet directly to the server connection when on the proxy.
     */
    private void serverSendSequenced(
            java.util.function.IntFunction<net.minecraft.network.packet.Packet<?>> packetFactory) {
        if (isProxyActive()) {
            try (var pending = ((eu.client.mixins.accessors.ClientWorldAccessor) mc.world)
                    .invokeGetPendingUpdateManager().incrementSequence()) {
                serverSend(packetFactory.apply(pending.getSequence()));
            }
        } else {
            NetworkUtils.sendSequencedPacket(seq -> {
                @SuppressWarnings("unchecked")
                var p = (net.minecraft.network.packet.Packet<net.minecraft.network.listener.ServerPlayPacketListener>) packetFactory
                        .apply(seq);
                return p;
            });
        }
    }

    @Getter
    public class Action {
        private final BlockPos position;
        private BlockState state;
        private final int priority;

        @Setter
        private float progress;
        private float prevProgress;
        private int attempts;
        private boolean mining;

        private boolean instantMine;
        private int startSlot = -1; // slot we switched FROM at start

        public Action(BlockPos position, int priority) {
            this.position = position;
            this.state = mc.world.getBlockState(position);
            this.priority = priority;

            start();
        }

        public boolean process() {
            if (isOutOfRange(position)) {
                cancel();
                return true;
            }

            // If the client is eating/interacting, pause mining — don't send
            // any packets that would change the server's slot state.
            if (interactPaused) {
                return false;
            }

            // After unpausing (client finished eating), restart mining from
            // scratch so the server recalculates with pickaxe speed.
            if (needsRestart && isProxyActive()) {
                needsRestart = false;
                start();
                return false;
            }

            boolean secondary = getSecondary() != null && position.equals(getSecondary().getPosition());
            if (secondary)
                instantMine = false;

            // Block is broken (air) — clean up and switch back to client's slot
            if (mc.world.getBlockState(position).isReplaceable()) {
                if (isProxyActive()) {
                    // Switch server back to the client's actual slot
                    serverSend(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                }
                if (switchAction != null) {
                    switchAction = null;
                }
                cancel();
                return true;
            }

            Direction direction = WorldUtils.getClosestDirection(position, true);
            BlockState state = mc.world.getBlockState(position);

            if (!state.isReplaceable() && state.getBlock() != this.state.getBlock()) {
                this.state = state;
            }

            if (mining) {
                int slot = switchMode.getValue().equalsIgnoreCase("None") ? -1
                        : InventoryUtils.findFastestItem(this.state, InventoryUtils.HOTBAR_START,
                                switchMode.getValue().equalsIgnoreCase("AltSwap")
                                        || switchMode.getValue().equalsIgnoreCase("AltPickup")
                                                ? InventoryUtils.INVENTORY_END
                                                : InventoryUtils.HOTBAR_END);
                if (slot == -1)
                    slot = mc.player.getInventory().selectedSlot;

                float delta = WorldUtils.getMineSpeed(this.state, slot) / Pingbypass.WORLD_MANAGER.getTimerMultiplier();

                prevProgress = progress;
                progress = MathHelper.clamp(progress + delta, 0.0f, getSpeed());

                if (rotate.getValue().equalsIgnoreCase("Normal") && progress + (delta * 2) >= getSpeed()) {
                    if (isProxyActive()) {
                        float[] rots = RotationUtils.getRotations(WorldUtils.getHitVector(position, direction));
                        serverSend(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                rots[0], rots[1], mc.player.isOnGround(), mc.player.horizontalCollision));
                    } else {
                        Pingbypass.ROTATION_MANAGER.rotate(
                                RotationUtils.getRotations(WorldUtils.getHitVector(position, direction)),
                                Pingbypass.ROTATION_MANAGER
                                        .getModulePriority(Pingbypass.MODULE_MANAGER.getModule(SpeedMineModule.class)));
                    }
                }

                if (progress >= getSpeed() && !state.isReplaceable()
                        && (whileEating.getValue() || !mc.player.isUsingItem())) {
                    if (!instantMine || instantTimer.hasTimeElapsed(instantDelay.getValue().longValue() * 50L)) {
                        Pingbypass.EVENT_HANDLER.post(new DestroyBlockEvent(position));

                        if (rotate.getValue().equalsIgnoreCase("Packet")) {
                            if (isProxyActive()) {
                                float[] rots = RotationUtils.getRotations(WorldUtils.getHitVector(position, direction));
                                serverSend(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                        rots[0], rots[1], mc.player.isOnGround(), mc.player.horizontalCollision));
                            } else {
                                Pingbypass.ROTATION_MANAGER.packetRotate(
                                        RotationUtils.getRotations(WorldUtils.getHitVector(position, direction)));
                            }
                        }

                        int previousSlot = mc.player.getInventory().selectedSlot;

                        if (isProxyActive()) {
                            int mineSlot = switchMode.getValue().equalsIgnoreCase("None") ? -1 : slot;
                            boolean needSwitch = mineSlot != -1 && mineSlot != previousSlot;

                            if (needSwitch)
                                serverSend(new UpdateSelectedSlotC2SPacket(mineSlot));

                            serverSendSequenced(seq -> new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                            if (grim.getValue())
                                serverSend(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                                        position.up(500), direction));
                            serverSend(new HandSwingC2SPacket(Hand.MAIN_HAND));

                            if (needSwitch)
                                serverSend(new UpdateSelectedSlotC2SPacket(previousSlot));
                        } else {
                            InventoryUtils.switchSlot(switchMode.getValue(), slot, previousSlot);

                            NetworkUtils.sendSequencedPacket(seq -> new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                            if (grim.getValue())
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, position.up(500), direction));
                            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                            if (strict.getValue() || (doubleMine.getValue() && secondary))
                                switchAction = new SwitchAction(slot, previousSlot, System.currentTimeMillis());
                            else if (switchAction == null)
                                InventoryUtils.switchBack(switchMode.getValue(), slot, previousSlot);
                        }

                        // Remove block client-side so modules see it as air immediately
                        if (clientRemove.getValue()) {
                            mc.world.removeBlock(position, false);
                        }

                        if (!instantMine || secondary)
                            mineTimer.reset();
                    }

                    attempts++;
                    if (!secondary) {
                        if (!instant.getValue()) {
                            // Don't restart immediately — wait for the server to confirm
                            // the break (block becomes air). The process() loop will detect
                            // the block is replaceable and return true on the next tick.
                            this.mining = false;
                        } else {
                            this.instantMine = true;
                            instantTimer.reset();
                        }
                    }

                    return doubleMine.getValue() && secondary;
                }
            } else {
                // Only restart if the block is still there and we haven't just sent STOP
                if (!mc.world.getBlockState(position).isReplaceable() && attempts == 0) {
                    start();
                }
            }

            return false;
        }

        public void render(MatrixStack matrices) {
        }

        public void start() {
            Direction direction = WorldUtils.getClosestDirection(position, true);

            if (isProxyActive()) {
                // Proxy: atomic switch→mine→switchback. Packets go directly
                // to the server connection so the proxy's local state is untouched.
                int slot = switchMode.getValue().equalsIgnoreCase("None") ? -1
                        : InventoryUtils.findFastestItem(state, InventoryUtils.HOTBAR_START,
                                switchMode.getValue().equalsIgnoreCase("AltSwap")
                                        || switchMode.getValue().equalsIgnoreCase("AltPickup")
                                                ? InventoryUtils.INVENTORY_END
                                                : InventoryUtils.HOTBAR_END);
                boolean needSwitch = slot != -1 && slot != mc.player.getInventory().selectedSlot;

                if (needSwitch)
                    serverSend(new UpdateSelectedSlotC2SPacket(slot));

                if (doubleMine.getValue()) {
                    serverSendSequenced(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                    serverSendSequenced(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, direction, seq));
                    serverSendSequenced(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                } else {
                    serverSendSequenced(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, direction, seq));
                }

                serverSend(new HandSwingC2SPacket(Hand.MAIN_HAND));
                // Do NOT switch back here — keep pickaxe held on the server
                // so continueMining() calculates with pickaxe speed every tick.
                // The switch-back happens in process() after STOP_DESTROY.
            } else {
                // Local: no slot switch in start(), only at STOP_DESTROY moment
                if (doubleMine.getValue()) {
                    NetworkUtils.sendSequencedPacket(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                    NetworkUtils.sendSequencedPacket(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, direction, seq));
                    NetworkUtils.sendSequencedPacket(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, position, direction, seq));
                } else {
                    NetworkUtils.sendSequencedPacket(seq -> new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, direction, seq));
                }
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            this.progress = 0.0f;
            this.prevProgress = 0.0f;
            this.attempts = 0;
            this.mining = true;
            this.instantMine = false;
        }

        public void cancel() {
            if (!doubleMine.getValue()) {
                if (isProxyActive()) {
                    serverSendSequenced(
                            seq -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, position,
                                    WorldUtils.getClosestDirection(position, true), seq));
                    serverSend(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    // Switch server back to client's actual slot
                    serverSend(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                } else {
                    NetworkUtils.sendSequencedPacket(
                            seq -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, position,
                                    WorldUtils.getClosestDirection(position, true), seq));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }

            this.progress = 0.0f;
            this.prevProgress = 0.0f;
            this.attempts = 0;
            this.mining = false;

            this.instantMine = false;
        }

        private float getSpeed() {
            return getSecondary() != null && position.equals(getSecondary().getPosition()) ? 1.0f
                    : speed.getValue().floatValue();
        }
    }

    private record SwitchAction(int slot, int previousSlot, long time) {
    }

    private record Target(PlayerEntity player, java.util.List<Position> feetPositions, BlockPos position) {
    }

    private record Position(BlockPos position, boolean feetPosition) {
    }
}
