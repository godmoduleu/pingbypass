package eu.client.utils.minecraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class IdentifierUtils {
    public static Item getItem(String name) {
        try {
            Item item = getIdentifier(Registries.ITEM, name);
            if (item != null) return item;
        } catch (Exception ignored) {}
        return null;
    }

    public static Block getBlock(String name) {
        try {
            Block block = getIdentifier(Registries.BLOCK, name);
            if (block != null) return block;
        } catch (Exception ignored) {}
        return null;
    }

    public static <T> T getIdentifier(Registry<T> registry, String name) {
        name = name.trim();

        Identifier identifier;
        if (name.contains(":")) {
            identifier = Identifier.of(name);
        } else {
            identifier = Identifier.of("minecraft", name);
        }

        if (registry.containsId(identifier)) return registry.get(identifier);
        return null;
    }
}
