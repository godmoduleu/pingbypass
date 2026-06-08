package eu.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.mixins.accessors.ItemRenderStateAccessor;
import eu.client.mixins.accessors.ItemRendererAccessor;
import eu.client.mixins.accessors.LayerRenderStateAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.miscellaneous.FakePlayerModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.EntityUtils;
import eu.client.utils.text.CustomFormatting;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name = "NameTags", description = "Replaces the default Minecraft NameTag with a more visible and customizable one.", category = Module.Category.VISUALS)
public class NameTagsModule extends Module {
    public BooleanSetting gameMode = new BooleanSetting("GameMode", "Renders the player's gamemode.", false);
    public BooleanSetting ping = new BooleanSetting("Ping", "Renders the player's latency to the server.", true);
    public BooleanSetting entityId = new BooleanSetting("EntityID", "Renders the player's entity ID.", false);
    public BooleanSetting health = new BooleanSetting("Health", "Renders the player's health and absorption.", true);
    public BooleanSetting totemPops = new BooleanSetting("TotemPops", "Renders the amount of totems that the player has popped.", true);
    public BooleanSetting antiBot = new BooleanSetting("AntiBot", "Prevents bots from having nametags rendered for them.", false);
    public BooleanSetting euCheck = new BooleanSetting("EUCheck", "Adds an indicator next to the name of other users..", true);

    public BooleanSetting items = new BooleanSetting("Items", "Renders the items that the player is wearing or holding.", true);
    public BooleanSetting enchantments = new BooleanSetting("Enchantments", "Renders the enchantments of the player's items.", false);
    public BooleanSetting durability = new BooleanSetting("Durability", "Renders the durability of the player's items.", true);
    public BooleanSetting itemName = new BooleanSetting("ItemName", "Renders the name of the item that the player is currently holding.", true);

    public NumberSetting scale = new NumberSetting("Scale", "The scaling that will be applied to the nametag rendering.", 30, 10, 100);
    public ModeSetting border = new ModeSetting("Border", "The border that will surround the text.", "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(border, "Fill", "Both"), new ColorSetting.Color(new Color(0, 0, 0, 100), false, false));
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(border, "Outline", "Both"), new ColorSetting.Color(new Color(0, 0, 0, 100), false, false));

