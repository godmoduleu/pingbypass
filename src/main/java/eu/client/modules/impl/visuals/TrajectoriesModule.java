package eu.client.modules.impl.visuals;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.EntityUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "Trajectories", description = "Draws a predicted trajectory of where throwables will end up when you throw them.", category = Module.Category.VISUALS)
public class TrajectoriesModule extends Module {
    public ColorSetting lineColor = new ColorSetting("LineColor", "The color of the trajectory line.", ColorUtils.getDefaultOutlineColor());

    public CategorySetting entitiesCategory = new CategorySetting("Entities", "The rendering that will be applied to entities hit by the trajectory.");
    public ModeSetting entitiesMode = new ModeSetting("EntitiesMode", "Mode", "The rendering that will be applied to the target entity.", new CategorySetting.Visibility(entitiesCategory), "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting entitiesFillColor = new ColorSetting("EntitiesFillColor", "FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(entitiesMode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting entitiesOutlineColor = new ColorSetting("EntitiesOutlineColor", "OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(entitiesMode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    public CategorySetting blocksCategory = new CategorySetting("Blocks", "The rendering that will be applied to blocks hit by the trajectory.");
    public ModeSetting blocksMode = new ModeSetting("BlocksMode", "Mode", "The rendering that will be applied to the target block.", new CategorySetting.Visibility(blocksCategory), "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting blocksFillColor = new ColorSetting("BlocksFillColor", "FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(blocksMode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting blocksOutlineColor = new ColorSetting("BlocksOutlineColor", "OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(blocksMode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    private List<Entity> hitEntities = new ArrayList<>();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson()) return;

        Hand activeHand;

        if (mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getMainHandStack().getItem() instanceof CrossbowItem || EntityUtils.isThrowable(mc.player.getMainHandStack().getItem())) activeHand = Hand.MAIN_HAND;
        else if (mc.player.getOffHandStack().getItem() instanceof BowItem || mc.player.getOffHandStack().getItem() instanceof CrossbowItem || EntityUtils.isThrowable(mc.player.getOffHandStack().getItem())) activeHand = Hand.OFF_HAND;
        else return;

        boolean prevBobView = mc.options.getBobView().getValue();
        mc.options.getBobView().setValue(false);

        hitEntities.clear();

        if ((mc.player.getOffHandStack().getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT), mc.player.getOffHandStack()) != 0) || (mc.player.getMainHandStack().getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT), mc.player.getMainHandStack()) != 0)) {
            project(event.getMatrices(), activeHand == Hand.OFF_HAND ? mc.player.getOffHandStack().getItem() : mc.player.getMainHandStack().getItem(), mc.player.getYaw() - 10, event.getTickDelta());
            project(event.getMatrices(), activeHand == Hand.OFF_HAND ? mc.player.getOffHandStack().getItem() : mc.player.getMainHandStack().getItem(), mc.player.getYaw(), event.getTickDelta());
            project(event.getMatrices(), activeHand == Hand.OFF_HAND ? mc.player.getOffHandStack().getItem() : mc.player.getMainHandStack().getItem(), mc.player.getYaw() + 10, event.getTickDelta());
        } else {
            project(event.getMatrices(), activeHand == Hand.OFF_HAND ? mc.player.getOffHandStack().getItem() : mc.player.getMainHandStack().getItem(), mc.player.getYaw(), event.getTickDelta());
        }

        mc.options.getBobView().setValue(prevBobView);
    }

    private void project(MatrixStack matrices, Item item, float yaw, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY());
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());

        y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;

        if (item == mc.player.getMainHandStack().getItem()) {
            x = x - MathHelper.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z - MathHelper.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        } else {
            x = x + MathHelper.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z + MathHelper.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        }

        float maxDistance = item instanceof BowItem ? 1.0f : 0.4f;

