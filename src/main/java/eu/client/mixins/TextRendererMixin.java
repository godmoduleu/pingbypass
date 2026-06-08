package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.FontModule;
import eu.client.utils.IMinecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
    @Inject(method = "drawLayer(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)F", at = @At("HEAD"), cancellable = true)
    private void drawLayer$String(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, TextRenderer.TextLayerType layerType, int backgroundColor, int light, boolean swapZIndex, CallbackInfoReturnable<Float> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            MatrixStack matrices = new MatrixStack();

            matrices.push();
            matrices.multiplyPositionMatrix(matrix);

            if (shadow) EUClient.FONT_MANAGER.getFontRenderer().drawString(matrices, text, x + EUClient.FONT_MANAGER.getShadowOffset(), y + EUClient.FONT_MANAGER.getShadowOffset(), color, true);
            EUClient.FONT_MANAGER.getFontRenderer().drawString(matrices, text, x, y, color, false);

            matrices.pop();

            info.setReturnValue(x + EUClient.FONT_MANAGER.getFontRenderer().getTextWidth(text));
        }
    }

    @Inject(method = "drawLayer(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)F", at = @At("HEAD"), cancellable = true)
    private void drawLayer$OrderedText(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, TextRenderer.TextLayerType layerType, int underlineColor, int light, boolean swapZIndex, CallbackInfoReturnable<Float> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            MatrixStack matrices = new MatrixStack();

            matrices.push();
            matrices.multiplyPositionMatrix(matrix);

            if (shadow) EUClient.FONT_MANAGER.getFontRenderer().drawText(matrices, text, x + EUClient.FONT_MANAGER.getShadowOffset(), y + EUClient.FONT_MANAGER.getShadowOffset(), color, true);
            EUClient.FONT_MANAGER.getFontRenderer().drawText(matrices, text, x, y, color, false);

            matrices.pop();

            info.setReturnValue(x + EUClient.FONT_MANAGER.getFontRenderer().getTextWidth(text));
        }
    }

    @Inject(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void getWidth(String text, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            info.setReturnValue(EUClient.FONT_MANAGER.getWidth(text));
        }
    }

    @Inject(method = "getWidth(Lnet/minecraft/text/StringVisitable;)I", at = @At("HEAD"), cancellable = true)
    private void getWidth(StringVisitable text, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            info.setReturnValue(EUClient.FONT_MANAGER.getWidth(text.getString()));
        }
    }

    @Inject(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), cancellable = true)
    private void getWidth(OrderedText text, CallbackInfoReturnable<Integer> info) {
        if (EUClient.MODULE_MANAGER.getModule(FontModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FontModule.class).customFont.getValue() && EUClient.MODULE_MANAGER.getModule(FontModule.class).global.getValue()) {
            info.setReturnValue((int) EUClient.FONT_MANAGER.getFontRenderer().getTextWidth(text));
        }
    }
}
