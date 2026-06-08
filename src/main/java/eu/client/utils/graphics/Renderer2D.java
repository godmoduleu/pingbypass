package eu.client.utils.graphics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.client.utils.IMinecraft;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Renderer2D implements IMinecraft {
    public static final Matrix4f LAST_PROJECTION_MATRIX = new Matrix4f();
    public static final Matrix4f LAST_MODEL_MATRIX = new Matrix4f();
    public static final Matrix4f LAST_WORLD_MATRIX = new Matrix4f();

    public static void renderQuad(MatrixStack matrices, float left, float top, float right, float bottom, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, left, top, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, top, 0.0f).color(color.getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderGradient(MatrixStack matrices, float left, float top, float right, float bottom, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, left, top, 0.0f).color(startColor.getRGB());
        buffer.vertex(matrix, left, bottom, 0.0f).color(endColor.getRGB());
        buffer.vertex(matrix, right, bottom, 0.0f).color(endColor.getRGB());
        buffer.vertex(matrix, right, top, 0.0f).color(startColor.getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderSidewaysGradient(MatrixStack matrices, float left, float top, float right, float bottom, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, left, top, 0.0f).color(startColor.getRGB());
        buffer.vertex(matrix, left, bottom, 0.0f).color(startColor.getRGB());
        buffer.vertex(matrix, right, bottom, 0.0f).color(endColor.getRGB());
        buffer.vertex(matrix, right, top, 0.0f).color(endColor.getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderOutline(MatrixStack matrices, float left, float top, float right, float bottom, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, left, top, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left + 0.5f, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left + 0.5f, top, 0.0f).color(color.getRGB());

        buffer.vertex(matrix, right - 0.5f, top, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right - 0.5f, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, top, 0.0f).color(color.getRGB());

        buffer.vertex(matrix, left, bottom - 0.5f, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, bottom, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, bottom - 0.5f, 0.0f).color(color.getRGB());

        buffer.vertex(matrix, left, top, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, left, top + 0.5f, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, top + 0.5f, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, right, top, 0.0f).color(color.getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderLine(MatrixStack matrices, float x, float y, float targetX, float targetY, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, x, y, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, targetX, targetY, 0.0f).color(color.getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderArrow(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x - width, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x + width, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderArrowOutline(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x - width, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x + width, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderCircle(MatrixStack matrices, float x, float y, float radius, Color color) {
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= 360; ++i) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) (x + Math.sin((double) i * 3.141526 / 180.0) * (double) radius), (float) (y + Math.cos((double) i * 3.141526 / 180.0) * (double) radius), 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderTexture(MatrixStack matrices, float left, float top, float right, float bottom, Identifier identifier, Color color) {
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrices.peek().getPositionMatrix(), left, top, 0).texture(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), left, bottom, 0).texture(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), right, bottom, 0).texture(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrices.peek().getPositionMatrix(), right, top, 0).texture(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, identifier);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static Vec3d project(Vec3d vec3d) {
        Vec3d camera = mc.getEntityRenderDispatcher().camera.getPos();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        Vector3f target = new Vector3f();
        Vector4f transform = new Vector4f((float) (vec3d.x - camera.x), (float) (vec3d.y - camera.y), (float) (vec3d.z - camera.z), 1.0f).mul(LAST_WORLD_MATRIX);

        Matrix4f matrixProj = new Matrix4f(LAST_PROJECTION_MATRIX);
        Matrix4f matrixModel = new Matrix4f(LAST_MODEL_MATRIX);

        matrixProj.mul(matrixModel).project(transform.x(), transform.y(), transform.z(), viewport, target);

        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (mc.getWindow().getHeight() - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }
}
