package eu.client.mixins.accessors;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Accessor("itemModelManager")
    ItemModelManager getItemModelManager();

    @Invoker("getDynamicDisplayGlintConsumer")
    static VertexConsumer invokeGetDynamicDisplayGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry) {
        throw new AssertionError();
    }

    @Invoker("renderBakedItemModel")
    static void inovkeRenderBakedItemModel(BakedModel model, int[] tints, int light, int overlay, MatrixStack matrices, VertexConsumer vertexConsumer) {
        throw new AssertionError();
    }
}
