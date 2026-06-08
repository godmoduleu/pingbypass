package eu.client.modules.impl.miscellaneous;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.mixins.accessors.MouseAccessor;
import eu.client.mixins.accessors.Vec3dAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.movement.HitboxDesyncModule;
import eu.client.modules.impl.movement.HoleSnapModule;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.minecraft.MovementUtils;
import eu.client.utils.rotations.RotationUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//@RegisterModule(name = "EURobotics", description = "Very cool robots", category = Module.Category.MISCELLANEOUS)
public class EURoboticsModule extends Module {
    public ModeSetting side = new ModeSetting("Side", "The side that this Minecraft instance is on.", "Client", new String[]{"Client", "Server"});
    public StringSetting port = new StringSetting("Port", "The port that will be used for communication.", "4311");

    @Setter private String target = "";

    private final Client client = new Client();
    private final Server server = new Server();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("update;" + mc.player.getName().getString());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onToggleModule(ToggleModuleEvent event) {
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("module;" + event.getModule().getName() + ";" + event.getModule().isToggled());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onUnfilteredKeyInput(UnfilteredKeyInputEvent event) {
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("key;" + event.getKey() + ";" + event.getScancode() + ";" + event.getAction() + ";" + event.getModifiers());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onUnfilteredMouseInput(UnfilteredMouseInputEvent event) {
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("mouse;" + event.getButton() + ";" + event.getAction() + ";" + event.getMods());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onChangeYaw(ChangeYawEvent event) {
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("yaw;" + event.getYaw());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onChangePitch(ChangePitchEvent event) {
        if (side.getValue().equalsIgnoreCase("Server") || client.getPrimarySocket() == null || !client.getPrimarySocket().isConnected()) return;

        try {
            client.sendMessage("pitch;" + event.getPitch());
        } catch (IOException exception) {
            EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (getNull() || mc.player.fallDistance >= 5.0f) return;
        if (side.getValue().equalsIgnoreCase("Client")) return;

        PlayerEntity player = getPlayer();
        if (player == null) return;

        if (mc.player.squaredDistanceTo(player.getPos()) <= 0.25) return;
        if (mc.player.squaredDistanceTo(player.getPos()) <= MathHelper.square(1.5) && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()))
            return;

        if (EUClient.MODULE_MANAGER.getModule(HoleSnapModule.class).isToggled()) return;
        if (EUClient.MODULE_MANAGER.getModule(HitboxDesyncModule.class).isToggled()) return;

        MovementUtils.moveTowards(event, Vec3d.ofCenter(player.getBlockPos(), 0), MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
    }

    @SubscribeEvent
    public void onEnable() {
        if (side.getValue().equalsIgnoreCase("Client")) {
            try {
                if (mc.player == null || mc.world == null) {
                    if (client.getPrimarySocket() != null) client.stopConnection();
                    return;
                }

                client.startConnection("127.0.0.1", Integer.parseInt(port.getValue()));
            } catch (IOException exception) {
                EUClient.LOGGER.error("An exception has been thrown by clientside EU Robotics!", exception);
                EUClient.CHAT_MANAGER.error("Failed to establish a connection to the EURobotics server.");
                setToggled(false);
            }
        } else {
            try {
                if (mc.player == null || mc.world == null) {
                    if (server.getServerSocket() != null) server.stopConnection();
                    return;
                }

                new Thread(() -> {
                    try {
                        server.startConnection(Integer.parseInt(port.getValue()));
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }).start();
            } catch (IOException exception) {
                EUClient.LOGGER.error("An exception has been thrown by serverside EU Robotics!", exception);
                EUClient.CHAT_MANAGER.error("Failed to start the EURobotics server.");
                setToggled(false);
            }
        }
    }

    public boolean shouldStep() {
        if (!isToggled()) return false;

        PlayerEntity player = getPlayer();
        if (player == null) return false;

        if (mc.player.squaredDistanceTo(player.getPos()) <= 0.25) return false;
        if (mc.player.squaredDistanceTo(player.getPos()) <= MathHelper.square(1.5) && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed())) return false;

        return true;
    }

    private PlayerEntity getPlayer() {
        if (target.isEmpty()) return null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.getName().getString().equals(target)) {
                return player;
            }
        }

        return null;
    }

    @Getter
    public static class Client {
        private Socket primarySocket;
        private PrintWriter primaryOut;

        private Socket secondarySocket;
        private PrintWriter secondaryOut;

        private Socket tertiarySocket;
        private PrintWriter tertiaryOut;

        public void startConnection(String ip, int port) throws IOException {
            primarySocket = new Socket(ip, port);
            primaryOut = new PrintWriter(primarySocket.getOutputStream(), true);

            secondarySocket = new Socket(ip, port + 1);
            secondaryOut = new PrintWriter(secondarySocket.getOutputStream(), true);

            tertiarySocket = new Socket(ip, port + 2);
            tertiaryOut = new PrintWriter(tertiarySocket.getOutputStream(), true);
        }

        public void stopConnection() throws IOException {
            primaryOut.close();
            primarySocket.close();
        }

        public void sendMessage(String msg) throws IOException {
            primaryOut.println(msg);
            secondaryOut.println(msg);
            tertiaryOut.println(msg);
        }
    }

    @Getter
    public static class Server {
        private ServerSocket serverSocket;
        private Socket clientSocket;
        private BufferedReader in;

        public void startConnection(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] split = inputLine.split(";");

                if (split[0].equalsIgnoreCase("module") && split.length == 3) {
                    Module module = EUClient.MODULE_MANAGER.getModule(split[1]);
                    if (module != null) {
                        EUClient.TASK_MANAGER.submit(() -> module.setToggled(Boolean.parseBoolean(split[2])));
                    }
                }

                if (split[0].equalsIgnoreCase("update") && split.length == 2) {
                    EUClient.MODULE_MANAGER.getModule(EURoboticsModule.class).setTarget(split[1]);
                }

                if (split[0].equalsIgnoreCase("key") && split.length == 5 && mc.keyboard != null) {
                    EUClient.TASK_MANAGER.submit(() -> mc.keyboard.onKey(mc.getWindow().getHandle(), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4])));
                }

                if (split[0].equalsIgnoreCase("mouse") && split.length == 4 && mc.mouse != null) {
                    EUClient.TASK_MANAGER.submit(() -> ((MouseAccessor) mc.mouse).invokeOnMouseButton(mc.getWindow().getHandle(), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3])));
                }

                if (split[0].equalsIgnoreCase("yaw") && split.length == 2 && mc.player != null) {
                    mc.player.setYaw(Float.parseFloat(split[1]));
                }

                if (split[0].equalsIgnoreCase("pitch") && split.length == 2 && mc.player != null) {
                    mc.player.setPitch(Float.parseFloat(split[1]));
                }

                if (!EUClient.MODULE_MANAGER.getModule(EURoboticsModule.class).isToggled()) {
                    stopConnection();
                    break;
                }
            }
        }

        public void stopConnection() throws IOException {
            in.close();
            clientSocket.close();
            serverSocket.close();
        }
    }
}
