package eu.client.modules.impl.visuals;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "ViewModel", description = "Modifies the position, scale and rotation of the player viewmodel.", category = Module.Category.VISUALS)
public class ViewModelModule extends Module {
    public CategorySetting translation = new CategorySetting("Translation", "The category that contains settings relating to modifying the translation of the items.");
    public NumberSetting translateX = new NumberSetting("TranslateX", "X", "The translation of the items on the X axis.", new CategorySetting.Visibility(translation), 0.0f, -2.0f, 2.0f);
    public NumberSetting translateY = new NumberSetting("TranslateY", "Y", "The translation of the items on the Y axis.", new CategorySetting.Visibility(translation), 0.0f, -2.0f, 2.0f);
    public NumberSetting translateZ = new NumberSetting("TranslateZ", "Z", "The translation of the items on the Z axis.", new CategorySetting.Visibility(translation), 0.0f, -2.0f, 2.0f);

    public CategorySetting rotation = new CategorySetting("Rotation", "The category that contains settings relating to modifying the rotation of the items.");
    public NumberSetting rotateX = new NumberSetting("RotateX", "X", "The rotation of the items on the X axis.", new CategorySetting.Visibility(rotation), 0, -180, 180);
    public NumberSetting rotateY = new NumberSetting("RotateY", "Y", "The rotation of the items on the Y axis.", new CategorySetting.Visibility(rotation), 0, -180, 180);
    public NumberSetting rotateZ = new NumberSetting("RotateZ", "Z", "The rotation of the items on the Z axis.", new CategorySetting.Visibility(rotation), 0, -180, 180);

    public CategorySetting scale = new CategorySetting("Scale", "The category that contains settings relating to modifying the scale of the items.");
    public NumberSetting scaleX = new NumberSetting("ScaleX", "X", "The scaling on of the items the X axis.", new CategorySetting.Visibility(scale), 1.0f, 0.0f, 3.0f);
    public NumberSetting scaleY = new NumberSetting("ScaleY", "Y", "The scaling on of the items the Y axis.", new CategorySetting.Visibility(scale), 1.0f, 0.0f, 3.0f);
    public NumberSetting scaleZ = new NumberSetting("ScaleZ", "Z", "The scaling on of the items the Z axis.", new CategorySetting.Visibility(scale), 1.0f, 0.0f, 3.0f);
}
