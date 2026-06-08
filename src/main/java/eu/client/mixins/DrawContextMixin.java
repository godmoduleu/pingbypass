package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.FontModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow public abstract void draw();

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"))
    private void drawText(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            draw();
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"))
    private void drawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            draw();
        }
    }
}
