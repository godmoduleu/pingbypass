package eu.client.utils.text;

import eu.client.EUClient;
import eu.client.mixins.accessors.StyleAccessor;
import eu.client.mixins.accessors.TextColorAccessor;
import eu.client.utils.color.ColorUtils;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;

public class FormattingUtils {
    public static String[] FORMATS = new String[]{"White", "Black", "Blue", "Dark Blue", "Green", "Dark Green", "Aqua", "Dark Aqua", "Red", "Dark Red", "Light Purple", "Dark Purple", "Yellow", "Gold", "Gray", "Dark Gray", "Client", "Rainbow"};

    public static Style withExclusiveFormatting(Style style, CustomFormatting formatting) {
        TextColor textColor = style.getColor();
        if (formatting == CustomFormatting.CLIENT) {
            textColor = TextColorAccessor.create(ColorUtils.getGlobalColor().getRGB(), "CLIENT");
        } else if(formatting == CustomFormatting.RAINBOW) {
            textColor = TextColorAccessor.create(ColorUtils.getGlobalColor().getRGB(), "RAINBOW");
        }

        return StyleAccessor.create(textColor, null, false, false, false, false, false, style.getClickEvent(), style.getHoverEvent(), style.getInsertion(), style.getFont());
    }

    public static StringIdentifiable getFormatting(String str) {
        return switch (str.toLowerCase()) {
            case "black" -> Formatting.BLACK;
            case "blue" -> Formatting.BLUE;
            case "dark blue" -> Formatting.DARK_BLUE;
            case "green" -> Formatting.GREEN;
            case "dark green" -> Formatting.DARK_GREEN;
            case "aqua" -> Formatting.AQUA;
            case "dark aqua" -> Formatting.DARK_AQUA;
            case "red" -> Formatting.RED;
            case "dark red" -> Formatting.DARK_RED;
            case "light purple" -> Formatting.LIGHT_PURPLE;
            case "dark purple" -> Formatting.DARK_PURPLE;
            case "yellow" -> Formatting.YELLOW;
            case "gold" -> Formatting.GOLD;
            case "gray" -> Formatting.GRAY;
            case "dark gray" -> Formatting.DARK_GRAY;
            case "client" -> CustomFormatting.CLIENT;
            case "rainbow" -> CustomFormatting.RAINBOW;
            default -> Formatting.WHITE;
        };
    }

    public static List<String> wrapText(String text, int width) {
        List<String> wrappedText = new ArrayList<>();
        String[] words = text.split(" ");
        String current = "";

        for(String word : words) {
            if(EUClient.FONT_MANAGER.getWidth(current) + EUClient.FONT_MANAGER.getWidth(word) <= width) {
                current += word + " ";
            } else {
                wrappedText.add(current);
                current = word + " ";
            }
        }
        if(EUClient.FONT_MANAGER.getWidth(current) > 0) wrappedText.add(current);

        return wrappedText;
    }

    public static String[] formatSeconds(long seconds) {
        String h = String.format("%02d", seconds/3600);
        String m = String.format("%02d", (seconds%3600)/60);
        String s = String.format("%02d", seconds%60);
        return new String[] {h,m,s};
    }
}