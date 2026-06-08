package eu.client.mixins.accessors;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public interface RenderLayerMultiPhaseParametersAccessor {
    @Accessor("outlineMode")
    RenderLayer.OutlineMode getOutlineMode();

    @Accessor("texture")
    RenderPhase.TextureBase getTexture();
}
