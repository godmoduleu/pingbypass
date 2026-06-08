package eu.client.modules.impl.miscellaneous;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.NumberSetting;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

@RegisterModule(name = "AutoReconnect", description = "Automatically reconnects you to a server after a specified time period.", category = Module.Category.MISCELLANEOUS)
public class AutoReconnectModule extends Module {
    public NumberSetting delay = new NumberSetting("Delay", "The amount of seconds that have to pass before reconnecting.", 5, 0, 20);

    @Override
    public String getMetaData() {
        return String.valueOf(delay.getValue().intValue());
    }
}
