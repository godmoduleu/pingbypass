package eu.client.pingbypass.modules;

import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.ModuleManager;
import eu.client.settings.Setting;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test: Proxy module independence (Property 5).
 *
 * <p><b>Validates: Requirements 8.4</b></p>
 *
 * <p>For any pair of distinct proxy modules A and B managed by the
 * ProxyModuleManager, toggling module A's enabled state SHALL NOT change
 * module B's enabled state or any of module B's setting values.</p>
 *
 * <p>Uses mocked Module instances to avoid loading the full Minecraft
 * environment in tests.</p>
 */
class ProxyModuleIndependencePropertyTest {

    private ProxyModuleManager manager;
    private ModuleManager mockModuleManager;
    private Object originalModuleManager;

    @BeforeProperty
    void setUp() throws Exception {
        originalModuleManager = EUClient.MODULE_MANAGER;
        mockModuleManager = mock(ModuleManager.class);

        // Create 8 mock modules with independent state
        Module[] modules = new Module[8];
        String[] names = {"AutoArmor", "AutoCrystal", "AutoTotem", "AutoTrap",
                "HoleFill", "SelfFill", "SelfTrap", "Surround"};
        Class<?>[] classes = {
                eu.client.modules.impl.combat.AutoArmorModule.class,
                eu.client.modules.impl.combat.AutoCrystalModule.class,
                eu.client.modules.impl.combat.AutoTotemModule.class,
                eu.client.modules.impl.combat.AutoTrapModule.class,
                eu.client.modules.impl.combat.HoleFillModule.class,
                eu.client.modules.impl.combat.SelfFillModule.class,
                eu.client.modules.impl.combat.SelfTrapModule.class,
                eu.client.modules.impl.combat.SurroundModule.class
        };

        for (int i = 0; i < 8; i++) {
            modules[i] = createMockModule(names[i],
                    new BooleanSetting("Setting1", "", false),
                    new NumberSetting("Setting2", "", 5.0, 0.0, 10.0));
            doReturn(modules[i]).when(mockModuleManager).getModule((Class<? extends Module>) classes[i]);
        }

        setStaticField(EUClient.class, "MODULE_MANAGER", mockModuleManager);

        manager = new ProxyModuleManager();
        manager.init();
    }

    @AfterProperty
    void tearDown() throws Exception {
        setStaticField(EUClient.class, "MODULE_MANAGER", originalModuleManager);
    }

    @Provide
    Arbitrary<List<Boolean>> toggleSequences() {
        return Arbitraries.of(true, false).list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<Integer> moduleIndexA() {
        return Arbitraries.integers().between(0, 7);
    }

    @Provide
    Arbitrary<Integer> moduleIndexB() {
        return Arbitraries.integers().between(0, 7);
    }

    @Property(tries = 100)
    void togglingModuleA_doesNotAffectModuleB(
            @ForAll("moduleIndexA") int idxA,
            @ForAll("moduleIndexB") int idxB,
            @ForAll("toggleSequences") List<Boolean> toggles
    ) {
        Assume.that(idxA != idxB);

        List<Module> modules = manager.getModules();
        Assume.that(idxA < modules.size() && idxB < modules.size());

        Module moduleA = modules.get(idxA);
        Module moduleB = modules.get(idxB);

        boolean bToggledBefore = moduleB.isToggled();
        var bSettingsBefore = snapshotSettings(moduleB);

        for (boolean enabled : toggles) {
            moduleA.setToggled(enabled);
        }

        assertEquals(bToggledBefore, moduleB.isToggled(),
                "Toggling " + moduleA.getName() + " should not change " + moduleB.getName() + "'s toggled state");

        var bSettingsAfter = snapshotSettings(moduleB);
        assertEquals(bSettingsBefore.size(), bSettingsAfter.size());
        for (int i = 0; i < bSettingsBefore.size(); i++) {
            assertEquals(bSettingsBefore.get(i), bSettingsAfter.get(i),
                    "Setting on " + moduleB.getName() + " should not change");
        }

        moduleA.setToggled(false);
    }

    private List<String> snapshotSettings(Module module) {
        return module.getSettings().stream()
                .map(this::settingValueToString)
                .toList();
    }

    private String settingValueToString(Setting setting) {
        if (setting instanceof BooleanSetting bs) return String.valueOf(bs.getValue());
        if (setting instanceof NumberSetting ns) return String.valueOf(ns.getValue());
        return setting.getName();
    }

    private Module createMockModule(String name, Setting... settings) {
        Module module = mock(Module.class);
        when(module.getName()).thenReturn(name);
        when(module.getSettings()).thenReturn(List.of(settings));
        final boolean[] toggled = {false};
        doAnswer(inv -> { toggled[0] = inv.getArgument(0); return null; })
                .when(module).setToggled(anyBoolean());
        doAnswer(inv -> { toggled[0] = inv.getArgument(0); return null; })
                .when(module).setToggled(anyBoolean(), anyBoolean());
        when(module.isToggled()).thenAnswer(inv -> toggled[0]);
        return module;
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
