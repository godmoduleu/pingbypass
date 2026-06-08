package eu.client.utils.mixins;

public interface IChatHudLineVisible {
    boolean euclient$isClientMessage();

    void euclient$setClientMessage(boolean clientMessage);

    String euclient$getClientIdentifier();

    void euclient$setClientIdentifier(String clientIdentifier);
}