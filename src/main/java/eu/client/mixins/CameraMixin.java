package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.visuals.FreecamModule;
import eu.client.modules.impl.visuals.NoRenderModule;
import eu.client.modules.impl.visuals.ViewClipModule;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow private boolean thirdPerson;

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    private void update(Args args) {
        if (EUClient.MODULE_MANAGER.getModule(ViewClipModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(ViewClipModule.class).extend.getValue()) {
            args.set(0, EUClient.MODULE_MANAGER.getModule(ViewClipModule.class).distance.getValue().floatValue());
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float f, CallbackInfoReturnable<Float> info) {
        if (EUClient.MODULE_MANAGER.getModule(ViewClipModule.class).isToggled()) {
            info.setReturnValue(f);
        }
    }

    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    private void getSubmersionType(CallbackInfoReturnable<CameraSubmersionType> info) {
        if (EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).liquidOverlay.getValue()) {
            info.setReturnValue(CameraSubmersionType.NONE);
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void update$TAIL(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            this.thirdPerson = true;
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void update$setRotation(Args args) {
        if (EUClient.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            args.setAll(EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeYaw(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreePitch());
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void update$setPos(Args args) {
        if (EUClient.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            args.setAll(EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeX(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeY(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeZ());
        }
    }
}
