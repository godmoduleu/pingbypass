package eu.client.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.client.EUClient;
import eu.client.events.impl.RenderHandEvent;
import eu.client.modules.impl.player.SwingModule;
import eu.client.modules.impl.visuals.HandProgressModule;
import eu.client.modules.impl.visuals.ViewModelModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = HeldItemRenderer.class, priority = 999)
public abstract class HeldItemRendererMixin {
    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm);

    @Shadow
    private ItemStack offHand;

    @Shadow
    protected abstract void renderMapInBothHands(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);

    @Shadow
    protected abstract void renderMapInOneHand(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);

    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void renderFirstPersonItem$HEAD(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        ViewModelModule module = EUClient.MODULE_MANAGER.getModule(ViewModelModule.class);
        if (!module.isToggled()) return;

        info.cancel();

        if (!player.isUsingSpyglass()) {
            boolean bl = Hand.MAIN_HAND == hand;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();

            matrices.push();

            matrices.translate(module.translateX.getValue().floatValue(), module.translateY.getValue().floatValue(), module.translateZ.getValue().floatValue());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(module.rotateY.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(module.rotateX.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(module.rotateZ.getValue().floatValue()));
            matrices.scale(1 - (1 - module.scaleX.getValue().floatValue()), 1 - (1 - module.scaleY.getValue().floatValue()), 1 - (1 - module.scaleZ.getValue().floatValue()));

            if (item.isEmpty()) {
                if (bl && !player.isInvisible()) {
                    this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                }
            } else if (item.isOf(Items.FILLED_MAP)) {
                if (bl && this.offHand.isEmpty()) {
                    this.renderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                } else {
                    this.renderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                }
            } else {
                boolean bl4;
                float v;
                float w;
                float x;
                float y;
                if (item.isOf(Items.CROSSBOW)) {
                    bl4 = CrossbowItem.isCharged(item);
                    boolean bl3 = Arm.RIGHT == arm;
                    int i = bl3 ? 1 : -1;
                    if (player.isUsingItem() && 0 < player.getItemUseTimeLeft() && player.getActiveHand() == hand) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        matrices.translate((float)i * -0.4785682F, -0.0943870022892952D, 0.05731530860066414D);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -9.785F));
                        assert this.client.player != null;
                        LivingEntity playerEntity = this.client.player;
                        v = item.getMaxUseTime(playerEntity) - (Objects.requireNonNull(playerEntity).getItemUseTimeLeft() - tickDelta + 1.0F);
                        w = v / CrossbowItem.getPullTime(item, playerEntity);
                        if (1.0F < w) {
                            w = 1.0F;
                        }

                        if (0.1F < w) {
                            x = MathHelper.sin((v - 0.1F) * 1.3F);
                            y = w - 0.1F;
                            float k = x * y;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.translate(w * 0.0F, w * 0.0F, w * 0.04F);
                        matrices.scale(1.0F, 1.0F, 1.0F + w * 0.2F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
                    } else {
                        v = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                        w = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855F);
                        x = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
                        matrices.translate(i * v, w, x);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                        if (bl4 && 0.001F > swingProgress && bl) {
                            matrices.translate((float) i * -0.641864F, 0.0D, 0.0D);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
                        }
                    }

                    this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
                } else {
                    bl4 = Arm.RIGHT == arm;
                    int o;
                    float u;
                    if (player.isUsingItem() && 0 < player.getItemUseTimeLeft() && player.getActiveHand() == hand) {
                        o = bl4 ? 1 : -1;
                        switch (item.getUseAction()) {
                            case NONE, BLOCK -> this.applyEquipOffset(matrices, arm, equipProgress);
                            case EAT, DRINK -> {
                                this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item, player);
                                this.applyEquipOffset(matrices, arm, equipProgress);
                            }
                            case BOW -> {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float) o * -0.2785682F, 0.18344387412071228D, 0.15731531381607056D);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                                u = getU(tickDelta, item, matrices, o, this.client);
                                v = u / 20.0F;
                                v = (v * v + v * 2.0F) / 3.0F;
                                v = getV(matrices, v, u);
                                matrices.translate(v * 0.0F, v * 0.0F, v * 0.04F);
                                matrices.scale(1.0F, 1.0F, 1.0F + v * 0.2F);
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) o * 45.0F));
                            }
                            case SPEAR -> {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float) o * -0.5F, 0.699999988079071D, 0.10000000149011612D);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                                u = getU(tickDelta, item, matrices, o, this.client);
                                v = u / 10.0F;
                                v = getV(matrices, v, u);
                                matrices.translate(0.0D, 0.0D, v * 0.2F);
                                matrices.scale(1.0F, 1.0F, 1.0F + v * 0.2F);
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) o * 45.0F));
                            }
                            default -> {
                            }
                        }
                    } else if (player.isUsingRiptide()) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        o = bl4 ? 1 : -1;
                        matrices.translate((float) o * -0.4F, 0.800000011920929D, 0.30000001192092896D);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)o * 65.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)o * -85.0F));
                    } else {
                        float aa = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                        u = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855F);
                        v = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
                        int ad = bl4 ? 1 : -1;

                        float translateX = !EUClient.MODULE_MANAGER.getModule(SwingModule.class).isToggled() || EUClient.MODULE_MANAGER.getModule(SwingModule.class).translateX.getValue() ? ad * aa : 0;
                        float translateY = !EUClient.MODULE_MANAGER.getModule(SwingModule.class).isToggled() || EUClient.MODULE_MANAGER.getModule(SwingModule.class).translateY.getValue() ? u : 0;
                        float translateZ = !EUClient.MODULE_MANAGER.getModule(SwingModule.class).isToggled() || EUClient.MODULE_MANAGER.getModule(SwingModule.class).translateZ.getValue() ? v : 0;

                        matrices.translate(translateX, translateY, translateZ);

                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    }

                    this.renderItem(player, item, bl4 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl4, matrices, vertexConsumers, light);
                }
            }

            matrices.pop();
        }
    }

    @Unique
    private boolean isEating(PlayerEntity player, ItemStack item, Hand hand) {
        return player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand && (item.getUseAction().equals(UseAction.EAT) || item.getUseAction().equals(UseAction.DRINK));
    }

    @Unique
    private static float getV(MatrixStack matrices, float v, float u) {
        float v1 = v;
        float w;
        float x;
        float y;
        if (1.0F < v1) {
            v1 = 1.0F;
        }
        if (0.1F < v1) {
            w = MathHelper.sin((u - 0.1F) * 1.3F);
            x = v1 - 0.1F;
            y = w * x;
            matrices.translate(y * 0.0F, y * 0.004F, y * 0.0F);
        }
        return v1;
    }

    @Unique
    private static float getU(float tickDelta, @NotNull ItemStack item, @NotNull MatrixStack matrices, float o, @NotNull MinecraftClient client) {
        float u;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(o * 35.3F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(o * -9.785F));
        assert null != client.player;
        LivingEntity playerEntity = client.player;
        u = (float) item.getMaxUseTime(playerEntity) - ((float) playerEntity.getItemUseTimeLeft() - tickDelta + 1.0F);
        return u;
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 2))
    private float renderItem$lerpMainhand(float delta, float start, float end, Operation<Float> original) {
        if (EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).modifyMainhand.getValue()) {
            float progress = EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).mainhandProgress.getValue().floatValue();
            return MathHelper.lerp(delta, progress, progress);
        }

        return MathHelper.lerp(delta, start, end);
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 3))
    private float renderItem$lerpOffhand(float delta, float start, float end, Operation<Float> original) {
        if (EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).modifyOffhand.getValue()) {
            float progress = EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).offhandProgress.getValue().floatValue();
            return MathHelper.lerp(delta, progress, progress);
        }

        return MathHelper.lerp(delta, start, end);
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void renderItem$renderFirstPersonItem(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Operation<Void> original) {
        RenderHandEvent event = new RenderHandEvent(vertexConsumers);
        EUClient.EVENT_HANDLER.post(event);

        original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, event.getVertexConsumers(), light);
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
    private void renderItem$TAIL(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo info) {
        EUClient.EVENT_HANDLER.post(new RenderHandEvent.Post());
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo info) {
        if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).isToggled()) {
            info.cancel();

            int i = arm == Arm.RIGHT ? 1 : -1;
            float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
            float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);

            if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).rotationY.getValue()) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
            if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).rotationZ.getValue()) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
            if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).rotationX.getValue()) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
            if (EUClient.MODULE_MANAGER.getModule(SwingModule.class).rotationY.getValue()) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
        }
    }

    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        if(EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(HandProgressModule.class).staticEating.getValue()) {
            applyEatOrDrinkTransformation(matrices, tickDelta, arm, stack, player);
            ci.cancel();
        }
    }

    @Unique
    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player) {
        HandProgressModule module = EUClient.MODULE_MANAGER.getModule(HandProgressModule.class);
        float f = (float)player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float)stack.getMaxUseTime(player);
        float h;

        if (module.isToggled() && module.staticEating.getValue()) {
            h = 1.0f;
        } else {
            if (g < 0.8F) {
                h = MathHelper.abs(MathHelper.cos(f / 4.0F * 3.1415927F) * 0.1F);
                matrices.translate(0.0F, h, 0.0F);
            }

            h = 1.0F - (float) Math.pow(g, 27.0);
        }

        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * (float)i, h * -0.5F, h * 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * h * 30.0F));
    }
}