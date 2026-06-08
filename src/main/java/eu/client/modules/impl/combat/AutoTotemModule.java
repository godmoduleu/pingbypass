package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerPopEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.player.SpeedMineModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.InventoryUtils;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;

@RegisterModule(name = "AutoTotem", description = "Automatically puts a specified item in your offhand slot.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class AutoTotemModule extends Module {
    public ModeSetting item = new ModeSetting("Item", "The item that will be placed in your offhand slot when safety conditions are met.", "Totem", new String[]{"Totem", "Crystal", "Gapple"});
    public NumberSetting health = new NumberSetting("Health", "The health at which a totem will be prioritized.", new ModeSetting.Visibility(item, "Crystal", "Gapple"), 16, 0, 36);
    public BooleanSetting elytraCheck = new BooleanSetting("ElytraCheck", "Prioritizes a totem whenever you're wearing an elytra.", true);
    public NumberSetting fallDistance = new NumberSetting("FallDistance", "The fall distance at which the module will prioritize a totem.", 20.0f, 0.0f, 80.0f);
    public BooleanSetting useGapple = new BooleanSetting("UseGapple", "Switches to a golden apple in your offhand when holding right click and holding a sword.", true);
    public BooleanSetting lethalOverride = new BooleanSetting("LethalOverride", "Overrides any necessity for a totem when right-click gappling.", new BooleanSetting.Visibility(useGapple, true), false);
    public BooleanSetting tickAbort = new BooleanSetting("TickAbort", "Enable the interval between switching item which is determine by player ping", true);
    public BooleanSetting smartMine = new BooleanSetting("SmartMine", "Switches to a crystal whenever you start mining and a totem when you aren't mining.", new ModeSetting.Visibility(item, "Crystal"), false);
    public BooleanSetting antiMace = new BooleanSetting("AntiMace", "Switches to a totem if a player near you is trying to smash attack you with a mace.", false);
    public NumberSetting maceRange = new NumberSetting("MaceRange", "The distance at which an enemy has to be in with a mace in order to swap to a totem.", new BooleanSetting.Visibility(antiMace, true), 12.0f, 0.0f, 24.0f);

    private int totemCount = 0;
    private int ticks = 0;

    @SubscribeEvent
    public void onPlayerPop(PlayerPopEvent event) {
        if (shouldRunOnProxy()) return;
        if (event.getPlayer() == mc.player && !EUClient.MODULE_MANAGER.getModule(SuicideModule.class).isToggled()) {
            ticks = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (ticks > 0 && tickAbort.getValue()) {
            ticks--;
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen) && mc.currentScreen instanceof HandledScreen<?>)
            return;

        Item item = getItem();
        if (item == null) return;

        int slot;

        if (item == Items.TOTEM_OF_UNDYING && EUClient.MODULE_MANAGER.getModule(SuicideModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(SuicideModule.class).offhandOverride.getValue()) {
            if (mc.player.getOffHandStack().isEmpty()) return;

            slot = InventoryUtils.findEmptySlot(InventoryUtils.HOTBAR_START, InventoryUtils.INVENTORY_END);
        } else {
            if (mc.player.getOffHandStack().getItem() == item) return;

            slot = InventoryUtils.findInventory(item);
            if (slot == -1) slot = InventoryUtils.find(item);

            if (slot == -1) {
                if (item == Items.TOTEM_OF_UNDYING) slot = InventoryUtils.findEmptySlot(InventoryUtils.HOTBAR_START, InventoryUtils.INVENTORY_END);
                else return;
            }
        }

        if (slot == -1) return;

        InventoryUtils.swap("Pickup", slot, 45);
        ticks = 2 + EUClient.SERVER_MANAGER.getPingDelay();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (shouldRunOnProxy()) return;
        if (mc.player == null || mc.world == null) return;
        totemCount = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
    }

    private Item getItem() {
        if (useGapple.getValue() && mc.options.useKey.isPressed() && (lethalOverride.getValue() || !needsTotem()) && (mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem) && hasItem(Items.ENCHANTED_GOLDEN_APPLE))
            return Items.ENCHANTED_GOLDEN_APPLE;

        if (hasItem(Items.TOTEM_OF_UNDYING)) {
            if (needsTotem()) return Items.TOTEM_OF_UNDYING;

            if (item.getValue().equalsIgnoreCase("Crystal") && smartMine.getValue()) {
                SpeedMineModule module = EUClient.MODULE_MANAGER.getModule(SpeedMineModule.class);
                if ((module.getPrimary() == null || !module.getPrimary().isMining()) && (module.getSecondary() == null || !module.getSecondary().isMining())) {
                    return Items.TOTEM_OF_UNDYING;
                }
            }
        }

        switch (item.getValue()) {
            case "Crystal" -> {
                if (!hasItem(Items.END_CRYSTAL)) return Items.TOTEM_OF_UNDYING;
                return Items.END_CRYSTAL;
            }
            case "Gapple" -> {
                if (!hasItem(Items.ENCHANTED_GOLDEN_APPLE)) return Items.TOTEM_OF_UNDYING;
                return Items.ENCHANTED_GOLDEN_APPLE;
            }
            default -> {
                return Items.TOTEM_OF_UNDYING;
            }
        }
    }

    private boolean needsTotem() {
        if (EUClient.MODULE_MANAGER.getModule(SuicideModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(SuicideModule.class).offhandOverride.getValue()) return false;

        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= health.getValue().floatValue()) return true;
        if (mc.player.fallDistance > fallDistance.getValue().floatValue()) return true;
        if (elytraCheck.getValue() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return true;

        return antiMace.getValue() && (mc.world.getPlayers().stream().anyMatch(entity -> entity != mc.player && !EUClient.FRIEND_MANAGER.contains(entity.getName().getString()) && mc.player.squaredDistanceTo(entity) <= MathHelper.square(maceRange.getValue().floatValue()) && entity.fallDistance >= 1.5 && entity.getMainHandStack().getItem().equals(Items.MACE)));
    }

    private boolean hasItem(Item item) {
        return InventoryUtils.find(item) != -1 || mc.player.getOffHandStack().getItem() == item;
    }

    @Override
    public String getMetaData() {
        return String.valueOf(totemCount);
    }
}
