package eu.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.client.EUClient;
import eu.client.events.impl.PlayerTravelEvent;
import eu.client.modules.impl.movement.KeepSprintModule;
import eu.client.modules.impl.movement.SafeWalkModule;
import eu.client.modules.impl.movement.SpeedModule;
import eu.client.modules.impl.movement.VelocityModule;
import eu.client.modules.impl.player.ReachModule;
import eu.client.utils.IMinecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IMinecraft {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "isPushedByFluids", at = @At("RETURN"))
    private boolean isPushedByFluids(boolean original) {
        if ((Object) this == mc.player && EUClient.MODULE_MANAGER.getModule(VelocityModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(VelocityModule.class).antiLiquidPush.getValue()) return false;
        return original;
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    private void attack(CallbackInfo callbackInfo) {
        if (EUClient.MODULE_MANAGER.getModule(KeepSprintModule.class).isToggled()) {
            float multiplier = 0.6f + 0.4f * EUClient.MODULE_MANAGER.getModule(KeepSprintModule.class).motion.getValue().floatValue();
            mc.player.setVelocity(mc.player.getVelocity().x / 0.6 * multiplier, mc.player.getVelocity().y, mc.player.getVelocity().z / 0.6 * multiplier);
            mc.player.setSprinting(true);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3d movementInput, CallbackInfo info) {
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput);
        EUClient.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            move(MovementType.SELF, getVelocity());
            info.cancel();
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getBlockInteractionRange(CallbackInfoReturnable<Double> info) {
        if (EUClient.MODULE_MANAGER.getModule(ReachModule.class).isToggled()) {
            info.setReturnValue(EUClient.MODULE_MANAGER.getModule(ReachModule.class).amount.getValue().doubleValue());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getEntityInteractionRange(CallbackInfoReturnable<Double> info) {
        if (EUClient.MODULE_MANAGER.getModule(ReachModule.class).isToggled()) {
            info.setReturnValue(EUClient.MODULE_MANAGER.getModule(ReachModule.class).amount.getValue().doubleValue());
        }
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        if (EUClient.MODULE_MANAGER.getModule(SafeWalkModule.class).isToggled()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "getMovementSpeed", at = @At("HEAD"), cancellable = true)
    private void getMovementSpeed(CallbackInfoReturnable<Float> info) {
        if (EUClient.MODULE_MANAGER.getModule(SpeedModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(SpeedModule.class).mode.getValue().equalsIgnoreCase("Vanilla")) {
            info.setReturnValue(EUClient.MODULE_MANAGER.getModule(SpeedModule.class).vanillaSpeed.getValue().floatValue());
        }
    }
}
