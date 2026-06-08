package eu.client.mixins;

import eu.client.pingbypass.PingBypassFlags;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/**
 * Safety net mixin — clears registry loading errors when PingBypass flag is set.
 * Should not be needed with proper RegistryCache, but kept as a fallback.
 */
@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {

    @ModifyVariable(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;isEmpty()Z"
        ),
        ordinal = 0
    )
    private static Map<RegistryKey<?>, Exception> euclient$clearErrors(Map<RegistryKey<?>, Exception> errors) {
        if (PingBypassFlags.tolerateRegistryErrors && !errors.isEmpty()) {
            PingBypassFlags.tolerateRegistryErrors = false;
            errors.clear();
        }
        return errors;
    }
}
