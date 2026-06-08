package eu.client.modules.impl.visuals;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.managers.WaypointManager;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.StaticPlayerEntity;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@RegisterModule(name = "Waypoints", description = "Renders waypoints on different locations.", category = Module.Category.VISUALS)
public class WaypointsModule extends Module {
    public BooleanSetting logoutSpots = new BooleanSetting("LogoutSpots", "Renders waypoints where players have logged out.", true);
    public BooleanSetting deaths = new BooleanSetting("Deaths", "Renders a waypoint in your last death position.", true);
    public BooleanSetting rally = new BooleanSetting("Rally", "Renders a waypoint in the position of the current IRC rally.", true);
    public ModeSetting textMode = new ModeSetting("TextMode", "The mode for the waypoints text render.", "Coordinates", new String[]{"Coordinates", "Distance", "None"});
    public ColorSetting textColor = new ColorSetting("TextColor", "The color for the waypoints text render.", new ColorSetting.Color(Color.WHITE, false ,false));
    public NumberSetting scale = new NumberSetting("Scale", "The scaling that will be applied to the text rendering.", 30, 10, 100);
    public BooleanSetting notification = new BooleanSetting("Notification", "Sends a message to notify when a player logs in or logs out of the server.", true);
    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to the waypoint chams.", "Both", new String[]{"Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color used for the fill rendering.", new ModeSetting.Visibility(mode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public  ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color used for the outline rendering.", new ModeSetting.Visibility(mode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    @Getter private final ArrayList<Waypoint> logoutPoints = new ArrayList<>();
    private Waypoint deathPoint;

    private WaypointManager.Waypoint rallyPoint;
    @Setter private long rallyTime;

    @Override
    public void onDisable() {
        deathPoint = null;
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(event.getPlayer() == mc.player) {
            deathPoint = new Waypoint(event.getPlayer());
        }
    }

    @SubscribeEvent
    public void onPlayerConnect(PlayerConnectEvent event) {
        if(getNull()) return;

        synchronized (logoutPoints) {
            Waypoint waypoint = logoutPoints.stream().filter(w -> w.id.equals(event.getId())).findFirst().orElse(null);

            if(waypoint != null) {
                logoutPoints.remove(waypoint);
                if(notification.getValue()) EUClient.CHAT_MANAGER.message(ChatUtils.getPrimary() + waypoint.name + ChatUtils.getSecondary() + " has logged in at " + ChatUtils.getPrimary() + "[" + (int) waypoint.pos.x + ", " + (int) waypoint.pos.y + ", " + (int) waypoint.pos.z  + "]" + ChatUtils.getSecondary() + ".", "waypoints-" + waypoint.name);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if(getNull()) return;

        synchronized (logoutPoints) {
            PlayerEntity player = mc.world.getPlayerByUuid(event.getId());
            if (player != null) {
                Waypoint waypoint = new Waypoint(player);
                logoutPoints.add(waypoint);
                if(notification.getValue()) EUClient.CHAT_MANAGER.message(ChatUtils.getPrimary() + waypoint.name + ChatUtils.getSecondary() + " has logged out at " + ChatUtils.getPrimary() + "[" + (int) waypoint.pos.x + ", " + (int) waypoint.pos.y + ", " + (int) waypoint.pos.z  + "]" + ChatUtils.getSecondary() + ".", "waypoints-" + waypoint.name);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) return;


        if (logoutSpots.getValue() && !logoutPoints.isEmpty()) {
            synchronized (logoutPoints) {
                for (Waypoint waypoint : logoutPoints.stream().filter(this::sameWorld).toList()) {
                    if (!Renderer3D.isFrustumVisible(waypoint.model.getBoundingBox())) continue;

                    waypoint.model.render(event, mode.getValue().equals("Fill") || mode.getValue().equals("Both"), fillColor.getColor(), mode.getValue().equals("Outline") || mode.getValue().equals("Both"), outlineColor.getColor());
                    Renderer3D.renderScaledText(event.getMatrices(), waypoint.name + getSuffix(waypoint), waypoint.pos.x, waypoint.pos.y + 2f, waypoint.pos.z, scale.getValue().intValue(), false, textColor.getColor());
                }
            }
        }

        if (deaths.getValue() && sameWorld(deathPoint) && Renderer3D.isFrustumVisible(deathPoint.model.getBoundingBox())) {
            deathPoint.model.render(event, mode.getValue().equals("Fill") || mode.getValue().equals("Both"), fillColor.getColor(), mode.getValue().equals("Outline") || mode.getValue().equals("Both"), outlineColor.getColor());
            Renderer3D.renderScaledText(event.getMatrices(), "Death" + getSuffix(deathPoint), deathPoint.pos.x, deathPoint.pos.y + 2f, deathPoint.pos.z, scale.getValue().intValue(), false, textColor.getColor());
        }

        if (rally.getValue() && rallyPoint != null && System.currentTimeMillis() - rallyTime <= 120000) {
            if(rallyPoint.getDimension().equals(WorldUtils.getDimension()) && rallyPoint.getServer().equals(EUClient.SERVER_MANAGER.getServer())) {
                Renderer3D.renderScaledText(event.getMatrices(), rallyPoint.getName() + getSuffix(rallyPoint.getPos()), rallyPoint.getPos().x, rallyPoint.getPos().y, rallyPoint.getPos().z, scale.getValue().intValue(), true, textColor.getColor());
            }
        }

        if(!EUClient.WAYPOINT_MANAGER.getWaypoints().isEmpty()) {
            for(WaypointManager.Waypoint waypoint : EUClient.WAYPOINT_MANAGER.getWaypoints().stream().filter(w -> w.getDimension().equals(WorldUtils.getDimension()) && w.getServer().equals(EUClient.SERVER_MANAGER.getServer())).toList()) {
                Renderer3D.renderScaledText(event.getMatrices(), waypoint.getName() + getSuffix(waypoint.getPos()), waypoint.getPos().x, waypoint.getPos().y, waypoint.getPos().z, scale.getValue().intValue(), true, textColor.getColor());
            }
        }
    }

    public void setRallyPoint(String[] args) {
        Vec3d vec3d = new Vec3d(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        rallyPoint =  new WaypointManager.Waypoint("Rally", vec3d, args[3], args[4]);
    }

    private String getSuffix(Waypoint waypoint) {
        if(textMode.getValue().equals("Coordinates")) {
            return " (" + (int) waypoint.pos.x + ", " + (int) waypoint.pos.z + ")";
        } else if(textMode.getValue().equals("Distance")) {
            return " (" + (int) Math.sqrt(mc.player.squaredDistanceTo(waypoint.pos)) + "m)";
        }
        return "";
    }

    private String getSuffix(Vec3d pos) {
        if(textMode.getValue().equals("Coordinates")) {
            return " (" + (int) pos.x + ", " + (int) pos.z + ")";
        } else if(textMode.getValue().equals("Distance")) {
            return " (" + (int) Math.sqrt(mc.player.squaredDistanceTo(pos)) + "m)";
        }
        return "";
    }

    private boolean sameWorld(Waypoint waypoint) {
        if(waypoint == null) return false;
        return WorldUtils.getDimension().equals(waypoint.dimension) && EUClient.SERVER_MANAGER.getServer().equals(waypoint.server);
    }

    private class Waypoint {
        public final String name;
        public final UUID id;
        public final Vec3d pos;
        public final String dimension;
        public final String server;
        public final StaticPlayerEntity model;

        public Waypoint(PlayerEntity player) {
            this.name = player.getName().getString();
            this.id = player.getUuid();
            this.pos = player.getPos();
            this.dimension = WorldUtils.getDimension();
            this.server = EUClient.SERVER_MANAGER.getServer();
            this.model = new StaticPlayerEntity(player);
        }
    }
}
