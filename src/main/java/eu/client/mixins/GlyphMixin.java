package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.FontModule;
import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Glyph.class)
public interface GlyphMixin {
    @Inject(method = "getShadowOffset", at = @At("HEAD"), cancellable = true)
    private void getShadowOffset(CallbackInfoReturnable<Float> info) {
        if (EUClient.MODULE_MANAGER != null && EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && !EUClient.MODULE_MANAGER.getModule(FontModule.class).shadowMode.getValue().equalsIgnoreCase("Default")) {
            info.setReturnValue(EUClient.FONT_MANAGER.getShadowOffset());
        }
    }
}
