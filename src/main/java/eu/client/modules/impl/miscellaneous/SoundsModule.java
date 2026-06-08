package eu.client.modules.impl.miscellaneous;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.AttackEntityEvent;
import eu.client.events.impl.TargetDeathEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.system.FileUtils;

import java.io.File;

@RegisterModule(name = "Sounds", description = "Plays custom sounds when something happens.", category = Module.Category.MISCELLANEOUS)
public class SoundsModule extends Module {
    CategorySetting killsCategory = new CategorySetting("Kills", "The kills category for the sounds.");
    BooleanSetting killSound = new BooleanSetting("KillSound", "Play sounds when you kill a player.", new CategorySetting.Visibility(killsCategory), true);
    NumberSetting killVolume = new NumberSetting("KillVolume", "Volume", "The volume for the kill sounds.", new CategorySetting.Visibility(killsCategory), 1.0f, 0.0f, 1.0f);
    StringSetting killName = new StringSetting("KillName", "Name", "The name of the kill sound file.", new CategorySetting.Visibility(killsCategory), "killsound.wav");

    CategorySetting hitsCategory = new CategorySetting("Hits", "The hits category for the sounds.");
    BooleanSetting hitSound = new BooleanSetting("HitSound", "Play sounds when you hit an entity.", new CategorySetting.Visibility(hitsCategory), true);
    NumberSetting hitVolume = new NumberSetting("HitVolume", "Volume", "The volume for the hit sounds.", new CategorySetting.Visibility(hitsCategory), 1.0f, 0.0f, 1.0f);
    StringSetting hitName = new StringSetting("HitName", "Name", "The name of the hit sound file.", new CategorySetting.Visibility(hitsCategory), "hitsound.wav");

    @SubscribeEvent
    public void onTargetDeath(TargetDeathEvent event) {
        if(getNull()) return;

        if(killSound.getValue()) FileUtils.playSound(new File(EUClient.MOD_NAME + "/Client/" + killName.getValue()), killVolume.getValue().floatValue());
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if(event.getPlayer() != mc.player) return;

        if(hitSound.getValue()) FileUtils.playSound(new File(EUClient.MOD_NAME + "/Client/" + hitName.getValue()), hitVolume.getValue().floatValue());

    }
}
