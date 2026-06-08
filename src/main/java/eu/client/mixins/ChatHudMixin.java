package eu.client.mixins;

import eu.client.EUClient;
import eu.client.modules.impl.core.CommandsModule;
import eu.client.modules.impl.core.HUDModule;
import eu.client.modules.impl.miscellaneous.BetterChatModule;
import eu.client.utils.IMinecraft;
import eu.client.utils.animations.Animation;
import eu.client.utils.animations.Easing;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.mixins.IChatHudLineVisible;
import eu.client.utils.text.CustomFormatting;
import eu.client.utils.text.FormattingUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Mixin(value = ChatHud.class, priority = 999)
public abstract class ChatHudMixin<E> implements IMinecraft {
    @Shadow protected abstract boolean isChatHidden();

    @Shadow public abstract int getVisibleLineCount();

    @Shadow @Final public List<ChatHudLine.Visible> visibleMessages;

    @Shadow public abstract double getChatScale();

    @Shadow public abstract int getWidth();

    @Shadow protected abstract int getMessageIndex(double chatLineX, double chatLineY);

    @Shadow protected abstract double toChatLineX(double x);

    @Shadow protected abstract double toChatLineY(double y);

    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract int getLineHeight();

    @Shadow private int scrolledLines;

    @Shadow private static double getMessageOpacityMultiplier(int age) { return 0; }

    @Shadow protected abstract int getIndicatorX(ChatHudLine.Visible line);

    @Shadow protected abstract void drawIndicatorIcon(DrawContext context, int x, int y, MessageIndicator.Icon icon);

    @Shadow private boolean hasUnreadNewMessages;

