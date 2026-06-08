package eu.client.modules.impl.visuals;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RegisterModule(name = "ChildESP", description = "Identifies and highlights players who use brainrot terms in chat.", category = Module.Category.VISUALS)
public class ChildESP extends Module {
    private static final java.util.Set<String> BRAINROT_TERMS = new java.util.HashSet<>(java.util.Arrays.asList(
        "skibidi", "ohio", "sigma", "gyatt", "rizz", "fanum", "tax", "cap", "no cap",
        "bussin", "slay", "ate", "that's ate", "it's the", "ong", "bet", "lowkey",
        "fr fr", "deadass", "slaps", "based", "cringe", "kino", "mogul", "mogul mods",
        "beta", "alpha", "alpha male", "grindset", "tism", "autism", "autistic",
        "neurodivergent", "neurodiversity", "based god", "degen", "degenerate",
        "wojak", "virgin", "chad", "incel", "simp", "simping", "soyjak", "cuck"
    ));

    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to child entities.", "Outline", new String[]{"Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(mode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(mode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which children will be rendered.", 64, 16, 256);
    public BooleanSetting showNames = new BooleanSetting("ShowNames", "Display player names in chat when they use brainrot terms.", true);

    private final Map<String, Long> detectedChildren = new HashMap<>();

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            try {
                String messageText = packet.content().getString().toLowerCase();

                // Try to extract player name and check for brainrot terms
                String[] parts = messageText.split(":");
                if (parts.length >= 2) {
                    String playerName = parts[0].trim();
                    String chatContent = String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length)).toLowerCase();

                    if (isBrainrotMessage(chatContent)) {
                        detectedChildren.put(playerName, System.currentTimeMillis());

                        if (showNames.getValue()) {
                            EUClient.CHAT_MANAGER.tagged(playerName + " uses brainrot terms", getName());
                        }
                    }
                }
            } catch (Exception e) {
                // Silently fail if we can't parse the message
            }
        }
    }

    private boolean isBrainrotMessage(String message) {
        for (String term : BRAINROT_TERMS) {
            if (message.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onTick(TickEvent event) {
        // Clear expired entries (older than 5 minutes)
        long currentTime = System.currentTimeMillis();
        detectedChildren.entrySet().removeIf(entry -> currentTime - entry.getValue() > 300000);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (detectedChildren.isEmpty()) return;

        double rangeSquared = Math.pow(range.getValue().doubleValue(), 2);

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (entity == mc.player) continue;

            String playerName = player.getName().getString();
            if (!detectedChildren.containsKey(playerName)) continue;

            double distSquared = mc.player.squaredDistanceTo(player);
            if (distSquared > rangeSquared) continue;

            Vec3d pos = EntityUtils.getRenderPos(player, event.getTickDelta());
            Box box = new Box(
                pos.x - player.getBoundingBox().getLengthX() / 2,
                pos.y,
                pos.z - player.getBoundingBox().getLengthZ() / 2,
                pos.x + player.getBoundingBox().getLengthX() / 2,
                pos.y + player.getBoundingBox().getLengthY(),
                pos.z + player.getBoundingBox().getLengthZ() / 2
            );

            if (mode.getValue().equalsIgnoreCase("Fill") || mode.getValue().equalsIgnoreCase("Both")) {
                Renderer3D.renderBox(event.getMatrices(), box, fillColor.getColor());
            }
            if (mode.getValue().equalsIgnoreCase("Outline") || mode.getValue().equalsIgnoreCase("Both")) {
                Renderer3D.renderBoxOutline(event.getMatrices(), box, outlineColor.getColor());
            }
        }
    }
}
