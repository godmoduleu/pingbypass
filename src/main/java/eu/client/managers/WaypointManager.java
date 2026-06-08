package eu.client.managers;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.utils.IMinecraft;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

@Getter
public class WaypointManager implements IMinecraft {
    private final ArrayList<Waypoint> waypoints = new ArrayList<>();

    public boolean contains(String name) {
        return waypoints.stream().anyMatch(w -> w.getName().equalsIgnoreCase(name));
    }

    public void add(String name, Vec3d pos) {
        add(name, pos, WorldUtils.getDimension(), EUClient.SERVER_MANAGER.getServer());
    }

    public void add(String name, Vec3d pos, String dimension, String server) {
        if (contains(name)) return;
        waypoints.add(new Waypoint(name, pos, dimension, server));
    }

    public void remove(String name) {
        waypoints.removeIf(w -> w.getName().equalsIgnoreCase(name));
    }

    public void clear() {
        waypoints.clear();
    }

    @Getter
    public static class Waypoint {
        private final String name;
        private final Vec3d pos;
        private final String dimension;
        private final String server;

        public Waypoint(String name, Vec3d pos, String dimension, String server) {
            this.name = name;
            this.pos = pos;
            this.dimension = dimension;
            this.server = server;
        }
    }
}