    private final ItemRenderState itemRenderState = new ItemRenderState();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent.Post event) {
        MatrixStack matrices = event.getMatrices();
        VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

        for (PlayerEntity player : mc.world.getPlayers().stream().sorted(Comparator.comparing(p -> -mc.player.distanceTo(p))).toList()) {
            if (player == mc.player) continue;
            if (antiBot.getValue() && EntityUtils.isBot(player)) continue;
            if (!Renderer3D.isFrustumVisible(player.getBoundingBox())) continue;

            double x = MathHelper.lerp(event.getTickDelta(), player.lastRenderX, player.getX());
            double y = MathHelper.lerp(event.getTickDelta(), player.lastRenderY, player.getY()) + (player.isSneaking() ? 1.9f : 2.1f);
            double z = MathHelper.lerp(event.getTickDelta(), player.lastRenderZ, player.getZ());

            Vec3d vec3d = new Vec3d(x - mc.getEntityRenderDispatcher().camera.getPos().x, y - mc.getEntityRenderDispatcher().camera.getPos().y, z - mc.getEntityRenderDispatcher().camera.getPos().z);
            float distance = (float) Math.sqrt(mc.getEntityRenderDispatcher().camera.getPos().squaredDistanceTo(x, y, z));
            float scaling = 0.0018f + (scale.getValue().intValue() / 10000.0f) * distance;
            if (distance <= 8.0) scaling = 0.0245f;

            matrices.push();
            matrices.translate(vec3d.x, vec3d.y, vec3d.z);
            matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
            matrices.scale(scaling, -scaling, scaling);

            String text = player.getName().getString();
            if (gameMode.getValue()) text += " [" + EntityUtils.getGameModeName(EntityUtils.getGameMode(player)) + "]";
            if (ping.getValue()) text += " " + EntityUtils.getLatency(player) + "ms";
            if (entityId.getValue()) text += " " + player.getId();
            if (health.getValue()) text += " " + ColorUtils.getHealthColor(player.getHealth() + player.getAbsorptionAmount()) + new DecimalFormat("0.0").format(player.getHealth() + player.getAbsorptionAmount()) + Formatting.RESET;

            int pops = EUClient.WORLD_MANAGER.getPoppedTotems().getOrDefault(player.getUuid(), 0);
            if (totemPops.getValue() && pops > 0) text += " " + ColorUtils.getTotemColor(pops) + "-" + pops;

            int width = EUClient.FONT_MANAGER.getWidth(text);

            if (border.getValue().equalsIgnoreCase("Fill") || border.getValue().equalsIgnoreCase("Both")) Renderer2D.renderQuad(matrices, -width / 2.0f - 1, -EUClient.FONT_MANAGER.getHeight() - 1, width / 2.0f + 2, 0, fillColor.getColor());
            if (border.getValue().equalsIgnoreCase("Outline") || border.getValue().equalsIgnoreCase("Both")) Renderer2D.renderOutline(matrices, -width / 2.0f - 1, -EUClient.FONT_MANAGER.getHeight() - 1, width / 2.0f + 2, 0, outlineColor.getColor());

            EUClient.FONT_MANAGER.drawTextWithShadow(matrices, text, -width / 2, -EUClient.FONT_MANAGER.getHeight(), vertexConsumers, EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).getPlayer() == player ? new Color(225, 0, 70) : player.isSneaking() ? new Color(255, 170, 0) : EUClient.FRIEND_MANAGER.contains(player.getName().getString()) ? EUClient.FRIEND_MANAGER.getDefaultFriendColor() : Color.WHITE);

            boolean renderedDurability = false;
            boolean renderedItems = false;
            int maxEnchants = 0;

            if (enchantments.getValue()) {
                for (int i = 0; i < 6; i++) {
                    ItemStack stack = getItem(player, i);
                    ItemEnchantmentsComponent component = EnchantmentHelper.getEnchantments(stack);

                    if (!component.getEnchantments().isEmpty()) {
                        int height = (component.getEnchantments().size() * EUClient.FONT_MANAGER.getHeight() / 2) - 18;
                        if (height > 0 && (height + 1) > maxEnchants) maxEnchants = height + 1;
                    }
                }
            }

            for (int i = 0; i < 6; i++) {
                ItemStack stack = getItem(player, i);
                if (stack.isEmpty()) continue;

                renderedItems = true;

                int stackX = -(108 / 2) + (i * 18) + 1;
                int stackY = -EUClient.FONT_MANAGER.getHeight() - 1 - (items.getValue() ? 18 + maxEnchants : 1);

                if (items.getValue()) {
                    ((ItemRendererAccessor) mc.getItemRenderer()).getItemModelManager().update(itemRenderState, stack, ModelTransformationMode.GUI, false, mc.world, mc.player, 0);

                    matrices.push();
                    matrices.translate(stackX + 8, stackY + 8, 0);
                    matrices.scale(16.0F, -16.0F, -0.001f);

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    GL11.glDepthFunc(GL11.GL_ALWAYS);

                    for (int j = 0; j < ((ItemRenderStateAccessor) itemRenderState).getLayerCount(); j++) {
                        ItemRenderState.LayerRenderState layer = ((ItemRenderStateAccessor) itemRenderState).getLayers()[j];

                        matrices.push();
                        ((LayerRenderStateAccessor) layer).getModel().getTransformation().getTransformation(ModelTransformationMode.GUI).apply(false, matrices);
                        matrices.translate(-0.5F, -0.5F, -0.5F);

                        if (((LayerRenderStateAccessor) layer).getSpecialModelType() != null) {
                            ((LayerRenderStateAccessor) layer).getSpecialModelType().render(((LayerRenderStateAccessor) layer).getData(), ModelTransformationMode.GUI, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, ((LayerRenderStateAccessor) layer).getGlint() != ItemRenderState.Glint.NONE);
                        } else if (((LayerRenderStateAccessor) layer).getModel() != null) {
                            VertexConsumer vertexConsumer;
                            RenderLayer renderLayer = RenderLayer.getGuiTextured(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

                            if (((LayerRenderStateAccessor) layer).getGlint() == ItemRenderState.Glint.SPECIAL) {
                                MatrixStack.Entry entry = matrices.peek().copy();
                                MatrixUtil.scale(entry.getPositionMatrix(), 0.5F);

                                vertexConsumer = ItemRendererAccessor.invokeGetDynamicDisplayGlintConsumer(vertexConsumers, renderLayer, entry);
                            } else {
                                vertexConsumer = ItemRenderer.getItemGlintConsumer(vertexConsumers, renderLayer, true, ((LayerRenderStateAccessor) layer).getGlint() != ItemRenderState.Glint.NONE);
                            }

                            ItemRendererAccessor.inovkeRenderBakedItemModel(((LayerRenderStateAccessor) layer).getModel(), ((LayerRenderStateAccessor) layer).getTints(), LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, matrices, vertexConsumer);
                        }

                        matrices.pop();
                    }

                    vertexConsumers.draw();

                    GL11.glDepthFunc(GL11.GL_LEQUAL);
                    RenderSystem.disableBlend();

                    matrices.pop();

                    if (stack.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE)) {
                        matrices.push();
                        matrices.translate(stackX, stackY, 0);
                        matrices.scale(0.5f, 0.5f, 1);
                        EUClient.FONT_MANAGER.drawTextWithShadow(matrices, "God", 0, 0, vertexConsumers, new Color(255, 125, 255));
                        matrices.pop();
                    }

                    if (stack.getCount() != 1) {
                        String count = stack.getCount() + "";
                        matrices.push();
                        matrices.translate(stackX + 17 - EUClient.FONT_MANAGER.getWidth(count), stackY + 9, 0);
                        EUClient.FONT_MANAGER.drawTextWithShadow(matrices, count, 0, 0, vertexConsumers, Color.WHITE);
                        matrices.pop();
                    }
                }

                if (durability.getValue() && stack.isDamageable()) {
                    float green = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
                    float red = 1.0f - green;

                    matrices.push();
                    matrices.translate(stackX, stackY - EUClient.FONT_MANAGER.getHeight() / 2f - 1, 0);
                    matrices.scale(0.5f, 0.5f, 1);
                    EUClient.FONT_MANAGER.drawTextWithShadow(matrices, Math.round(((stack.getMaxDamage() - stack.getDamage()) * 100.0f) / stack.getMaxDamage()) + "%", 0, 0, vertexConsumers, new Color(red, green, 0));
                    matrices.pop();

                    renderedDurability = true;
                }

                if (items.getValue() && enchantments.getValue() && stack.hasEnchantments()) {
                    ItemEnchantmentsComponent component = EnchantmentHelper.getEnchantments(stack);
                    Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
                    for (RegistryEntry<Enchantment> enchantment : component.getEnchantments()) {
                        enchantments.put(enchantment, component.getLevel(enchantment));
                    }

                    int height = 0;
                    for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : Object2IntMaps.fastIterable(enchantments)) {
                        String str = getEnchantmentName(entry.getKey().getIdAsString(), entry.getIntValue());

                        matrices.push();
                        matrices.translate(stackX, stackY + height, 0);
                        matrices.scale(0.5f, 0.5f, 1);
                        EUClient.FONT_MANAGER.drawTextWithShadow(matrices, str, 0, 0, vertexConsumers, Color.WHITE);
                        matrices.pop();

                        height += EUClient.FONT_MANAGER.getHeight() / 2;
                    }
                }
            }

            if (itemName.getValue() && !player.getMainHandStack().isEmpty()) {
                String itemText = player.getMainHandStack().getName().getString();

                matrices.push();
                matrices.translate(-EUClient.FONT_MANAGER.getWidth(itemText) / 2f / 2f, -EUClient.FONT_MANAGER.getHeight() - 1 - EUClient.FONT_MANAGER.getHeight() / 2f - 1 - (renderedItems ? (items.getValue() ? 18 + maxEnchants : 1) + (durability.getValue() && renderedDurability ? EUClient.FONT_MANAGER.getHeight() / 2.0f + 1 : 0) : 0), 0);
                matrices.scale(0.5f, 0.5f, 1);
                EUClient.FONT_MANAGER.drawTextWithShadow(matrices, itemText, 0, 0, vertexConsumers, Color.WHITE);
                matrices.pop();
            }

            matrices.pop();
        }
    }

    private ItemStack getItem(PlayerEntity player, int index) {
        return switch (index) {
            case 0 -> player.getMainHandStack();
            case 1 -> player.getInventory().armor.get(3);
            case 2 -> player.getInventory().armor.get(2);
            case 3 -> player.getInventory().armor.get(1);
            case 4 -> player.getInventory().armor.get(0);
            case 5 -> player.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    private String getEnchantmentName(String id, int level) {
        id = id.replace("minecraft:", "");
        id = level > 1 ? id.substring(0, 2) : id.substring(0, 3);
        return id.substring(0, 1).toUpperCase() + id.substring(1) + " " + (level > 1 ? level : "");
    }

    private record ItemElement(ItemStack stack, List<String> enchantments) { }
}