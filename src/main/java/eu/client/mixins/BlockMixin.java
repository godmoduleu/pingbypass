package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.movement.IceSpeedModule;
import eu.client.modules.impl.movement.NoSlowModule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "getSlipperiness", at = @At("HEAD"), cancellable = true)
    private void getSlipperiness(CallbackInfoReturnable<Float> info) {
        if ((((Object) this) == Blocks.ICE || ((Object) this) == Blocks.PACKED_ICE || ((Object) this) == Blocks.BLUE_ICE || ((Object) this) == Blocks.FROSTED_ICE) && EUClient.MODULE_MANAGER.getModule(IceSpeedModule.class).isToggled()) {
            info.setReturnValue(1.0f - (0.8f * EUClient.MODULE_MANAGER.getModule(IceSpeedModule.class).speed.getValue().floatValue()));
        }

        if ((Object) this == Blocks.SLIME_BLOCK && EUClient.MODULE_MANAGER.getModule(NoSlowModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(NoSlowModule.class).slimeBlocks.getValue()) {
            info.setReturnValue(0.6f);
        }
    }
}