        double motionX = -MathHelper.sin(yaw / 180.0f * 3.1415927f) * MathHelper.cos(mc.player.getPitch() / 180.0f * 3.1415927f) * maxDistance;
        double motionY = -MathHelper.sin((mc.player.getPitch() - getThrowPitch(item)) / 180.0f * 3.141593f) * maxDistance;
        double motionZ = MathHelper.cos(yaw / 180.0f * 3.1415927f) * MathHelper.cos(mc.player.getPitch() / 180.0f * 3.1415927f) * maxDistance;

        float power = mc.player.getItemUseTime() / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;
        if (power > 1.0f || power == 0) power = 1.0f;

        float distance = MathHelper.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        float pow = (item instanceof BowItem ? (power * 2.0f) : item instanceof CrossbowItem ? (2.2f) : 1.0f) * getThrowVelocity(item);
        motionX *= pow;
        motionY *= pow;
        motionZ *= pow;

        if (!mc.player.isOnGround()) motionY += mc.player.getVelocity().getY();

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        Vec3d lastPosition;
        boolean landed = false;
        HitResult result = null;
        Entity entity = null;

        while(!landed && y > -65) {
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

            if (item instanceof BowItem) motionY -= 0.05000000074505806;
            else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) motionY -= 0.05000000074505806;
            else motionY -= 0.03f;

            Vec3d position = new Vec3d(x, y, z);

            Entity hitEntity = getHitEntity(new Box(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1));
            HitResult possibleResult = mc.world.raycast(new RaycastContext(lastPosition, position, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

            if(hitEntity != null) {
                entity = hitEntity;
                landed = true;
            } else {
                if(possibleResult != null && possibleResult.getType() != HitResult.Type.MISS) {
                    result = possibleResult;
                    landed = true;
                }
            }

            Renderer3D.DEBUG_LINES.add(new Renderer3D.VertexCollection(new Renderer3D.Vertex(matrix4f, (float) (lastPosition.getX() - mc.gameRenderer.getCamera().getPos().getX()), (float) (lastPosition.getY() - mc.gameRenderer.getCamera().getPos().getY()), (float) (lastPosition.getZ() - mc.gameRenderer.getCamera().getPos().getZ()), lineColor.getColor().getRGB()),
                    new Renderer3D.Vertex(matrix4f, (float) (position.getX() - mc.gameRenderer.getCamera().getPos().getX()), (float) (position.getY() - mc.gameRenderer.getCamera().getPos().getY()), (float) (position.getZ() - mc.gameRenderer.getCamera().getPos().getZ()), lineColor.getColor().getRGB())));
        }

        if(result != null && result.getType() == HitResult.Type.BLOCK) {
            Box box = new Box(result.getPos().getX() - 0.15, result.getPos().getY() - 0.15, result.getPos().getZ() - 0.15, result.getPos().getX() + 0.15, result.getPos().getY() + 0.15, result.getPos().getZ() + 0.15);

            if (blocksMode.getValue().equalsIgnoreCase("Fill") || blocksMode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBox(matrices, box, blocksFillColor.getColor());
            if (blocksMode.getValue().equalsIgnoreCase("Outline") || blocksMode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBoxOutline(matrices, box, blocksOutlineColor.getColor());
        }

        if(entity != null && !hitEntities.contains(entity)) {
            if (entitiesMode.getValue().equalsIgnoreCase("Fill") || entitiesMode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBox(matrices, entity.getBoundingBox(), entitiesFillColor.getColor());
            if (entitiesMode.getValue().equalsIgnoreCase("Outline") || entitiesMode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBoxOutline(matrices, entity.getBoundingBox(), entitiesOutlineColor.getColor());
            hitEntities.add(entity);
        }
    }

    private Entity getHitEntity(Box box) {
        for(Entity entity : mc.world.getEntities()) {
            if(entity == mc.player || entity instanceof ArrowEntity) continue;
            if (entity.getBoundingBox().intersects(box)) return entity;
        }
        return null;
    }

    private float getThrowVelocity(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.59f;
        if (item instanceof TridentItem) return 2f;
        return 1.5f;
    }

    private int getThrowPitch(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof ExperienceBottleItem)
            return 20;
        return 0;
    }
}
