package eu.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.client.EUClient;
import eu.client.modules.impl.visuals.EntityModifierModule;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalEntityModel.class)
public class EndCrystalEntityModelMixin {
    @ModifyExpressionValue(method = "setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;age:F", ordinal = 0))
    private float modifySpeed(float original) {
        if (EUClient.MODULE_MANAGER.getModule(EntityModifierModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(EntityModifierModule.class).crystals.getValue()) {
            return original * EUClient.MODULE_MANAGER.getModule(EntityModifierModule.class).crystalSpeed.getValue().floatValue();
        }

        return original;
    }
}
