package eu.client.mixins;

import eu.client.EUClient;
import eu.client.events.impl.EntitySpawnEvent;
import eu.client.modules.impl.visuals.AtmosphereModule;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).modifyFog.getValue()) {
            info.setReturnValue(EUClient.MODULE_MANAGER.getModule(AtmosphereModule.class).fogColor.getColor().getRGB());
        }
    }

    @Inject(method = "addEntity", at = @At(value = "HEAD"))
    private void addEntity(Entity entity, CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new EntitySpawnEvent(entity));
    }
}
