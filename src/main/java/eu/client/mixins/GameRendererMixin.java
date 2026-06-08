package eu.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.client.EUClient;
import eu.client.events.impl.RenderEntityEvent;
import eu.client.events.impl.RenderShaderEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.impl.miscellaneous.FOVModifierModule;
import eu.client.modules.impl.player.NoEntityTraceModule;
import eu.client.modules.impl.visuals.AspectRatioModule;
import eu.client.modules.impl.visuals.FreecamModule;
import eu.client.modules.impl.visuals.NoRenderModule;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow private float zoom;

    @Shadow private float zoomX;

    @Shadow private float zoomY;

    @Shadow private float viewDistance;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void renderWorld$HEAD(RenderTickCounter tickCounter, CallbackInfo info) {
        Renderer3D.prepare();
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void renderWorld$swap(RenderTickCounter tickCounter, CallbackInfo info, @Local(ordinal = 2) Matrix4f matrix4f3, @Local(ordinal = 1) float tickDelta, @Local MatrixStack matrixStack) {
        RenderSystem.getModelViewStack().pushMatrix();

        RenderSystem.getModelViewStack().mul(matrix4f3);
        RenderSystem.getModelViewStack().mul(matrixStack.peek().getPositionMatrix().invert());

        EUClient.EVENT_HANDLER.post(new RenderWorldEvent(matrixStack, tickDelta));

        Renderer3D.draw(Renderer3D.QUADS, Renderer3D.DEBUG_LINES, false);
        Renderer3D.draw(Renderer3D.SHINE_QUADS, Renderer3D.SHINE_DEBUG_LINES, true);

        EUClient.EVENT_HANDLER.post(new RenderWorldEvent.Post(matrixStack, tickDelta));
        EUClient.EVENT_HANDLER.post(new RenderEntityEvent.Post());

        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void renderWorld$TAIL(RenderTickCounter renderTickCounter, CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new RenderShaderEvent.Post());
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurt(CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).hurtCamera.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "updateCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"), cancellable = true)
    private void updateCrosshairTarget(float tickDelta, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            Profilers.get().pop();
            client.crosshairTarget = WorldUtils.getRaytraceTarget(EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeYaw(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreePitch(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeX(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeY(), EUClient.MODULE_MANAGER.getModule(FreecamModule.class).getFreeZ());
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private @Nullable EntityHitResult findCrosshairTarget(@Nullable EntityHitResult original) {
        if (EUClient.MODULE_MANAGER.getModule(NoEntityTraceModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoEntityTraceModule.class).shouldIgnore()) {
            return null;
        }

        return original;
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void showFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).totemAnimation.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void renderWorld(RenderTickCounter tickCounter, CallbackInfo info, @Local(ordinal = 0) Matrix4f matrix4f) {
        MatrixStack matrix = new MatrixStack();
        matrix.peek().getPositionMatrix().mul(matrix4f);

        Renderer2D.LAST_PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        Renderer2D.LAST_MODEL_MATRIX.set(RenderSystem.getModelViewMatrix());
        Renderer2D.LAST_WORLD_MATRIX.set(matrix.peek().getPositionMatrix());
    }

    @Inject(method = "getFov", at = @At("TAIL"), cancellable = true)
    private void getFOV(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> info) {
        FOVModifierModule module = EUClient.MODULE_MANAGER.getModule(FOVModifierModule.class);
        if(module.isToggled()) {
            if(info.getReturnValue() == 70 && !module.items.getValue()) return;
            info.setReturnValue(module.fov.getValue().floatValue());
        }
    }

    @Inject(method = "getBasicProjectionMatrix",at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> info) {
        if (EUClient.MODULE_MANAGER.getModule(AspectRatioModule.class).isToggled()) {
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.peek().getPositionMatrix().identity();
            if (zoom != 1.0f) {
                matrixStack.translate(zoomX, -zoomY, 0.0f);
                matrixStack.scale(zoom, zoom, 1.0f);
            }

            matrixStack.peek().getPositionMatrix().mul(new Matrix4f().setPerspective((float)(fovDegrees * 0.01745329238474369), EUClient.MODULE_MANAGER.getModule(AspectRatioModule.class).ratio.getValue().floatValue(), 0.05f, viewDistance * 4.0f));
            info.setReturnValue(matrixStack.peek().getPositionMatrix());
        }
    }
}
