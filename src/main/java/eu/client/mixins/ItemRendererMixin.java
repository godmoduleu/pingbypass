package eu.client.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.client.EUClient;
import eu.client.modules.impl.visuals.EntityModifierModule;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.*;
import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow
    private static int getTint(int[] tints, int index) {
        return 0;
    }

    @WrapOperation(method = "renderItem(Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;[IIILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"))
    private static void renderItem$renderBakedItemModel(BakedModel model, int[] tints, int light, int overlay, MatrixStack matrices, VertexConsumer vertexConsumer, Operation<Void> original, @Local(argsOnly = true) ModelTransformationMode renderMode) {
        EntityModifierModule module = EUClient.MODULE_MANAGER.getModule(EntityModifierModule.class);
        Color color = module.isToggled() && module.items.getValue() && (renderMode != ModelTransformationMode.GUI || module.itemGlobal.getValue()) ? module.itemColor.getColor() : null;

        Random random = Random.create();
        long l = 42L;

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            euclient$renderBakedItemQuads(matrices, vertexConsumer, model.getQuads(null, direction, random), tints, color, light, overlay);
        }

        random.setSeed(42L);
        euclient$renderBakedItemQuads(matrices, vertexConsumer, model.getQuads(null, null, random), tints, color, light, overlay);
    }

    @Unique
    private static void euclient$renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, Color color, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();

        for (BakedQuad bakedQuad : quads) {
            float f;
            float g;
            float h;
            float j;

            if (bakedQuad.hasTint()) {
                int i = getTint(tints, bakedQuad.getTintIndex());
                f = (float) (color == null ? ColorHelper.getAlpha(i) : color.getAlpha()) / 255.0F;
                g = (float) (color == null ? ColorHelper.getRed(i) : color.getRed()) / 255.0F;
                h = (float) (color == null ? ColorHelper.getGreen(i) : color.getGreen()) / 255.0F;
                j = (float) (color == null ? ColorHelper.getBlue(i) : color.getBlue()) / 255.0F;
            } else {
                f = 1.0F;
                g = 1.0F;
                h = 1.0F;
                j = 1.0F;
            }

            vertexConsumer.quad(entry, bakedQuad, g, h, j, f, light, overlay);
        }
    }
}