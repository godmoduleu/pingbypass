package eu.client.utils.mixins;

public interface IChatHudLineVisible {
    boolean pingbypass$isClientMessage();

    void pingbypass$setClientMessage(boolean clientMessage);

    String pingbypass$getClientIdentifier();

    void pingbypass$setClientIdentifier(String clientIdentifier);
}