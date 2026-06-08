package eu.client.mixins.accessors;

import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.MultiPhase.class)
public interface RenderLayerMultiPhaseAccessor {
    @Invoker("getPhases")
    RenderLayer.MultiPhaseParameters invokeGetPhases();
}
