package eu.client.mixins;

import eu.client.Pingbypass;
import eu.client.events.impl.KeyboardTickEvent;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick$TAIL(CallbackInfo info) {
        KeyboardTickEvent event = new KeyboardTickEvent(movementForward, movementSideways);
        Pingbypass.EVENT_HANDLER.post(event);
        if (event.isCancelled()) {
            this.movementForward = event.getMovementForward();
            this.movementSideways = event.getMovementSideways();
        }
    }
}
