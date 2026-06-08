package eu.client.pingbypass.modules;

import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.impl.combat.*;
import eu.client.modules.impl.player.SpeedMineModule;
import eu.client.settings.Setting;

import java.util.*;

/**
 * Manages the subset of existing client modules that are safe to run
 * server-side on the PingBypass proxy. Instead of creating separate
 * wrapper classes, this manager references the real {@link Module}
 * instances from {@link eu.client.modules.ModuleManager} — the same
 * code runs on both client and proxy since HeadlessMC provides the
 * full {@code mc.player} / {@code mc.world} API.
 *
 * <p>Modules that require rendering, client input, or a display
 * (visuals, GUI, movement) are excluded.</p>
 */
public class ProxyModuleManager {

    /**
     * The set of module classes that are safe to run on the headless proxy.
     * These only use mc.player, mc.world, and mc.player.networkHandler —
     * no rendering, no GUI, no client input.
     */
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

    /**
     * Initializes the proxy module list by pulling the real module instances
     * from the global {@link eu.client.modules.ModuleManager}. Must be called
     * after {@code EUClient.MODULE_MANAGER} is initialized.
     */
    public void init() {
        if (EUClient.MODULE_MANAGER == null) {
            EUClient.LOGGER.warn("ProxyModuleManager.init() called before MODULE_MANAGER — no modules registered");
            return;
        }

        for (Class<? extends Module> clazz : PROXY_SAFE_MODULES) {
            Module module = EUClient.MODULE_MANAGER.getModule(clazz);
            if (module != null) {
                modules.add(module);
            } else {
                EUClient.LOGGER.warn("Proxy-safe module {} not found in ModuleManager", clazz.getSimpleName());
            }
        }

        modules.sort(Comparator.comparing(Module::getName));
        EUClient.LOGGER.info("ProxyModuleManager initialized with {} modules", modules.size());
    }

    /**
     * Looks up a proxy module by name (case-insensitive).
     */
    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all proxy-safe modules.
     */
    public List<Module> getModules() {
        return modules;
    }
}
