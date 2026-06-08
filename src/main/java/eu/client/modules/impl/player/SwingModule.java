package eu.client.modules.impl.player;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "Swing", description = "Allows you to modify the swing animation and the hand that will be used for swinging.", category = Module.Category.PLAYER)
public class SwingModule extends Module {
    public ModeSetting hand = new ModeSetting("Hand", "The hand that will be used for swinging.", "Default", new String[]{"Default", "None", "Packet", "Mainhand", "Offhand", "Both"});
    public BooleanSetting noPacket = new BooleanSetting("NoPacket", "Only swings clientside, meaning nobody will be able to tell if you're swinging or not.", new ModeSetting.Visibility(hand, "Default", "Mainhand", "Offhand", "Both"), false);

    public CategorySetting speedCategory = new CategorySetting("Speed", "The category containing all settings related to swing speed.");
    public BooleanSetting modifySpeed = new BooleanSetting("ModifySpeed", "Enabled", "Modifies the speed of your swing animation.", new CategorySetting.Visibility(speedCategory), false);
    public NumberSetting speed = new NumberSetting("Speed", "Amount", "The speed at which your hand will be swinging.", new BooleanSetting.Visibility(modifySpeed, true), 15, 1, 20);

    public CategorySetting translationCategory = new CategorySetting("Translation", "The category related to the swing translations.");
    public BooleanSetting translateX = new BooleanSetting("TranslateX", "X", "Enables the swing translation on the X axis.", new CategorySetting.Visibility(translationCategory), true);
    public BooleanSetting translateY = new BooleanSetting("TranslateY", "Y", "Enables the swing translation on the Y axis.", new CategorySetting.Visibility(translationCategory), true);
    public BooleanSetting translateZ = new BooleanSetting("TranslateZ", "Z", "Enables the swing translation on the Z axis.", new CategorySetting.Visibility(translationCategory), true);

    public CategorySetting rotationCategory = new CategorySetting("Rotation", "The category related to the swing rotations.");
    public BooleanSetting rotationX = new BooleanSetting("RotationX", "X", "Enables the swing rotation on the X axis.", new CategorySetting.Visibility(rotationCategory), true);
    public BooleanSetting rotationY = new BooleanSetting("RotationY", "Y", "Enables the swing rotation on the Y axis.", new CategorySetting.Visibility(rotationCategory), true);
    public BooleanSetting rotationZ = new BooleanSetting("RotationZ", "Z", "Enables the swing rotation on the Z axis.", new CategorySetting.Visibility(rotationCategory), true);
}
