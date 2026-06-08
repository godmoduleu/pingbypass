package eu.client.utils.graphics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.client.EUClient;
import eu.client.mixins.accessors.WorldRendererAccessor;
import eu.client.utils.IMinecraft;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Renderer3D implements IMinecraft {
    public static boolean RENDERING = false;

    public static List<VertexCollection> QUADS = new ArrayList<>();
    public static List<VertexCollection> DEBUG_LINES = new ArrayList<>();

    public static List<VertexCollection> SHINE_QUADS = new ArrayList<>();
    public static List<VertexCollection> SHINE_DEBUG_LINES = new ArrayList<>();

    public static void renderBox(MatrixStack matrices, Box box, Color color) {
        renderGradientBox(matrices, box, color, color);
    }

    public static void renderGradientBox(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        if (!RENDERING) return;
        if (!isFrustumVisible(box)) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        box = cameraTransform(box);

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())));
    }

    public static void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        renderGradientBoxOutline(matrices, box, color, color);
    }

    public static void renderGradientBoxOutline(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        if (!RENDERING) return;
        if (!isFrustumVisible(box)) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        box = cameraTransform(box);

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));
    }

    public static void renderLine(MatrixStack matrices, Vec3d from, Vec3d to, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        from = cameraTransform(from);
        to = cameraTransform(to);

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) from.x, (float) from.y, (float) from.z, color.getRGB()),
                new Vertex(matrix, (float) to.x, (float) to.y, (float) to.z, color.getRGB())));
    }

    public static void renderScaledText(MatrixStack matrices, String text, double x, double y, double z, int scale, boolean background, Color color) {
        float distance = (float) Math.sqrt(mc.getEntityRenderDispatcher().camera.getPos().squaredDistanceTo(x, y, z));
        float scaling = 0.0018f + (scale / 10000.0f) * distance;
        if (distance <= 8.0) scaling = 0.0245f;

        renderText(matrices, text, x, y, z, scaling, background, color);
    }

    public static void renderText(MatrixStack matrices, String text, double x, double y, double z, float scaling, boolean background, Color color) {
        VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

        Vec3d vec3d = new Vec3d(x - mc.getEntityRenderDispatcher().camera.getPos().x, y - mc.getEntityRenderDispatcher().camera.getPos().y, z - mc.getEntityRenderDispatcher().camera.getPos().z);

        matrices.push();
        matrices.translate(vec3d.x, vec3d.y, vec3d.z);
        matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
        matrices.scale(scaling, -scaling, scaling);

        if (background) Renderer2D.renderQuad(matrices, -EUClient.FONT_MANAGER.getWidth(text) / 2.0f - 2, -EUClient.FONT_MANAGER.getHeight() - 2, EUClient.FONT_MANAGER.getWidth(text) / 2.0f + 2, 1, new Color(0, 0, 0, 100));
        EUClient.FONT_MANAGER.drawTextWithShadow(matrices, text, -EUClient.FONT_MANAGER.getWidth(text) / 2, -EUClient.FONT_MANAGER.getHeight(), vertexConsumers, color);

        matrices.pop();
    }

    public static void prepare() {
        QUADS = new ArrayList<>();
        DEBUG_LINES = new ArrayList<>();

        SHINE_QUADS = new ArrayList<>();
        SHINE_DEBUG_LINES = new ArrayList<>();

        RENDERING = true;
    }

    public static void draw(List<VertexCollection> quads, List<VertexCollection> debugLines, boolean shine) {
        RenderSystem.enableBlend();
        if (shine) RenderSystem.blendFunc(770, 32772);
        else RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        if (!quads.isEmpty()) {
            BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (VertexCollection collection : quads) collection.vertex(buffer);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.disableCull();

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.enableCull();
        }

        if (!debugLines.isEmpty()) {
            BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (VertexCollection collection : debugLines) collection.vertex(buffer);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static boolean isFrustumVisible(Box box) {
        return ((WorldRendererAccessor) mc.worldRenderer).getFrustum().isVisible(box);
    }

    private static Vec3d cameraTransform(Vec3d vec3d) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        return new Vec3d(vec3d.x - camera.getX(), vec3d.y - camera.getY(), vec3d.z - camera.getZ());
    }

    private static Box cameraTransform(Box box) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        return new Box(box.minX - camera.getX(), box.minY - camera.getY(), box.minZ - camera.getZ(), box.maxX - camera.getX(), box.maxY - camera.getY(), box.maxZ - camera.getZ());
    }

    public record VertexCollection(Vertex... vertices) {
        public void vertex(BufferBuilder buffer) {
            for (Vertex vertex : vertices) buffer.vertex(vertex.matrix, vertex.x, vertex.y, vertex.z).color(vertex.color);
        }
    }

    public record Vertex(Matrix4f matrix, float x, float y, float z, int color) { }
}
