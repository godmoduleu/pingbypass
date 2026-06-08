package eu.client.mixins;

import eu.client.Pingbypass;
import eu.client.events.impl.KeyInputEvent;
import eu.client.events.impl.UnfilteredKeyInputEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        Pingbypass.EVENT_HANDLER.post(new UnfilteredKeyInputEvent(key, scancode, action, modifiers));
        if (window == client.getWindow().getHandle() && action == 1 && client.currentScreen == null) {
            Pingbypass.EVENT_HANDLER.post(new KeyInputEvent(key, modifiers));
        }
    }
}
