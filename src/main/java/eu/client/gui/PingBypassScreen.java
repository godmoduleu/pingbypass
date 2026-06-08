package eu.client.gui;

import eu.client.EUClient;
import eu.client.pingbypass.PingBypassConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * PingBypass connection screen — allows configuring proxy IP/port/password
 * and target server, then connecting or resuming a session.
 */
public class PingBypassScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget ipField;
    private TextFieldWidget portField;
    private TextFieldWidget passwordField;
    private TextFieldWidget serverField;
    private ButtonWidget connectButton;
    private ButtonWidget resumeButton;
    private volatile String proxyStatus = "Pinging...";
    private volatile boolean pinged = false;

    public PingBypassScreen(Screen parent) {
        super(Text.literal("PingBypass"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;

        // IP field
        ipField = new TextFieldWidget(textRenderer, centerX - 150, startY, 300, 20, Text.literal("Proxy IP"));
        ipField.setMaxLength(256);
        ipField.setText(getConfig().getIp());
        ipField.setPlaceholder(Text.literal("127.0.0.1"));
        addDrawableChild(ipField);

        // Port field
        portField = new TextFieldWidget(textRenderer, centerX - 150, startY + 34, 145, 20, Text.literal("Port"));
        portField.setMaxLength(5);
        portField.setText(String.valueOf(getConfig().getPort()));
        portField.setPlaceholder(Text.literal("25565"));
        addDrawableChild(portField);

        // Password field
        passwordField = new TextFieldWidget(textRenderer, centerX + 5, startY + 34, 145, 20, Text.literal("Password"));
        passwordField.setMaxLength(64);
        passwordField.setText(getConfig().getPassword());
        passwordField.setPlaceholder(Text.literal("Password"));
        addDrawableChild(passwordField);

        // Server target field
        serverField = new TextFieldWidget(textRenderer, centerX - 150, startY + 68, 300, 20, Text.literal("Target Server"));
        serverField.setMaxLength(256);
        String savedServer = EUClient.MODULE_MANAGER.getModule(
                eu.client.modules.impl.core.PingBypassModule.class).server.getValue();
        serverField.setText(savedServer.isEmpty() ? "" : savedServer);
        serverField.setPlaceholder(Text.literal("mc.server.com:25565"));
        addDrawableChild(serverField);

        // Connect button
        connectButton = ButtonWidget.builder(Text.literal("Connect"), button -> connect())
                .dimensions(centerX - 150, startY + 110, 300, 20)
                .build();
        addDrawableChild(connectButton);

        // Resume button (only active if PingBypass module is toggled = connected to proxy)
        var pbModule = EUClient.MODULE_MANAGER.getModule(eu.client.modules.impl.core.PingBypassModule.class);
        resumeButton = ButtonWidget.builder(Text.literal("Resume Session"), button -> resume())
                .dimensions(centerX - 150, startY + 138, 300, 20)
                .build();
        resumeButton.active = pbModule != null && pbModule.isToggled();
        addDrawableChild(resumeButton);

        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(centerX - 150, this.height - 30, 300, 20)
                .build());

        // Ping the proxy to check its status
        pingProxy();
    }

    private void pingProxy() {
        pinged = false;
        proxyStatus = "Pinging...";
        String proxyIp = getConfig().getIp();
        int proxyPort = getConfig().getPort();

        new Thread(() -> {
            try {
                var address = new ServerAddress(proxyIp, proxyPort);
                var serverInfo = new ServerInfo("PingBypass", address.getAddress() + ":" + address.getPort(), ServerInfo.ServerType.OTHER);
                net.minecraft.client.network.MultiplayerServerListPinger pinger = new net.minecraft.client.network.MultiplayerServerListPinger();
                pinger.add(serverInfo, () -> {}, () -> {});
                // Wait briefly for the ping to complete
                Thread.sleep(2000);
                pinger.tick();
                pinger.cancel();

                if (serverInfo.label != null) {
                    proxyStatus = serverInfo.label.getString();
                } else if (serverInfo.playerCountLabel != null) {
                    proxyStatus = serverInfo.playerCountLabel.getString();
                } else {
                    proxyStatus = "Proxy online";
                }
                pinged = true;
            } catch (Exception e) {
                proxyStatus = "§cOffline — cannot reach proxy";
                pinged = true;
            }
        }, "PingBypass-Pinger").start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, "PingBypass", width / 2, 20, 0xFFFFFF);

        // Labels
        int centerX = width / 2;
        int startY = 50;
        context.drawTextWithShadow(textRenderer, "Proxy IP", centerX - 150, startY - 11, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, "Port", centerX - 150, startY + 23, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, "Password", centerX + 5, startY + 23, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, "Target Server", centerX - 150, startY + 57, 0xA0A0A0);

        // Status from ping
        int statusColor = proxyStatus.contains("Connected") ? 0x55FF55
                : proxyStatus.contains("Idle") ? 0xFFAA00
                : proxyStatus.contains("Offline") ? 0xFF5555
                : 0xAAAAAA;
        context.drawCenteredTextWithShadow(textRenderer, proxyStatus, width / 2, startY + 165, statusColor);

        // Active session status
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive) {
            var pbModule = EUClient.MODULE_MANAGER.getModule(eu.client.modules.impl.core.PingBypassModule.class);
            String session = "Session active — " + (pbModule.getServerName() != null ? pbModule.getServerName() : "connected");
            context.drawCenteredTextWithShadow(textRenderer, session, width / 2, startY + 178, 0x55FF55);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void connect() {
        // Save settings to the module
        var pbModule = EUClient.MODULE_MANAGER.getModule(eu.client.modules.impl.core.PingBypassModule.class);
        pbModule.ip.setValue(ipField.getText().trim());
        try {
            pbModule.port.setValue(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        pbModule.password.setValue(passwordField.getText());
        pbModule.server.setValue(serverField.getText().trim());

        // Disable first if already toggled (reset state), then re-enable
        if (pbModule.isToggled()) {
            pbModule.setToggled(false, false);
        }
        pbModule.setToggled(true);
    }

    private void resume() {
        // Return to parent — session is already active, just close this screen
        client.setScreen(parent);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private PingBypassConfig getConfig() {
        return EUClient.PINGBYPASS_CONFIG;
    }
}
