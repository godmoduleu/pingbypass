package eu.client.utils.font;

import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import lombok.Getter;
import eu.client.mixins.accessors.NativeImageAccessor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class GlyphMap {
    private NativeImageBackedTexture texture;

    private final Font[] fonts;
    private int width;
    private int height;

    private final char include;
    private final char exclude;

    private final int padding;
    private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap<>();

    boolean generated = false;

    public GlyphMap(Font[] fonts, char include, char exclude, int padding) {
        this.fonts = fonts;
        this.include = include;
        this.exclude = exclude;
        this.padding = padding;
    }

    public void generate() {
        synchronized (this) {
            privateGenerate();
        }
    }

    private void privateGenerate() {
        if (generated) return;

        int range = exclude - include - 1;
        int charsVert = (int) (Math.ceil(Math.sqrt(range)) * 1.5);
        int generatedChars = 0;
        int charNX = 0;
        int maxX = 0, maxY = 0;
        int currentX = 0, currentY = 0;
        int currentRowMaxY = 0;

        List<Glyph> glyphs = new ArrayList<>();

        AffineTransform affineTransform = new AffineTransform();
        FontRenderContext context = new FontRenderContext(affineTransform, true, true);

        while (generatedChars <= range) {
            char currentChar = (char) (include + generatedChars);
            Font font = getFontForGlyph(currentChar);
            Rectangle2D stringBounds = font.getStringBounds(String.valueOf(currentChar), context);

            int width = (int) Math.ceil(stringBounds.getWidth());
            int height = (int) Math.ceil(stringBounds.getHeight());
            generatedChars++;
            maxX = Math.max(maxX, currentX + width);
            maxY = Math.max(maxY, currentY + height);

            if (charNX >= charsVert) {
                currentX = 0;
                currentY += currentRowMaxY + padding;
                charNX = 0;
                currentRowMaxY = 0;
            }

            currentRowMaxY = Math.max(currentRowMaxY, height);
            glyphs.add(new Glyph(this, currentX, currentY, width, height, currentChar));
            currentX += width + padding;
            charNX++;
        }

        BufferedImage bufferedImage = new BufferedImage(Math.max(maxX + padding, 1), Math.max(maxY + padding, 1), BufferedImage.TYPE_INT_ARGB);
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();

        Graphics2D graphics = bufferedImage.createGraphics();

        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.WHITE);

        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (Glyph glyph : glyphs) {
            graphics.setFont(getFontForGlyph(glyph.value()));
            FontMetrics fontMetrics = graphics.getFontMetrics();
            graphics.drawString(String.valueOf(glyph.value()), glyph.u(), glyph.v() + fontMetrics.getAscent());
            this.glyphs.put(glyph.value(), glyph);
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        IntBuffer backingBuffer = MemoryUtil.memIntBuffer(((NativeImageAccessor) (Object) image).getPointer(), image.getWidth() * image.getHeight());

        Object object;
        WritableRaster raster = bufferedImage.getRaster();
        ColorModel colorModel = bufferedImage.getColorModel();

        int numBands = raster.getNumBands();
        int dataType = raster.getDataBuffer().getDataType();

        object = switch (dataType) {
            case DataBuffer.TYPE_BYTE -> new byte[numBands];
            case DataBuffer.TYPE_USHORT -> new short[numBands];
            case DataBuffer.TYPE_INT -> new int[numBands];
            case DataBuffer.TYPE_FLOAT -> new float[numBands];
            case DataBuffer.TYPE_DOUBLE -> new double[numBands];
            default -> {
                throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
            }
        };

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                raster.getDataElements(x, y, object);
                backingBuffer.put(colorModel.getAlpha(object) << 24 | colorModel.getBlue(object) << 16 | colorModel.getGreen(object) << 8 | colorModel.getRed(object));
            }
        }

        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        texture.upload();
        this.texture = texture;

        generated = true;
    }

    public Glyph getGlyph(char character) {
        synchronized (this) {
            if (!generated) privateGenerate();
            return glyphs.get(character);
        }
    }

    public void destroy() {
        synchronized (this) {
            generated = false;

            if (texture != null) texture.close();
            glyphs.clear();

            this.width = -1;
            this.height = -1;
        }
    }

    public boolean contains(char c) {
        return c >= include && c < exclude;
    }

    private Font getFontForGlyph(char c) {
        for (Font font1 : this.fonts) {
            if (font1.canDisplay(c)) {
                return font1;
            }
        }

        return this.fonts[0];
    }
}