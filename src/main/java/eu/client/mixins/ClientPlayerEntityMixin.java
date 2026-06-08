package eu.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import eu.client.EUClient;
import eu.client.events.impl.*;
import eu.client.modules.impl.movement.InventoryControlModule;
import eu.client.modules.impl.movement.NoSlowModule;
import eu.client.modules.impl.movement.VelocityModule;
import eu.client.modules.impl.player.SwingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow @Final protected MinecraftClient client;

    @Shadow protected abstract void autoJump(float dx, float dz);

    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE))
    private void tick$BEFORE(CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new PlayerUpdateEvent());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.AFTER))
    private void tick$AFTER(CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new UpdateMovementEvent());
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;tickables:Ljava/util/List;", shift = At.Shift.BEFORE))
    private void tick$tickables(CallbackInfo ci) {
        EUClient.EVENT_HANDLER.post(new UpdateMovementEvent.Post());
    }

    @WrapOperation(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastYaw:F", ordinal = 0))
    private float sendMovementPackets$lastYaw(ClientPlayerEntity instance, Operation<Float> original) {
        if (EUClient.ROTATION_MANAGER.getRotation() != null) return EUClient.ROTATION_MANAGER.getServerYaw();
        return original.call(instance);
    }

    @WrapOperation(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastPitch:F", ordinal = 0))
    private float sendMovementPackets$lastPitch(ClientPlayerEntity instance, Operation<Float> original) {
        if (EUClient.ROTATION_MANAGER.getRotation() != null) return EUClient.ROTATION_MANAGER.getServerPitch();
        return original.call(instance);
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean tickMovement$isUsingItem(boolean original) {
        if (EUClient.MODULE_MANAGER.getModule(NoSlowModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoSlowModule.class).items.getValue() && !EUClient.MODULE_MANAGER.getModule(NoSlowModule.class).shouldSlow()) return false;
        return original;
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(MovementType movementType, Vec3d movement, CallbackInfo info) {
        PlayerMoveEvent event = new PlayerMoveEvent(movementType, movement);
        EUClient.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            info.cancel();

            double prevX = getX();
            double prevZ = getZ();

            super.move(movementType, event.getMovement());
            autoJump((float) (getX() - prevX), (float) (getZ() - prevZ));
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPackets(CallbackInfo info) {
        // When proxy forwarding is active on the SERVER (proxy), cancel sendMovementPackets.
        // The client's movement packets are forwarded by PbPlayHandler.onPlayerMove.
        // Without this cancel, the proxy would send its OWN movement to the server
        // (conflicting with the forwarded client packets).
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && EUClient.PINGBYPASS_CONFIG != null && EUClient.PINGBYPASS_CONFIG.isServer()) {
            info.cancel();
            return;
        }
        // On the CLIENT side: do NOT cancel. The client sends movement packets
        // to the proxy normally, and the proxy forwards them to the real server.
        SendMovementEvent event = new SendMovementEvent();
        EUClient.EVENT_HANDLER.post(event);
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean canStartSprinting$isUsingItem(boolean original) {
        NoSlowModule module = EUClient.MODULE_MANAGER.getModule(NoSlowModule.class);
        if (EUClient.MODULE_MANAGER != null && module.isToggled() && module.items.getValue()) {
            return false;
        }

        return original;
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(VelocityModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(VelocityModule.class).antiBlockPush.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void swingHand(Hand hand, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).isToggled()) {
            if (!EUClient.MODULE_MANAGER.getModule(SwingModule.class).hand.getValue().equals("None")) {
                switch (EUClient.MODULE_MANAGER.getModule(SwingModule.class).hand.getValue()) {
                    case "Default" -> super.swingHand(hand);
                    case "Mainhand" -> super.swingHand(Hand.MAIN_HAND);
                    case "Offhand" -> super.swingHand(Hand.OFF_HAND);
                    case "Both" -> {
                        super.swingHand(Hand.MAIN_HAND);
                        super.swingHand(Hand.OFF_HAND);
                    }
                }

                if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).hand.getValue().equalsIgnoreCase("Packet") || !EUClient.MODULE_MANAGER.getModule(SwingModule.class).noPacket.getValue()) {
                    networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                }
            }

            info.cancel()   ;
        }
    }

    @Inject(method = "tickNausea", at = @At("HEAD"), cancellable = true)
    private void tickNausea(boolean fromPortalEffect, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(InventoryControlModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(InventoryControlModule.class).portals.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "setCurrentHand", at = @At(value = "HEAD"))
    private void setCurrentHand(Hand hand, CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new ChangeHandEvent());
    }
}
