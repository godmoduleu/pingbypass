package eu.client.modules.impl.visuals;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;

@RegisterModule(name = "TextESP", description = "Renders text ESP on the world.", category = Module.Category.VISUALS)
public class TextESPModule extends Module {
    public BooleanSetting items = new BooleanSetting("Items", "Renders text ESP on item entities.", true);
    public BooleanSetting pearls = new BooleanSetting("Pearls", "Renders text ESP on pearl entities.", true);
    public BooleanSetting chorus = new BooleanSetting("Chorus", "Renders text ESP on chorus sounds.", true);

    public NumberSetting scale = new NumberSetting("Scale", "The scaling that will be applied to the text ESP rendering.", 30, 10, 100);
    public ColorSetting color = new ColorSetting("Color", "The color that will be used for the text ESP rendering.", new ColorSetting.Color(Color.WHITE, false, false));

    private final ArrayList<Chorus> chorusList = new ArrayList<>();

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if(getNull()) return;

        synchronized (chorusList) {
            if (event.getPacket() instanceof PlaySoundS2CPacket packet) {
                SoundEvent sound = packet.getSound().value();

                if (sound == SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT || sound == SoundEvents.ENTITY_ENDERMAN_TELEPORT) {
                    chorusList.add(new Chorus(mc.getSoundManager().get(sound.id()).getSubtitle().getString(), new Vec3d(packet.getX(), packet.getY(), packet.getZ())));
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(getNull()) return;

        for(Entity e : mc.world.getEntities()) {
            if(!Renderer3D.isFrustumVisible(e.getBoundingBox())) continue;

            if(e instanceof ItemEntity item && items.getValue()) {
                Vec3d pos = EntityUtils.getRenderPos(item, event.getTickDelta());
                String s = item.getName().getString() + (item.getStack().getCount() > 1 ? " x" + item.getStack().getCount() : "");
                Renderer3D.renderScaledText(event.getMatrices(), s, pos.x, pos.y, pos.z, scale.getValue().intValue(), false, color.getColor());
            } else if(e instanceof EnderPearlEntity pearl && pearl.getOwner() != null && pearls.getValue()) {
                Vec3d pos = EntityUtils.getRenderPos(pearl, event.getTickDelta());
                Renderer3D.renderScaledText(event.getMatrices(), pearl.getOwner().getName().getString(), pos.x, pos.y, pos.z, scale.getValue().intValue(), false, color.getColor());
            }
        }

        if(chorus.getValue()) {
            synchronized (chorusList) {
                chorusList.removeIf(c -> System.currentTimeMillis() - c.time >= 1500);

                for (Chorus c : chorusList) {
                    Renderer3D.renderScaledText(event.getMatrices(), c.subtitle, c.pos.x, c.pos.y, c.pos.z, scale.getValue().intValue(), false, color.getColor());
                }
            }
        }
    }


    private class Chorus {
        private final String subtitle;
        private final Vec3d pos;
        private final long time;

        public Chorus(String subtitle, Vec3d pos) {
            this.subtitle = subtitle;
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }
}
