package eu.client.mixins.accessors;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("model")
    BakedModel getModel();

    @Accessor("specialModelType")
    SpecialModelRenderer<Object> getSpecialModelType();

    @Accessor("data")
    Object getData();

    @Accessor("glint")
    ItemRenderState.Glint getGlint();

    @Accessor("tints")
    int[] getTints();
}
