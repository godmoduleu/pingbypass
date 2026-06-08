package eu.client.managers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import eu.client.EUClient;
import eu.client.mixins.accessors.*;
import eu.client.utils.IMinecraft;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class ShaderManager implements IMinecraft {
    private final OutlineVertexConsumerProvider vertexConsumerProvider = new OutlineVertexConsumerProvider(VertexConsumerProvider.immediate(new BufferAllocator(256)));
    private final Framebuffer framebuffer;

    private final RenderPhase.Target target;
    private final Function<RenderPhase.TextureBase, RenderLayer> layerCreator;

    public ShaderManager() {
        framebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
        framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        target = new RenderPhase.Target("shader_target", () -> framebuffer.beginWrite(false), () -> mc.getFramebuffer().beginWrite(false));
        layerCreator = memoizeTexture(texture -> RenderLayer.of("euclient_overlay", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, 1536, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.OUTLINE_PROGRAM).texture(texture).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).target(target).build(RenderLayer.OutlineMode.IS_OUTLINE)));
    }

    public void prepare() {
        framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clear();

        mc.getFramebuffer().beginWrite(false);
    }

    public void render(int renderMode, float opacity) {
        PostEffectProcessor shader = mc.getShaderLoader().loadPostEffect(Identifier.of(EUClient.MOD_ID, "outline"), DefaultFramebufferSet.MAIN_ONLY);
        ShaderProgram program = ((PostEffectProcessorAccessor) shader).getPasses().getFirst().getProgram();

        program.addSamplerTexture("DiffuseSampler", framebuffer.getColorAttachment());
        program.getUniform("RenderMode").set(renderMode);
        program.getUniform("FillOpacity").set(opacity);

        shader.render(framebuffer, ((GameRendererAccessor) mc.gameRenderer).getPool());
        mc.getFramebuffer().beginWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);

        framebuffer.drawInternal(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());

        RenderSystem.disableBlend();
    }

    public void resize(int width, int height) {
        if (framebuffer != null) framebuffer.resize(width, height);
    }

    public VertexConsumerProvider create(VertexConsumerProvider parent, Color color) {
        return layer -> {
            VertexConsumer parentBuffer = parent.getBuffer(layer);

            if (!(layer instanceof RenderLayer.MultiPhase) || ((RenderLayerMultiPhaseParametersAccessor) (Object) ((RenderLayerMultiPhaseAccessor) layer).invokeGetPhases()).getOutlineMode() == RenderLayer.OutlineMode.NONE) {
                return parentBuffer;
            }

            vertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 255);

            VertexConsumer outlineBuffer = vertexConsumerProvider.getBuffer(layerCreator.apply(((RenderLayerMultiPhaseParametersAccessor) (Object) ((RenderLayerMultiPhaseAccessor) layer).invokeGetPhases()).getTexture()));
            return outlineBuffer != null ? VertexConsumers.union(outlineBuffer, parentBuffer) : parentBuffer;
        };
    }

    private Function<RenderPhase.TextureBase, RenderLayer> memoizeTexture(Function<RenderPhase.TextureBase, RenderLayer> function) {
        return new Function<>() {
            private final Map<Identifier, RenderLayer> cache = new HashMap<>();

            public RenderLayer apply(RenderPhase.TextureBase texture) {
                return this.cache.computeIfAbsent(((RenderPhaseTextureBaseAccessor) texture).invokeGetId().get(), id -> function.apply(texture));
            }
        };
    }
}
