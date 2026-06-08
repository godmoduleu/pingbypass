package eu.client.mixins.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Invoker("isWalking")
    boolean invokeIsWalking();

    @Invoker("canSprint")
    boolean invokeCanSprint();

    @Invoker("sendMovementPackets")
    void invokeSendMovementPackets();

    @Accessor("lastOnGround")
    void setLastOnGround(boolean lastOnGround);

    @Accessor("lastYaw")
    void setLastYaw(float lastYaw);

    @Accessor("lastPitch")
    void setLastPitch(float lastPitch);
}
