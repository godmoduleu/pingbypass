package eu.client.pingbypass.modules;

import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.ModuleManager;
import eu.client.settings.Setting;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProxyModuleManager.
 *
 * <p>Verifies that the manager correctly pulls Module instances from
 * the global ModuleManager and exposes them for proxy-side use.</p>
 *
 * <p>Uses mocked ModuleManager and Module instances to avoid loading
 * the full Minecraft environment in tests.</p>
 *
 * <p><b>Validates: Requirements 8.1, 8.4</b></p>
 */
class ProxyModuleManagerTest {

    private ProxyModuleManager manager;
    private ModuleManager mockModuleManager;

    // Mock modules representing the 8 proxy-safe combat modules
    private Module autoTotem, autoCrystal, surround, autoArmor,
            holeFill, selfFill, selfTrap, autoTrap;

    @BeforeEach
    void setUp() throws Exception {
        mockModuleManager = mock(ModuleManager.class);

        autoTotem = createMockModule("AutoTotem", new BooleanSetting("Soft", "", false));
        autoCrystal = createMockModule("AutoCrystal",
                new BooleanSetting("Place", "", true),
                new BooleanSetting("Break", "", true),
                new NumberSetting("PlaceRange", "", 4.5, 1.0, 6.0),
                new NumberSetting("BreakRange", "", 4.5, 1.0, 6.0));
        surround = createMockModule("Surround", new BooleanSetting("Center", "", true));
        autoArmor = createMockModule("AutoArmor");
        holeFill = createMockModule("HoleFill");
        selfFill = createMockModule("SelfFill");
        selfTrap = createMockModule("SelfTrap");
        autoTrap = createMockModule("AutoTrap");

        // Wire up the mock ModuleManager to return our mock modules by class
        // Use doReturn().when() to avoid generic type mismatch with when().thenReturn()
        doReturn(autoTotem).when(mockModuleManager).getModule(eu.client.modules.impl.combat.AutoTotemModule.class);
        doReturn(autoCrystal).when(mockModuleManager).getModule(eu.client.modules.impl.combat.AutoCrystalModule.class);
        doReturn(surround).when(mockModuleManager).getModule(eu.client.modules.impl.combat.SurroundModule.class);
        doReturn(autoArmor).when(mockModuleManager).getModule(eu.client.modules.impl.combat.AutoArmorModule.class);
        doReturn(holeFill).when(mockModuleManager).getModule(eu.client.modules.impl.combat.HoleFillModule.class);
        doReturn(selfFill).when(mockModuleManager).getModule(eu.client.modules.impl.combat.SelfFillModule.class);
        doReturn(selfTrap).when(mockModuleManager).getModule(eu.client.modules.impl.combat.SelfTrapModule.class);
        doReturn(autoTrap).when(mockModuleManager).getModule(eu.client.modules.impl.combat.AutoTrapModule.class);

        // Inject the mock ModuleManager
        setStaticField(EUClient.class, "MODULE_MANAGER", mockModuleManager);

        manager = new ProxyModuleManager();
        manager.init();
    }

    // ========================================================================
    // Module registration and lookup (Requirement 8.1)
    // ========================================================================

    @Test
    void init_registersExpectedModules() {
        assertNotNull(manager.getModules());
        assertEquals(8, manager.getModules().size(),
                "Should register 8 proxy-safe combat modules");
    }

    @Test
    void getModule_byName_returnsCorrectModule() {
        Module totem = manager.getModule("AutoTotem");
        assertNotNull(totem, "Should find AutoTotem by name");
        assertEquals("AutoTotem", totem.getName());
    }

    @Test
    void getModule_byName_caseInsensitive() {
        Module crystal = manager.getModule("autocrystal");
        assertNotNull(crystal, "Should find module with case-insensitive lookup");
        assertEquals("AutoCrystal", crystal.getName());
    }

    @Test
    void getModule_unknownName_returnsNull() {
        assertNull(manager.getModule("NonExistentModule"),
                "Should return null for unknown module name");
    }

    @Test
    void getModules_returnsSortedByName() {
        var modules = manager.getModules();
        for (int i = 1; i < modules.size(); i++) {
            assertTrue(modules.get(i - 1).getName().compareTo(modules.get(i).getName()) <= 0,
                    "Modules should be sorted alphabetically by name");
        }
    }

    @Test
    void registeredModules_haveSettings() {
        Module crystal = manager.getModule("AutoCrystal");
        assertNotNull(crystal);
        assertFalse(crystal.getSettings().isEmpty(),
                "AutoCrystal should have settings registered");
        assertEquals(4, crystal.getSettings().size());
    }

    @Test
    void getSetting_unknownName_returnsNull() {
        Module totem = manager.getModule("AutoTotem");
        assertNotNull(totem);
        assertNull(totem.getSetting("NonExistent"),
                "Should return null for unknown setting name");
    }

    // ========================================================================
    // Toggle behavior (Requirement 8.4)
    // ========================================================================

    @Test
    void setToggled_true_enablesModule() {
        Module totem = manager.getModule("AutoTotem");
        assertNotNull(totem);
        totem.setToggled(true);
        assertTrue(totem.isToggled());
        totem.setToggled(false);
    }

    @Test
    void setToggled_false_disablesModule() {
        Module totem = manager.getModule("AutoTotem");
        assertNotNull(totem);
        totem.setToggled(true);
        totem.setToggled(false);
        assertFalse(totem.isToggled());
    }

    @Test
    void toggleOneModule_doesNotAffectOthers() {
        Module totem = manager.getModule("AutoTotem");
        Module crystal = manager.getModule("AutoCrystal");
        assertNotNull(totem);
        assertNotNull(crystal);

        boolean crystalBefore = crystal.isToggled();
        totem.setToggled(true);
        assertEquals(crystalBefore, crystal.isToggled(),
                "Enabling AutoTotem should not affect AutoCrystal");
        totem.setToggled(false);
    }

    @Test
    void modules_areRealModuleInstances() {
        Module totem = manager.getModule("AutoTotem");
        assertSame(autoTotem, totem,
                "ProxyModuleManager should reference the same Module instance from ModuleManager");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private Module createMockModule(String name, Setting... settings) {
        Module module = mock(Module.class);
        when(module.getName()).thenReturn(name);
        when(module.getSettings()).thenReturn(List.of(settings));
        when(module.getSetting(anyString())).thenAnswer(inv -> {
            String settingName = inv.getArgument(0);
            for (Setting s : settings) {
                if (s.getName().equalsIgnoreCase(settingName)) return s;
            }
            return null;
        });
        // Track toggled state
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
