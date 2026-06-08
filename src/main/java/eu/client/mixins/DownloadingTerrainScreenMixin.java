package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.MenuModule;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(DownloadingTerrainScreen.class)
public class DownloadingTerrainScreenMixin extends Screen {
    @Shadow @Final private DownloadingTerrainScreen.WorldEntryReason worldEntryReason;

    protected DownloadingTerrainScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(worldEntryReason.equals(DownloadingTerrainScreen.WorldEntryReason.OTHER) && EUClient.MODULE_MANAGER.getModule(MenuModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(MenuModule.class).mainMenu.getValue()) {
            Renderer2D.renderQuad(context.getMatrices(), 0, 0, width, height, new Color(25, 25, 25, 255));

            for (int i = 0; i < width; i++) {
                Color color = ColorUtils.getRainbow(2L, 0.7f, 1.0f, 255, i * 5L);
                Renderer2D.renderQuad(context.getMatrices(), i, 0, i + 1, 1, color);
            }

            ci.cancel();
        }
    }
}
