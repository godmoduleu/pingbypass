package eu.client.modules.impl.visuals;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.EntitySpawnEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.animations.Animation;
import eu.client.utils.animations.Easing;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@RegisterModule(name = "Icons", description = "Renders icons for specific events.", category = Module.Category.VISUALS)
public class IconsModule extends Module {
    public CategorySetting chorusCategory = new CategorySetting("Chorus", "The category for chorus icons.");
    public BooleanSetting chorus = new BooleanSetting("Chorus", "Enabled", "Renders an icon for chorus positions.", new CategorySetting.Visibility(chorusCategory), true);
    public ColorSetting chorusColor = new ColorSetting("ChorusColor", "Color", "The color for the chorus icons.", new CategorySetting.Visibility(chorusCategory), new ColorSetting.Color(new Color(192, 147, 212), false, false));
    public NumberSetting duration = new NumberSetting("Duration", "The duration of icon renders.", new CategorySetting.Visibility(chorusCategory), 1500, 0, 5000);

    public CategorySetting pearlsCategory = new CategorySetting("Pearls", "The category for pearl icons.");
    public BooleanSetting pearls = new BooleanSetting("Pearls", "Enabled", "Renders an icon for pearl positions.", new CategorySetting.Visibility(pearlsCategory), true);
    public ColorSetting pearlsColor = new ColorSetting("PearlsColor", "Color", "The color for the pearl icons.", new CategorySetting.Visibility(pearlsCategory), new ColorSetting.Color(new Color(30, 131, 89), false, false));

    public NumberSetting scale = new NumberSetting("Scale", "The scaling that will be applied to the text ESP rendering.", 40, 10, 50);

    private final ArrayList<Icon> iconList = new ArrayList<>();
    private final Map<Icon, Integer> pearlMap = new HashMap<>();

