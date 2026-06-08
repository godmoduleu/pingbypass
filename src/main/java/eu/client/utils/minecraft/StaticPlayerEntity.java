package eu.client.utils.minecraft;

import com.mojang.authlib.GameProfile;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.mixins.accessors.LimbAnimatorAccessor;
import eu.client.utils.IMinecraft;
import eu.client.utils.graphics.ModelRenderer;
import eu.client.utils.mixins.ILivingEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;
import java.util.UUID;

public class StaticPlayerEntity extends OtherClientPlayerEntity implements IMinecraft {
    public StaticPlayerEntity(PlayerEntity player) {
        super(mc.world, new GameProfile(UUID.randomUUID(), player.getName().getString()));

        ((ILivingEntity) this).euclient$setStaticPlayerEntity(true);

        copyPositionAndRotation(player);
        prevYaw = getYaw();
        prevPitch = getPitch();
        headYaw = player.headYaw;
        prevHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        prevBodyYaw = bodyYaw;

        setSneaking(player.isSneaking());

        limbAnimator.setSpeed(player.limbAnimator.getSpeed());
        ((LimbAnimatorAccessor) limbAnimator).setPos(player.limbAnimator.getPos());

        setPose(player.getPose());
    }

    public void render(RenderWorldEvent event, boolean fill, Color fillColor, boolean outline, Color outlineColor) {
        ModelRenderer.renderModel(this, true, 1.0f, event.getTickDelta(), new ModelRenderer.Render(fill, fillColor, outline, outlineColor, false));
    }
}
