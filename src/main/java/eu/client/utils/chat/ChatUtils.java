package eu.client.utils.chat;

import eu.client.EUClient;
import eu.client.modules.impl.core.CommandsModule;
import eu.client.utils.text.FormattingUtils;
import net.minecraft.util.StringIdentifiable;

public class ChatUtils {
    public static StringIdentifiable getPrimary() {
        return FormattingUtils.getFormatting(EUClient.MODULE_MANAGER.getModule(CommandsModule.class).primaryMessageColor.getValue());
    }

    public static StringIdentifiable getSecondary() {
        return FormattingUtils.getFormatting(EUClient.MODULE_MANAGER.getModule(CommandsModule.class).secondaryMessageColor.getValue());
    }
}
