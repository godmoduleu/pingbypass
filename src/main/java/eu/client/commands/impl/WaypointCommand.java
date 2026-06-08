package eu.client.commands.impl;

import eu.client.EUClient;
import eu.client.commands.Command;
import eu.client.commands.RegisterCommand;
import eu.client.managers.WaypointManager;
import eu.client.utils.chat.ChatUtils;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

@RegisterCommand(name = "waypoint", tag = "Waypoint", description = "Allows you to manage the client's custom waypoints.", syntax = "<add|del> <[x, y, z]|[x, z]> | <clear|list>", aliases = {"w"})
public class WaypointCommand extends Command {

    @Override
    public void execute(String[] args) {
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("clear")) {
                EUClient.WAYPOINT_MANAGER.clear();
                EUClient.CHAT_MANAGER.tagged("Successfully cleared your custom waypoints.", getTag(), getName() + "-list");
            } else if(args[0].equalsIgnoreCase("list")) {
                ArrayList<WaypointManager.Waypoint> waypoints = EUClient.WAYPOINT_MANAGER.getWaypoints();
                if(waypoints.isEmpty()) {
                    EUClient.CHAT_MANAGER.tagged("You currently have no custom waypoints set.", getTag());
                } else {
                    StringBuilder builder = new StringBuilder();
                    int index = 0;

                    for(WaypointManager.Waypoint waypoint : waypoints) {
                        index++;
                        builder.append(ChatUtils.getSecondary()).append(waypoint.getName())
                                .append(index == waypoints.size() ? "" : ", ");
                    }

                    EUClient.CHAT_MANAGER.message("Custom waypoints " + ChatUtils.getPrimary() + "[" + ChatUtils.getSecondary() + waypoints.size() + ChatUtils.getPrimary() + "]: " + ChatUtils.getSecondary() + builder, getName() + "-list");
                }
            } else {
                messageSyntax();
            }
        }else if(args.length == 2 || args.length == 4 || args.length == 5) {
            int x, y, z;
            try {
                x = args.length == 2 ? (int) mc.player.getX() : Integer.parseInt(args[2]);
                y = args.length == 2 || args.length == 4 ? (int) mc.player.getY() + 1 : Integer.parseInt(args[3]);
                z = args.length == 2 ? (int) mc.player.getZ() : args.length == 4 ? Integer.parseInt(args[3]) : Integer.parseInt(args[4]);

                Vec3d vec3d = new Vec3d(x, y, z);

                if(args[0].equalsIgnoreCase("add")) {
                    EUClient.WAYPOINT_MANAGER.add(args[1], vec3d);
                    EUClient.CHAT_MANAGER.tagged("Successfully added " + ChatUtils.getPrimary() + args[1] + " [" + (int)vec3d.x + ", " + (int)vec3d.y + ", "   + (int)vec3d.z  + "]" + ChatUtils.getSecondary() + " to your custom waypoints.", getTag(), getName());
                } else if(args[0].equalsIgnoreCase("del")) {
                    EUClient.WAYPOINT_MANAGER.remove(args[1]);
                    EUClient.CHAT_MANAGER.tagged("Successfully removed " + ChatUtils.getPrimary() + args[1] + ChatUtils.getSecondary() + " to your custom waypoints.", getTag(), getName());
                } else {
                    messageSyntax();
                }
            } catch (NumberFormatException exception) {
                EUClient.CHAT_MANAGER.tagged("Please input valid " + ChatUtils.getPrimary() + "integer" + ChatUtils.getSecondary() + " numbers for the coordinates.", getTag(), getName());
            }
        } else {
            messageSyntax();
        }
    }
}
