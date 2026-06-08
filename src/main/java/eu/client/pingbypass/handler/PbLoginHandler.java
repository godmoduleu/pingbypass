package eu.client.pingbypass.handler;

import com.mojang.authlib.GameProfile;
import eu.client.pingbypass.server.ProxyServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.state.ConfigurationStates;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Random;

/**
 * Handles the login state of the PingBypass proxy connection.
 * Performs RSA key exchange, enables encryption, sends LoginSuccess,
 * then transitions to PbPasswordHandler or play state.
 */
public class PbLoginHandler implements ServerLoginPacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbLoginHandler.class);
    private static final Random RANDOM = new Random();

    private final ProxyServer proxyServer;
    private final ClientConnection connection;
    private final KeyPair keyPair;
    private final byte[] nonce = new byte[4];

    private LoginState state = LoginState.HELLO;
    private String playerName;

    public PbLoginHandler(ProxyServer proxyServer, ClientConnection connection) {
        this.proxyServer = proxyServer;
        this.connection = connection;
        try {
            this.keyPair = NetworkEncryptionUtils.generateServerKeyPair();
        } catch (NetworkEncryptionException e) {
            throw new IllegalStateException("Failed to generate RSA keypair", e);
        }
        RANDOM.nextBytes(this.nonce);
    }

    @Override
    public void onHello(LoginHelloC2SPacket packet) {
        if (this.state != LoginState.HELLO) {
            throw new IllegalStateException("Unexpected hello packet");
        }

        this.playerName = packet.name();
        LOGGER.info("Login attempt from {}", this.playerName);

        this.state = LoginState.KEY;
        this.connection.send(new LoginHelloS2CPacket(
                "",
                this.keyPair.getPublic().getEncoded(),
                this.nonce,
                false
        ));
    }

    @Override
    public void onKey(LoginKeyC2SPacket packet) {
        if (this.state != LoginState.KEY) {
            throw new IllegalStateException("Unexpected key packet");
        }

        PrivateKey privateKey = this.keyPair.getPrivate();

        if (!packet.verifySignedNonce(this.nonce, privateKey)) {
            throw new IllegalStateException("Protocol error: invalid nonce!");
        }

        SecretKey secretKey;
        try {
            secretKey = packet.decryptSecretKey(privateKey);
        } catch (NetworkEncryptionException e) {
            throw new IllegalStateException("Protocol error", e);
        }
        this.state = LoginState.READY_TO_ACCEPT;

        try {
            Cipher decryptCipher = NetworkEncryptionUtils.cipherFromKey(2, secretKey);
            Cipher encryptCipher = NetworkEncryptionUtils.cipherFromKey(1, secretKey);
            this.connection.setupEncryption(decryptCipher, encryptCipher);
        } catch (NetworkEncryptionException e) {
            throw new IllegalStateException("Protocol error", e);
        }

        acceptPlayer();
    }

    private GameProfile acceptedProfile;

    private void acceptPlayer() {
        GameProfile profile = new GameProfile(
                Uuids.getOfflinePlayerUuid(this.playerName),
                this.playerName
        );

        this.acceptedProfile = profile;
        this.state = LoginState.PROTOCOL_SWITCHING;
        this.connection.send(new LoginSuccessS2CPacket(profile));
        LOGGER.info("Login success for {} ({}), waiting for LOGIN_ACKNOWLEDGED", profile.getName(), profile.getId());
    }

    @Override
    public void onEnterConfiguration(EnterConfigurationC2SPacket packet) {
        if (this.state != LoginState.PROTOCOL_SWITCHING) {
            throw new IllegalStateException("Unexpected login acknowledgement packet");
        }

        // Client has acknowledged login — now transition to CONFIGURATION phase.
        // The PbConfigurationHandler will perform a minimal config handshake
        // (send FINISH_CONFIGURATION, wait for client ack) then transition to PLAY.
        this.connection.transitionOutbound(ConfigurationStates.S2C);
        this.connection.transitionInbound(
                ConfigurationStates.C2S,
                new PbConfigurationHandler(this.proxyServer, this.connection, this.acceptedProfile)
        );
        this.state = LoginState.ACCEPTED;
        LOGGER.info("Transitioning {} to configuration phase", this.playerName);
    }

    @Override
    public void onQueryResponse(LoginQueryResponseC2SPacket packet) {
        this.disconnect(Text.literal("Unexpected query response"));
    }

    @Override
    public void onCookieResponse(CookieResponseC2SPacket packet) {
        this.disconnect(Text.literal("Unexpected cookie response"));
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        LOGGER.info("Client {} disconnected during login: {}",
                this.playerName != null ? this.playerName : "unknown",
                info.reason());
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }

    private void disconnect(Text reason) {
        try {
            this.connection.send(new LoginDisconnectS2CPacket(reason));
            this.connection.disconnect(reason);
        } catch (Exception e) {
            LOGGER.error("Error disconnecting client", e);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    private enum LoginState {
        HELLO,
        KEY,
        READY_TO_ACCEPT,
        PROTOCOL_SWITCHING,
        ACCEPTED
    }
}