    /**
     * @author p4nda
     * @reason BetterChatModule
     */
    @Overwrite
    public void render(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused) {
        BetterChatModule module = EUClient.MODULE_MANAGER.getModule(BetterChatModule.class);

        if (!this.isChatHidden()) {
            int i = this.getVisibleLineCount();
            int j = this.visibleMessages.size();
            if (j > 0) {
                Profilers.get().push("chat");
                float f = (float)this.getChatScale();
                int k = MathHelper.ceil((float)this.getWidth() / f);
                int l = context.getScaledWindowHeight();
                context.getMatrices().push();
                context.getMatrices().scale(f, f, 1.0F);
                context.getMatrices().translate(4.0F, 0.0F, 0.0F);
                int m = MathHelper.floor((float)(l - 40) / f);
                int n = this.getMessageIndex(this.toChatLineX(mouseX), this.toChatLineY(mouseY));
                double d = this.client.options.getChatOpacity().getValue() * 0.8999999761581421 + 0.10000000149011612;
                double e = this.client.options.getTextBackgroundOpacity().getValue();
                double g = this.client.options.getChatLineSpacing().getValue();
                int o = this.getLineHeight();
                int p = (int)Math.round(-8.0 * (g + 1.0) + 4.0 * g);
                int q = 0;

                int t;
                int u;
                int v;
                int x;
                for(int r = 0; r + this.scrolledLines < this.visibleMessages.size() && r < i; ++r) {

                    context.getMatrices().push();
                    context.getMatrices().translate(module.offset.getValue().intValue(), 0, 0);

                    int s = r + this.scrolledLines;
                    ChatHudLine.Visible visible = this.visibleMessages.get(s);
                    if (visible != null) {
                        t = currentTick - visible.addedTime();
                        if (t < 200 || focused) {
                            double h = focused ? 1.0 : getMessageOpacityMultiplier(t);
                            u = (int)(255.0 * h * (module.isToggled() ? module.textAlpha.getValue().intValue() / 255f : d));
                            v = (int)(255.0 * h * (module.isToggled() && !module.background.getValue().equals("Default") ? (module.background.getValue().equals("Clear") ? 0 : module.color.getAlpha() / 255f) : e));
                            ++q;
                            if (u > 3) {
                                x = m - r * o;
                                int y = x + p;
                                context.fill(-4, x - o, 0 + k + 4 + 4, x, module.isToggled() && module.background.getValue().equals("Custom") ? module.color.getColor().getRGB() : v << 24);
                                MessageIndicator messageIndicator = visible.indicator();
                                if ((!module.isToggled() || !module.noIndicators.getValue()) && messageIndicator != null) {
                                    int z = ((IChatHudLineVisible) (Object) visible).euclient$isClientMessage() ? ColorUtils.getGlobalColor().getRGB() : messageIndicator.indicatorColor() | u << 24;
                                    context.fill(-4, x - o, -2, x, z);
                                    if (s == n && messageIndicator.icon() != null) {
                                        int aa = this.getIndicatorX(visible);
                                        Objects.requireNonNull(this.client.textRenderer);
                                        int ab = y + 9;
                                        this.drawIndicatorIcon(context, aa, ab, messageIndicator.icon());
                                    }
                                }

                                context.getMatrices().push();
                                float scale = module.getAnimationMap().containsKey(visible) ? 1.0f - Easing.toDelta(module.getAnimationMap().get(visible), module.delay.getValue().intValue()) : 0;
                                context.getMatrices().translate(-mc.textRenderer.getWidth(visible.content()) * scale, 0.0F, 50.0F);
                                context.drawTextWithShadow(this.client.textRenderer, processOrderedText(visible.content()), 0, y, 16777215 + (u << 24));
                                context.getMatrices().pop();
                            }
                        }
                    }

                    context.getMatrices().pop();
                }

                if(module.isToggled() && module.animation.getValue()) module.getAnimationMap().entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > module.delay.getValue().intValue());

                long ac = this.client.getMessageHandler().getUnprocessedMessageCount();
                int ad;
                if (ac > 0L) {
                    ad = (int)(128.0 * d);
                    t = (int)(255.0 * e);
                    context.getMatrices().push();
                    context.getMatrices().translate(0.0F, (float)m, 0.0F);
                    context.fill(-2, 0, k + 4, 9, t << 24);
                    context.getMatrices().translate(0.0F, 0.0F, 50.0F);
                    context.drawTextWithShadow(this.client.textRenderer, Text.translatable("chat.queue", new Object[]{ac}), 0, 1, 16777215 + (ad << 24));
                    context.getMatrices().pop();
                }

                if (focused) {
                    ad = this.getLineHeight();
                    t = j * ad;
                    int ae = q * ad;
                    int af = this.scrolledLines * ae / j - m;
                    u = ae * ae / t;
                    if (t != ae) {
                        v = af > 0 ? 170 : 96;
                        int w = this.hasUnreadNewMessages ? 13382451 : 3355562;
                        x = k + 4;
                        context.fill(x, -af, x + 2, -af - u, 100, w + (v << 24));
                        context.fill(x + 2, -af, x + 1, -af - u, 100, 13421772 + (v << 24));
                    }
                }

                context.getMatrices().pop();
                Profilers.get().pop();
            }
        }
    }

    @Unique
    private OrderedText processOrderedText(OrderedText orderedText) {
        MutableText builder = Text.empty();

        int[] i = {0};
        orderedText.accept((index, style, codePoint) -> {
            MutableText text = Text.empty();

            if (style.getColor() != null && (style.getColor().toString().toLowerCase().equals(CustomFormatting.CLIENT.getName()) || style.getColor().toString().toLowerCase().equals(CustomFormatting.RAINBOW.getName()))) {
                if (style.getColor().toString().toLowerCase().equals(CustomFormatting.CLIENT.getName())) {
                    text.append(Text.literal(String.valueOf(Character.toChars(codePoint))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ColorUtils.getGlobalColor().getRGB()))));
                }

                if (style.getColor().toString().toLowerCase().equals(CustomFormatting.RAINBOW.getName())) {
                    long index1 = (long) i[0] * (EUClient.MODULE_MANAGER.getModule(HUDModule.class).rainbowOffset.getValue().longValue() * 5L);
                    Color color = ColorUtils.getOffsetRainbow(index1);
                    text.append(Text.literal(String.valueOf(Character.toChars(codePoint))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getRGB()))));
                }
            } else {
                text.append(Text.literal(String.valueOf(Character.toChars(codePoint))).setStyle(style));
            }

            builder.append(text);
            i[0]++;

            return true;
        });

        return builder.asOrderedText();
    }

    @Redirect(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
    private void addVisibleMessage(List<E> instance, int i, E e) {
        ChatHudLine.Visible visible = (ChatHudLine.Visible) e;
        if (EUClient.MODULE_MANAGER.getModule(BetterChatModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(BetterChatModule.class).animation.getValue()) EUClient.MODULE_MANAGER.getModule(BetterChatModule.class).getAnimationMap().put(visible, System.currentTimeMillis());
        instance.add(i, e);
    }

    @ModifyArgs(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine;<init>(ILnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"))
    private void addMessage(Args args, Text message, MessageSignatureData signature, MessageIndicator indicator) {
        BetterChatModule module = EUClient.MODULE_MANAGER.getModule(BetterChatModule.class);
        if (module.isToggled() && module.timestamps.getValue()) {
            args.set(1, Text.literal("").append(Text.literal(FormattingUtils.getFormatting(EUClient.MODULE_MANAGER.getModule(CommandsModule.class).secondaryWatermarkColor.getValue()) + module.opening.getValue() + FormattingUtils.getFormatting(EUClient.MODULE_MANAGER.getModule(CommandsModule.class).primaryWatermarkColor.getValue()) + new SimpleDateFormat("HH:mm").format(new Date()) + FormattingUtils.getFormatting(EUClient.MODULE_MANAGER.getModule(CommandsModule.class).secondaryWatermarkColor.getValue()) + module.closing.getValue() + Formatting.RESET + " ")).append(message));
        }
    }
}
