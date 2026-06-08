package eu.client.utils.minecraft;

import eu.client.EUClient;
import eu.client.mixins.accessors.ClientPlayerInteractionManagerAccessor;
import eu.client.utils.IMinecraft;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtils implements IMinecraft {
    public static String[] SWITCH_MODES = new String[]{"None", "Normal", "Silent", "AltPickup", "AltSwap"};
    public static String[] SWAP_MODES = new String[]{"Pickup", "Swap"};

    public static int HOTBAR_START = 0;
    public static int HOTBAR_END = 8;

    public static int INVENTORY_START = 9;
    public static int INVENTORY_END = 35;

    public static void switchSlot(String mode, int slot, int previousSlot) {
        if (mode.equalsIgnoreCase("None")) return;
        if (slot == -1 || previousSlot == -1 || slot == EUClient.POSITION_MANAGER.getServerSlot()) return;

        switch (mode) {
            case "Normal" -> {
                mc.player.getInventory().selectedSlot = slot;
                ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
            }
            case "Silent" -> mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            case "AltPickup" -> swap("Pickup", slot, previousSlot);
            case "AltSwap" -> swap("Swap", slot, previousSlot);
        }
    }

    public static void switchBack(String mode, int slot, int previousSlot) {
        if (mode.equalsIgnoreCase("None")) return;
        if (previousSlot == -1) return;

        switch (mode) {
            case "Silent" -> {
                if (previousSlot == EUClient.POSITION_MANAGER.getServerSlot()) return;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            }
            case "AltPickup" -> swap("Pickup", slot, previousSlot);
            case "AltSwap" -> swap("Swap", slot, previousSlot);
        }
    }

    public static void swap(String mode, int slot, int targetSlot) {
        switch (mode) {
            case "Pickup" -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(targetSlot), 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
            }
            case "Swap" -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), targetSlot, SlotActionType.SWAP, mc.player);
            }
        }
    }

    public static int indexToSlot(int index) {
        if (index >= 0 && index <= 8) return 36 + index;
        return index;
    }

    public static int find(Item item) { return find(item, HOTBAR_START, INVENTORY_END); }
    public static int findHotbar(Item item) { return find(item, HOTBAR_START, HOTBAR_END); }
    public static int findInventory(Item item) { return find(item, INVENTORY_START, INVENTORY_END); }
    public static int find(Item item, int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != item) continue;

            return i;
        }

        return -1;
    }

    public static int find(Class<? extends Item> item) { return find(item, HOTBAR_START, INVENTORY_END); }
    public static int findHotbar(Class<? extends Item> item) { return find(item, HOTBAR_START, HOTBAR_END); }
    public static int findInventory(Class<? extends Item> item) { return find(item, INVENTORY_START, INVENTORY_END); }
    public static int find(Class<? extends Item> item, int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.getItem().getClass().isAssignableFrom(item)) continue;

            return i;
        }

        return -1;
    }

    public static int findInventory(Item item, int count) {
        for (int i = INVENTORY_END; i >= INVENTORY_START; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() != item) continue;
            if (mc.player.getInventory().getStack(i).getCount() < count) continue;

            return i;
        }

        return -1;
    }

    public static int findHardestBlock(int start, int end) {
        float bestHardness = -1;
        int bestSlot = -1;

        for (int i = start; i <= end; i++) {
            if (!(mc.player.getInventory().getStack(i).getItem() instanceof BlockItem item)) continue;

            float hardness = item.getBlock().getHardness();
            if (hardness == -1) return i;
            if (hardness > bestHardness) {
                bestHardness = hardness;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static int findFastestItem(BlockState blockState, int start, int end) {
        double bestScore = -1;
        int bestSlot = -1;

        for (int i = start; i <= end; i++) {
            double score = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static int findBestSword(int start, int end) {
        int netheriteSlot = -1;
        int diamondSlot = -1;
        int ironSlot = -1;
        int goldenSlot = -1;
        int stoneSlot = -1;
        int woodenSlot = -1;

        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() == Items.NETHERITE_SWORD) netheriteSlot = i;
            if (stack.getItem() == Items.DIAMOND_SWORD) diamondSlot = i;
            if (stack.getItem() == Items.IRON_SWORD) ironSlot = i;
            if (stack.getItem() == Items.GOLDEN_SWORD) goldenSlot = i;
            if (stack.getItem() == Items.STONE_SWORD) stoneSlot = i;
            if (stack.getItem() == Items.WOODEN_SWORD) woodenSlot = i;
        }

        if (netheriteSlot != -1) return netheriteSlot;
        if (diamondSlot != -1) return diamondSlot;
        if (ironSlot != -1) return ironSlot;
        if (goldenSlot != -1) return goldenSlot;
        if (stoneSlot != -1) return stoneSlot;

        return woodenSlot;
    }

    public static int findEmptySlot(int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) continue;

            return i;
        }

        return -1;
    }

    public static boolean inInventoryScreen() {
        return mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen || mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof ShulkerBoxScreen;
    }
}
