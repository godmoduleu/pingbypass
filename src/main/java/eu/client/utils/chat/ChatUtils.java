package eu.client.utils.chat;

import eu.client.Pingbypass;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

public class ChatUtils {
    public static StringIdentifiable getPrimary() {
        return Formatting.WHITE;
    }

    public static StringIdentifiable getSecondary() {
        return Formatting.GRAY;
    }
}
