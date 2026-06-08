package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.PingBypassModule;
import eu.client.pingbypass.PingBypassFlags;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (!PingBypassFlags.proxyForwardingActive) return;

        // Position below all vanilla buttons (the grid ends around height/4 + 120 + 24)
        addDrawableChild(ButtonWidget.builder(Text.literal("§cDisconnect Proxy"), button -> {
            PingBypassModule pbModule = EUClient.MODULE_MANAGER.getModule(PingBypassModule.class);
            if (pbModule != null && pbModule.isToggled()) {
                pbModule.setToggled(false);
            }
        }).dimensions(width / 2 - 102, height - 40, 204, 20).build());
    }
}
