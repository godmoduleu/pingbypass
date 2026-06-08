package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.miscellaneous.ExtraTabModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Shadow protected abstract Text applyGameModeFormatting(PlayerListEntry entry, MutableText name);

    @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntries(CallbackInfoReturnable<List<PlayerListEntry>> info) {
        if (EUClient.MODULE_MANAGER.getModule(ExtraTabModule.class).isToggled()) {
            info.setReturnValue(client.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(EUClient.MODULE_MANAGER.getModule(ExtraTabModule.class).limit.getValue().longValue()).toList());
        }
    }

    @Inject(method = "getPlayerName", at = @At(value = "HEAD"), cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> info) {
        if (EUClient.MODULE_MANAGER.getModule(ExtraTabModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(ExtraTabModule.class).friends.getValue() && EUClient.FRIEND_MANAGER.contains(entry.getProfile().getName())) {
            if (entry.getDisplayName() != null) {
                MutableText text = Text.empty();

                for (Text sibling : entry.getDisplayName().getSiblings()) {
                    if (sibling.getString().equals(entry.getProfile().getName())) {
                        text.append(Text.literal(entry.getProfile().getName()).formatted(Formatting.AQUA));
                        continue;
                    }

                    if (sibling.getString().equals("] " + entry.getProfile().getName())) {
                        text.append(Text.literal("] ").formatted(Formatting.WHITE).append(Text.literal(entry.getProfile().getName()).formatted(Formatting.AQUA)));
                        continue;
                    }

                    text.append(sibling);
                }

                info.setReturnValue(applyGameModeFormatting(entry, text));
                return;
            }

            info.setReturnValue(applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName()))));
        }
    }
}
