package eu.client.modules.impl.visuals;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "NoRender", description = "Disables the rendering of certain things.", category = Module.Category.VISUALS)
public class NoRenderModule extends Module {
    public BooleanSetting hurtCamera = new BooleanSetting("HurtCamera", "Disables the rendering of the hurt camera.", true);
    public BooleanSetting explosions = new BooleanSetting("Explosions", "Disables the rendering of explosion particles.", true);
    public BooleanSetting fireOverlay = new BooleanSetting("FireOverlay", "Disables the rendering of the fire overlay.", true);
    public BooleanSetting blockOverlay = new BooleanSetting("BlockOverlay", "Disables the rendering of the block suffocation overlay.", false);
    public BooleanSetting liquidOverlay = new BooleanSetting("LiquidOverlay", "Disables the rendering of the liquid overlay.", false);
    public BooleanSetting snowOverlay = new BooleanSetting("SnowOverlay", "Disables the rendering of the snow overlay.", false);
    public BooleanSetting pumpkinOverlay = new BooleanSetting("PumpkinOverlay", "Disables the rendering of the pumpkin overlay.", true);
    public BooleanSetting portalOverlay = new BooleanSetting("PortalOverlay", "Disables the rendering of the portal overlay.", false);
    public BooleanSetting totemAnimation = new BooleanSetting("TotemAnimation", "Disables the rendering of the totem pop animation.", false);
    public BooleanSetting bossBar = new BooleanSetting("BossBar", "Disables the rendering of the boss bar.", false);
    public BooleanSetting vignette = new BooleanSetting("Vignette", "Disables the rendering of the vignette.", true);
    public BooleanSetting blindness = new BooleanSetting("Blindness", "Disables the rendering of the blindness and darkness potion effects.", true);
    public BooleanSetting fog = new BooleanSetting("Fog", "Disables the rendering of the fog.", false);
    public BooleanSetting signText = new BooleanSetting("SignText", "Disables the rendering of sign text.", false);
    public BooleanSetting armor = new BooleanSetting("Armor", "Disables the rendering of armor.", false);
    public BooleanSetting limbSwing = new BooleanSetting("LimbSwing", "Disables the rendering of limb swing animations.", false);
    public BooleanSetting corpses = new BooleanSetting("Corpses", "Disables the rendering of corpses.", false);
    public ModeSetting tileEntities = new ModeSetting("TileEntities", "Disables the rendering of tile entities, such as chests, when meeting requirements.", "Never", new String[]{"Never", "Distance", "Always"});
    public NumberSetting tileDistance = new NumberSetting("TileDistance", "The distance at which tile entities will stop rendering.", new ModeSetting.Visibility(tileEntities, "Distance"), 10.0f, 0.0f, 100.0f);
}
