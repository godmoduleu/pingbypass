package eu.client.modules.impl.core;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import net.minecraft.util.Identifier;

@Getter
@RegisterModule(name = "Capes", description = "Applies the EUClient cape to yourself and to other users.", category = Module.Category.CORE, toggled = true, drawn = false)
public class CapesModule extends Module {
    public CapesModule() {
        this.capeTexture = Identifier.of(EUClient.MOD_ID, "textures/cape.png");
    }

    private final Identifier capeTexture;
}
