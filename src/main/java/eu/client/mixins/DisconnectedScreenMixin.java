package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.miscellaneous.AutoReconnectModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {
    @Shadow @Final private DirectionalLayoutWidget grid;

    @Unique private ButtonWidget toggleButton;
    @Unique private ButtonWidget button;
    @Unique private double time = 100.0;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V", shift = At.Shift.BEFORE))
    private void init(CallbackInfo info) {
        if (EUClient.SERVER_MANAGER.getLastConnection() != null) {
            button = new ButtonWidget.Builder(Text.literal(getText()), button -> tryConnecting()).width(200).build();
            toggleButton = new ButtonWidget.Builder(Text.literal("Toggle " + (EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).isToggled() ? Formatting.GREEN : Formatting.RED) + "AutoReconnect"), button -> {
                EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).setToggled(!EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).isToggled(), false);

                this.toggleButton.setMessage(Text.literal("Toggle " + (EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).isToggled() ? Formatting.GREEN : Formatting.RED) + "AutoReconnect"));
                this.button.setMessage(Text.literal(getText()));

                time = EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).delay.getValue().intValue() * 20;
            }).width(200).build();

            grid.add(button);
            grid.add(toggleButton);
        }
    }

    @Override
    public void tick() {
        if (!EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).isToggled() || EUClient.SERVER_MANAGER.getLastConnection() == null) return;

        if (time <= 0) {
            tryConnecting();
        } else {
            time--;
            if (button != null) button.setMessage(Text.literal(getText()));
        }
    }

    @Unique
    private String getText() {
        String reconnectText = "Reconnect";
        if (EUClient.MODULE_MANAGER.getModule(AutoReconnectModule.class).isToggled()) reconnectText += " " + String.format("(" + Formatting.GREEN + "%.1fs" + Formatting.RESET + ")", time / 20);

        return reconnectText;
    }

    @Unique
    private void tryConnecting() {
        ConnectScreen.connect(new TitleScreen(), MinecraftClient.getInstance(), EUClient.SERVER_MANAGER.getLastConnection().left(), EUClient.SERVER_MANAGER.getLastConnection().right(), false, null);
    }
}
