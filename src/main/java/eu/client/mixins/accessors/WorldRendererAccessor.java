package eu.client.mixins.accessors;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("frustum")
    Frustum getFrustum();

    @Accessor("entityOutlineFramebuffer")
    Framebuffer getEntityOutlineFramebuffer();

    @Accessor("entityOutlineFramebuffer")
    void setEntityOutlineFramebuffer(Framebuffer framebuffer);
}
