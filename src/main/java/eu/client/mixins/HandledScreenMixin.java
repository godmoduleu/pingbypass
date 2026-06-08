package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.miscellaneous.ShulkerInfoModule;
import eu.client.utils.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {
    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ShulkerInfoModule shulkerInfoModule = EUClient.MODULE_MANAGER.getModule(ShulkerInfoModule.class);

        if(!shulkerInfoModule.isToggled()) return;

        if(focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.playerScreenHandler.getCursorStack().isEmpty() && shulkerInfoModule.hasItems(focusedSlot.getStack())) {
            shulkerInfoModule.renderInfo(context, mouseX, mouseY, focusedSlot.getStack());
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void drawMouseoverTooltip(DrawContext drawContext, int x, int y, CallbackInfo ci) {
        ShulkerInfoModule shulkerInfoModule = EUClient.MODULE_MANAGER.getModule(ShulkerInfoModule.class);

        if(!shulkerInfoModule.isToggled()) return;

        if(focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.playerScreenHandler.getCursorStack().isEmpty() && shulkerInfoModule.hasItems(focusedSlot.getStack())) {
            ci.cancel();
        }
    }
}
