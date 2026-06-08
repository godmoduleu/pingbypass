package eu.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import eu.client.settings.Setting;
import eu.client.utils.annotations.AllowedTypes;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter @Setter
public class WhitelistSetting extends Setting {
    private final Type type;
    private final Set<Object> whitelist = new HashSet<>();

    public WhitelistSetting(String name, String description, Type type) {
        super(name, name, description, new Setting.Visibility());
        this.type = type;
    }

    public WhitelistSetting(String name, String tag, String description, Type type) {
        super(name, tag, description, new Setting.Visibility());
        this.type = type;
    }

    public WhitelistSetting(String name, String description, Setting.Visibility visibility, Type type) {
        super(name, name, description, visibility);
        this.type = type;
    }

    public WhitelistSetting(String name, String tag, String description, Setting.Visibility visibility, Type type) {
        super(name, tag, description, visibility);
        this.type = type;
    }

    @AllowedTypes({Item.class, Block.class})
    public void add(Object object) {
        whitelist.add(object);
    }

    @AllowedTypes({Item.class, Block.class})
    public void remove(Object id) {
        whitelist.remove(id);
    }

    @AllowedTypes({Item.class, Block.class})
    public boolean isWhitelistContains(Object object) {
        return whitelist.contains(object);
    }

    public List<String> getWhitelistIds() {
        return whitelist.stream().map(object -> {
            if (object instanceof Item item) {
                return Registries.ITEM.getId(item).toString();
            } else if (object instanceof Block block) {
                return Registries.BLOCK.getId(block).toString();
            }
            return null;
        }).toList();
    }

    public void clear() {
        whitelist.clear();
    }

    public enum Type {
        ITEMS, BLOCKS
    }
}
