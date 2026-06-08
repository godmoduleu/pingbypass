package eu.client.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import eu.client.EUClient;
import eu.client.modules.impl.miscellaneous.NameProtectModule;
import eu.client.utils.IMinecraft;
import eu.client.utils.text.CustomFormatting;
import eu.client.utils.text.FormattingUtils;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin implements IMinecraft {
    @WrapOperation(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At(value = "INVOKE", target = "Ljava/lang/String;charAt(I)C", ordinal = 1))
    private static char visitFormatted(String instance, int index, Operation<Character> original, @Local(ordinal = 2) LocalRef<Style> style) {
        CustomFormatting customFormatting = CustomFormatting.byCode(instance.charAt(index));
        if (customFormatting != null) style.set(FormattingUtils.withExclusiveFormatting(style.get(), customFormatting));

        return original.call(instance, index);
    }

    @ModifyVariable(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static String replaceText(String value) {
        NameProtectModule module = EUClient.MODULE_MANAGER.getModule(NameProtectModule.class);
        if (module.isToggled()) return value.replaceAll(mc.getSession().getUsername(), module.name.getValue());
        return value;
    }
}
