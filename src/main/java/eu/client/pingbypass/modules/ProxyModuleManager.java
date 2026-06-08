package eu.client.pingbypass.modules;

import eu.client.Pingbypass;
import eu.client.modules.Module;
import eu.client.modules.impl.combat.*;
import eu.client.modules.impl.player.SpeedMineModule;
import eu.client.settings.Setting;

import java.util.*;

public class ProxyModuleManager {

    private static final Set<Class<? extends Module>> PROXY_SAFE_MODULES = Set.of(
            AutoTotemModule.class,
            AutoCrystalModule.class,
            SurroundModule.class,
            AutoArmorModule.class,
            HoleFillModule.class,
            SelfFillModule.class,
            SelfTrapModule.class,
            AutoTrapModule.class,
            SpeedMineModule.class
    );

    private final List<Module> modules = new ArrayList<>();

    public void init() {
        if (Pingbypass.MODULE_MANAGER == null) {
            Pingbypass.LOGGER.warn("ProxyModuleManager.init() called before MODULE_MANAGER — no modules registered");
            return;
        }

        for (Class<? extends Module> clazz : PROXY_SAFE_MODULES) {
            Module module = Pingbypass.MODULE_MANAGER.getModule(clazz);
            if (module != null) {
                modules.add(module);
            } else {
                Pingbypass.LOGGER.warn("Proxy-safe module {} not found in ModuleManager", clazz.getSimpleName());
            }
        }

        modules.sort(Comparator.comparing(Module::getName));
        Pingbypass.LOGGER.info("ProxyModuleManager initialized with {} modules", modules.size());
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getModules() {
        return modules;
    }
}
