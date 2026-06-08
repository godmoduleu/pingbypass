package eu.client.utils.color;

import eu.client.EUClient;
import eu.client.modules.impl.core.ColorModule;
import eu.client.settings.impl.ColorSetting;
import net.minecraft.util.Formatting;

import java.awt.*;

public class ColorUtils {
    public static ColorSetting.Color getDefaultColor() {
        return new ColorSetting.Color(new Color(160, 120, 255), true, false);
    }

    public static ColorSetting.Color getDefaultFillColor() {
        return new ColorSetting.Color(new Color(160, 120, 255, 40), true, false);
    }

    public static ColorSetting.Color getDefaultOutlineColor() {
        return new ColorSetting.Color(new Color(160, 120, 255, 120), true, false);
    }

    public static Color getGlobalColor() {
        return EUClient.MODULE_MANAGER.getModule(ColorModule.class).color.getColor();
    }

    public static Color getGlobalColor(int alpha) {
        return getColor(EUClient.MODULE_MANAGER.getModule(ColorModule.class).color.getColor(), alpha);
    }

    public static Color getColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color getRainbow() {
        return getRainbow(255);
    }

    public static Color getOffsetRainbow(long index) {
        return getOffsetRainbow(255, index);
    }

    public static Color getRainbow(int alpha) {
        return getRainbow(EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowSpeed.getValue().longValue(), EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowSaturation.getValue().floatValue() / 100.0f, EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowBrightness.getValue().floatValue() / 100.0f, alpha);
    }

    public static Color getOffsetRainbow(int alpha, long index) {
        return getRainbow(EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowSpeed.getValue().longValue(), EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowSaturation.getValue().floatValue() / 100.0f, EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowBrightness.getValue().floatValue() / 100.0f, alpha, index);
    }

    public static Color getRainbow(long speed, float saturation, float brightness, int alpha) {
        return getRainbow(speed, saturation, brightness, alpha, 0);
    }

    public static Color getRainbow(long speed, float saturation, float brightness, int alpha, long index) {
        speed = Math.clamp(speed, 1, 20);

        float hue = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500.0f - (500.0f * (float) speed));
        Color color = new Color(Color.HSBtoRGB(Math.clamp(hue, 0.0f, 1.0f), Math.clamp(saturation, 0.0f, 1.0f), Math.clamp(brightness, 0.0f, 1.0f)));

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color getOffsetWave(Color color, long index) {
        return getWave(color, EUClient.MODULE_MANAGER.getModule(ColorModule.class).rainbowSpeed.getValue().longValue(), 255, index);
    }

    public static Color getWave(Color color, long speed, int alpha, long index) {
        speed = Math.max(1, Math.min(speed, 20));

        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);

        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500.0f - (500.0f * (float) speed));
        float adjustedBrightness = Math.abs((cycle * 2.0f) % 2.0f - 1.0f);
        hsb[2] = 0.5F + 0.5F * adjustedBrightness;

        Color resultColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), alpha);
    }

    public static Color getPulse(Color color) {
        return getPulse(color, 15);
    }

    public static Color getPulse(Color color, long speed) {
        speed = Math.max(1, Math.min(speed, 20));

        double sin = Math.sin(2 * Math.PI * (speed/20f) * ((System.currentTimeMillis() - EUClient.UPTIME)/1000f));
        double scale = (sin + 1)/2f;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * scale));
    }

    public static Color getHashColor(String text) {
        int hash = text.hashCode();
        return new Color((hash & 0xFF0000) >> 16, (hash & 0x00FF00) >> 8, (hash & 0x0000FF));
    }

    public static boolean isValidColorCode(String code) {
        if (code.startsWith("#")) code = code.substring(1);
        if (!(code.length() == 3 || code.length() == 6)) return false;

        for (int i = 0; i < code.length(); i++) {
            if (!((code.charAt(i) >= '0' && code.charAt(i) <= 9) || (code.charAt(i) >= 'a' && code.charAt(i) <= 'f') || (code.charAt(i) >= 'A' || code.charAt(i) <= 'F'))) {
                return false;
            }
        }

        return true;
    }

    public static Formatting getHealthColor(double health) {
        if (health > 18.0) return Formatting.GREEN;
        else if (health > 16.0) return Formatting.DARK_GREEN;
        else if (health > 12.0) return Formatting.YELLOW;
        else if (health > 8.0) return Formatting.GOLD;
        else if (health > 5.0) return Formatting.RED;

        return Formatting.DARK_RED;
    }

    public static Formatting getTotemColor(int pops) {
        if (pops == 1) return Formatting.GREEN;
        else if (pops == 2) return Formatting.DARK_GREEN;
        else if (pops == 3) return Formatting.YELLOW;
        else if (pops == 4) return Formatting.GOLD;
        else if (pops == 5) return Formatting.RED;

        return Formatting.DARK_RED;
    }
}
