package eu.client.modules.impl.miscellaneous;

import com.google.common.collect.Sets;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Set;

@RegisterModule(name = "NoSoundLag", description = "Prevents lagging caused by a large amount of sounds being played.", category = Module.Category.MISCELLANEOUS)
public class NoSoundLagModule extends Module {
    public BooleanSetting armor = new BooleanSetting("Armor", "Prevents lagging caused by armor sounds.", true);
    public BooleanSetting withers = new BooleanSetting("Withers", "Prevents lagging caused by wither sounds.", true);
    public BooleanSetting ghasts = new BooleanSetting("Ghasts", "Prevents lagging caused by ghast sounds.", true);

    public static final Set<RegistryEntry<SoundEvent>> ARMOR_SOUNDS = Sets.newHashSet(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final Set<SoundEvent> WITHER_SOUNDS = Sets.newHashSet(SoundEvents.ENTITY_WITHER_AMBIENT, SoundEvents.ENTITY_WITHER_DEATH, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundEvents.ENTITY_WITHER_HURT, SoundEvents.ENTITY_WITHER_SPAWN, SoundEvents.ENTITY_WITHER_SHOOT);
    public Set<SoundEvent> GHAST_SOUNDS = Sets.newHashSet(SoundEvents.ENTITY_GHAST_AMBIENT, SoundEvents.ENTITY_GHAST_DEATH, SoundEvents.ENTITY_GHAST_HURT, SoundEvents.ENTITY_GHAST_SCREAM, SoundEvents.ENTITY_GHAST_SHOOT, SoundEvents.ENTITY_GHAST_WARN);

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PlaySoundS2CPacket packet) {
            if ((armor.getValue() && ARMOR_SOUNDS.contains(packet.getSound())) || (withers.getValue() && WITHER_SOUNDS.contains(packet.getSound().value())) || (ghasts.getValue() && GHAST_SOUNDS.contains(packet.getSound().value()))) {
                event.setCancelled(true);
            }
        }
    }
}