    @SubscribeEvent
    public void onEntitySpawn(EntitySpawnEvent event) {
        if(getNull()) return;

        synchronized (iconList) {
            if (event.getEntity() instanceof EnderPearlEntity pearl) {
                mc.world.getPlayers().stream().min(Comparator.comparingDouble(p -> p.distanceTo(pearl))).ifPresent(player -> {
                    Vec3d landing = projectPearl(pearl.getPos(), player.getYaw(), player.getPitch(), player.getVelocity());
                    if (landing != null) {
                        Icon icon = new Icon(new Vec3d(landing.x, landing.y, landing.z), Icon.Type.PEARL);
                        pearlMap.put(icon, pearl.getId());
                        iconList.add(icon);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if(getNull()) return;

        synchronized (iconList) {
            if (event.getPacket() instanceof PlaySoundS2CPacket packet) {
                SoundEvent sound = packet.getSound().value();

                if (sound == SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT || sound == SoundEvents.ENTITY_ENDERMAN_TELEPORT) {
                    iconList.add(new Icon(new Vec3d(packet.getX(), packet.getY(), packet.getZ()), Icon.Type.CHORUS));
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(getNull()) return;

        MatrixStack matrices = event.getMatrices();

        synchronized (iconList) {
            iconList.removeIf(icon -> (icon.type == Icon.Type.CHORUS && (System.currentTimeMillis() - icon.time >= duration.getValue().intValue() + 1200)) ||
                    (icon.type == Icon.Type.PEARL) && !(mc.world.getEntityById(pearlMap.get(icon)) instanceof EnderPearlEntity) && icon.animation.get(0) == 0);

            if (chorus.getValue()) {
                for (Icon icon : iconList.stream().filter(icon -> icon.type == Icon.Type.CHORUS).toList()) {
                    icon.render(matrices, System.currentTimeMillis() - icon.time >= 600 + duration.getValue().intValue() ? 0 : 1, () -> {
                        Renderer2D.renderTexture(matrices, -6f, -6.5f, 6f, 6.5f, Identifier.of(EUClient.MOD_ID, "textures/chorus.png"), Color.WHITE);
                    }, chorusColor.getColor());
                }
            }

            if (pearls.getValue()) {
                for (Icon icon : iconList.stream().filter(icon -> icon.type == Icon.Type.PEARL).toList()) {
                    icon.render(matrices, !(mc.world.getEntityById(pearlMap.get(icon)) instanceof EnderPearlEntity) ? 0 : 1, () -> {
                        Renderer2D.renderTexture(matrices, -6.5f, -6.5f, 6.5f, 6.5f, Identifier.of(EUClient.MOD_ID, "textures/pearl.png"), Color.WHITE);
                    }, pearlsColor.getColor());
                }
            }
        }
    }

    private Vec3d projectPearl(Vec3d vec3d, float yaw, float pitch, Vec3d velocity) {
        double x = vec3d.x;
        double y = vec3d.y;
        double z = vec3d.z;

        y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;

        float maxDistance = 0.4f;

        double motionX = -MathHelper.sin(yaw / 180.0f * 3.1415927f) * MathHelper.cos(pitch / 180.0f * 3.1415927f) * maxDistance;
        double motionY = -MathHelper.sin(pitch / 180.0f * 3.141593f) * maxDistance;
        double motionZ = MathHelper.cos(yaw / 180.0f * 3.1415927f) * MathHelper.cos(pitch / 180.0f * 3.1415927f) * maxDistance;

        float distance = MathHelper.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        float pow = 1.5f;
        motionX *= pow;
        motionY *= pow;
        motionZ *= pow;

        motionX += velocity.x;
        motionY += velocity.y;
        motionZ += velocity.z;

        Vec3d lastPosition;
        while(y > -65) {
            lastPosition = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;

            if (mc.world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }

            motionY -= 0.03f;

            Vec3d position = new Vec3d(x, y, z);

            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player || entity instanceof ArrowEntity || entity instanceof EnderPearlEntity) continue;
                if (entity.getBoundingBox().intersects(new Box(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1))) {
                    return entity.getBoundingBox().getCenter();
                }
            }

            HitResult possibleResult = mc.world.raycast(new RaycastContext(lastPosition, position, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (possibleResult != null && possibleResult.getType() != HitResult.Type.MISS) {
                return possibleResult.getPos();
            }
        }

        return null;
    }

    @Getter
    private class Icon {
        private final Vec3d pos;
        private final Animation animation;
        private final Type type;
        private final long time;

        public Icon(Vec3d pos, Type type) {
            this.pos = pos;
            this.animation = new Animation(0, 1, 600, Easing.Method.EASE_IN_OUT_ELASTIC);
            this.type = type;
            this.time = System.currentTimeMillis();
        }

        private void render(MatrixStack matrices, float target, Runnable runnable, Color color) {
            float progress = animation.get(target);

            float distance = (float) Math.sqrt(mc.getEntityRenderDispatcher().camera.getPos().squaredDistanceTo(pos.x, pos.y, pos.z));
            float scaling = 0.0018f + (scale.getValue().floatValue() / 10000.0f) * distance;
            if (distance <= 4.0) scaling = 0.0245f;

            Vec3d vec3d = new Vec3d(pos.x - mc.getEntityRenderDispatcher().camera.getPos().x, pos.y - mc.getEntityRenderDispatcher().camera.getPos().y, pos.z - mc.getEntityRenderDispatcher().camera.getPos().z);

            matrices.push();
            matrices.translate(vec3d.x, vec3d.y, vec3d.z);
            matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
            matrices.scale(scaling, -scaling, scaling);

            matrices.scale(progress, progress, 1);

            Renderer2D.renderCircle(matrices, 0, 0, 12f, new Color(0, 0, 0, 100));
            Renderer2D.renderCircle(matrices, 0, 0, 10f, color);
            runnable.run();

            matrices.pop();
        }

        public enum Type {
            CHORUS,
            PEARL
        }
    }
}
