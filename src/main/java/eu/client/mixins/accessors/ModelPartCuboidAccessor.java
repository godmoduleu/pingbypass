package eu.client.mixins.accessors;

import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.Cuboid.class)
public interface ModelPartCuboidAccessor {
    @Accessor("sides")
    ModelPart.Quad[] getSides();
}
