package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.visuals.AtmosphereModule;
import eu.client.modules.impl.visuals.NoRenderModule;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @ModifyArgs(method = "applyFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Fog;<init>(FFLnet/minecraft/client/render/FogShape;FFFF)V"))
    private static void applyFog(Args args, Camera camera, BackgroundRenderer.FogType fogType, Vector4f originalColor, float viewDistance, boolean thickenFog, float tickDelta) {
        if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).fog.getValue()) {
            args.set(0, viewDistance * 4);
            args.set(1, viewDistance * 4.25f);
        } else {
            if (EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).modifyFog.getValue()) {
                Color color = EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).fogColor.getColor();

                args.set(0, EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).fogStart.getValue().floatValue());
                args.set(1, EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).fogEnd.getValue().floatValue());
                args.set(3, color.getRed() / 255.0f);
                args.set(4, color.getGreen() / 255.0f);
                args.set(5, color.getBlue() / 255.0f);
                args.set(6, color.getAlpha() / 255.0f);
            }
        }
    }

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<BackgroundRenderer.StatusEffectFogModifier> info) {
        if (EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoRenderModule.class).blindness.getValue()) {
            info.setReturnValue(null);
        }
    }
}
