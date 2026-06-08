package eu.client.utils.graphics;

import eu.client.EUClient;
import eu.client.modules.impl.visuals.NoInterpolationModule;
import eu.client.utils.IMinecraft;
import eu.client.utils.mixins.ILivingEntityRenderer;
import eu.client.utils.mixins.IMultiPhase;
import eu.client.utils.mixins.IMultiPhaseParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.*;

import java.awt.*;
import java.util.List;

public class ModelRenderer implements IMinecraft {
    private static Render render;
    private static Matrix4f matrix4f;
    private static Vec3d offset;
    private static Vec3d camera;

    public static void renderModel(Entity entity, float scale, float tickDelta, Render render) {
        renderModel(entity, false, scale, tickDelta, render);
    }

    public static void renderModel(Entity entity, boolean staticEntity, float scale, float tickDelta, Render render) {
        ModelRenderer.render = render;
        ModelRenderer.camera = mc.gameRenderer.getCamera().getPos();

        if (!EUClient.MODULE_MANAGER.getModule(NoInterpolationModule.class).isToggled()) ModelRenderer.offset = new Vec3d(MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()), MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()), MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()));
        else ModelRenderer.offset = new Vec3d(entity.getX(), entity.getY(), entity.getZ());

        EntityRenderer<?, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState renderState = ((EntityRenderer<Entity, ?>) renderer).getAndUpdateRenderState(entity, tickDelta);

        MatrixStack matrices = new MatrixStack();
        ModelRenderer.matrix4f = matrices.peek().getPositionMatrix();

        matrices.push();
        matrices.scale(scale, scale, scale);

        if (renderer instanceof LivingEntityRenderer<?, ?, ?> && renderState instanceof LivingEntityRenderState state) {
            ((ILivingEntityRenderer) renderer).euclient$render(state, matrices, CustomVertexConsumerProvider.INSTANCE, 15);
        }

        if (renderer instanceof EndCrystalEntityRenderer crystalRenderer && renderState instanceof EndCrystalEntityRenderState state) {
            crystalRenderer.render(state, matrices, CustomVertexConsumerProvider.INSTANCE, 15);
        }

        matrices.push();
    }

    private static class CustomVertexConsumerProvider implements VertexConsumerProvider {
        public static final CustomVertexConsumerProvider INSTANCE = new CustomVertexConsumerProvider();

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            if (layer instanceof IMultiPhase phase && ((IMultiPhaseParameters) (Object) phase.euclient$getParameters()).euclient$getTarget() == RenderLayer.ITEM_ENTITY_TARGET) {
                return EmptyVertexConsumer.INSTANCE;
            }

            return CustomVertexConsumer.INSTANCE;
        }
    }

    private static class CustomVertexConsumer implements VertexConsumer {
        public static final CustomVertexConsumer INSTANCE = new CustomVertexConsumer();
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];

        private int i = 0;

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            xs[i] = x;
            ys[i] = y;
            zs[i] = z;

            i++;

            if (i == 4) {
                List<Renderer3D.VertexCollection> quads = render.shine() ? Renderer3D.SHINE_QUADS : Renderer3D.QUADS;
                List<Renderer3D.VertexCollection> debugLines = render.shine() ? Renderer3D.SHINE_DEBUG_LINES : Renderer3D.DEBUG_LINES;

                if (render.fill) {
                    quads.add(new Renderer3D.VertexCollection(new Renderer3D.Vertex(matrix4f, (float) (offset.getX() + xs[0] - camera.getX()), (float) (offset.getY() + ys[0] - camera.getY()), (float) (offset.getZ() + zs[0] - camera.getZ()), render.fillColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.getX() + xs[1] - camera.getX()), (float) (offset.getY() + ys[1] - camera.getY()), (float) (offset.getZ() + zs[1] - camera.getZ()), render.fillColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.getX() + xs[2] - camera.getX()), (float) (offset.getY() + ys[2] - camera.getY()), (float) (offset.getZ() + zs[2] - camera.getZ()), render.fillColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.getX() + xs[3] - camera.getX()), (float) (offset.getY() + ys[3] - camera.getY()), (float) (offset.getZ() + zs[3] - camera.getZ()), render.fillColor().getRGB())));
                }

                if (render.outline) {
                    debugLines.add(new Renderer3D.VertexCollection(new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[0] - camera.getX()), (float) (offset.y + ys[0] - camera.getY()), (float) (offset.z + zs[0] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[1] - camera.getX()), (float) (offset.y + ys[1] - camera.getY()), (float) (offset.z + zs[1] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[1] - camera.getX()), (float) (offset.y + ys[1] - camera.getY()), (float) (offset.z + zs[1] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[2] - camera.getX()), (float) (offset.y + ys[2] - camera.getY()), (float) (offset.z + zs[2] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[2] - camera.getX()), (float) (offset.y + ys[2] - camera.getY()), (float) (offset.z + zs[2] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[3] - camera.getX()), (float) (offset.y + ys[3] - camera.getY()), (float) (offset.z + zs[3] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[0] - camera.getX()), (float) (offset.y + ys[0] - camera.getY()), (float) (offset.z + zs[0] - camera.getZ()), render.outlineColor().getRGB()),
                            new Renderer3D.Vertex(matrix4f, (float) (offset.x + xs[0] - camera.getX()), (float) (offset.y + ys[0] - camera.getY()), (float) (offset.z + zs[0] - camera.getZ()), render.outlineColor().getRGB())));
                }

                i = 0;
            }

            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }

    private static class EmptyVertexConsumer implements VertexConsumer {
        private static final EmptyVertexConsumer INSTANCE = new EmptyVertexConsumer();

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }

    public record Render(boolean fill, Color fillColor, boolean outline, Color outlineColor, boolean shine) { }
}