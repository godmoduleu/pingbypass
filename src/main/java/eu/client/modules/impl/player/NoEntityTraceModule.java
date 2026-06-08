package eu.client.modules.impl.player;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.WhitelistSetting;
import net.minecraft.item.PickaxeItem;

@RegisterModule(name = "NoEntityTrace", description = "Allows you to interact with blocks, bypassing the entities between you and the block.", category = Module.Category.PLAYER)
public class NoEntityTraceModule extends Module {
    public BooleanSetting pickaxeOnly = new BooleanSetting("PickaxeOnly", "Only bypasses entities if you're holding a pickaxe.", false);
    public WhitelistSetting ignoredItem = new WhitelistSetting("IgnoredItem", "Do not bypasses entities if you're holding these items.", WhitelistSetting.Type.ITEMS);

    public boolean shouldIgnore() {
        if(pickaxeOnly.getValue() && mc.player.getMainHandStack().getItem() instanceof PickaxeItem) return true;
        return !ignoredItem.isWhitelistContains(mc.player.getMainHandStack().getItem());
    }
}
