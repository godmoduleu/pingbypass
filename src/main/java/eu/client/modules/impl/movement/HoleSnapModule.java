package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerMoveEvent;
import eu.client.mixins.accessors.Vec3dAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.MovementUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.MathUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name = "HoleSnap", description = "Pulls you toward your nearest hole.", category = Module.Category.MOVEMENT)
public class HoleSnapModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "Range for the holes.", 5, 1, 8);
    public BooleanSetting doubleHoles = new BooleanSetting("DoubleHoles", "Whether or not to snap you to double holes.", true);
    public BooleanSetting quadHoles = new BooleanSetting("QuadHoles", "Whether or not to snap you to quad holes.", true);
    public BooleanSetting step = new BooleanSetting("Step", "Automatically steps when trying to holesnap.", false);

    public Box hole = null;

    @Override
    public void onEnable() {
        hole = null;
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (getNull() || mc.player.fallDistance >= 5.0f) return;

        List<HoleUtils.Hole> holes = getHoles();
        if(holes.isEmpty()) return;

        hole = holes.get(0).box();

        if(mc.player.getX() == hole.getCenter().x && mc.player.getY() == hole.minY && mc.player.getZ() == hole.getCenter().z) {
            if(EUClient.MODULE_MANAGER.getModule(StepModule.class).isToggled()) EUClient.MODULE_MANAGER.getModule(StepModule.class).setToggled(false);
            if(EUClient.MODULE_MANAGER.getModule(SpeedModule.class).isToggled()) EUClient.MODULE_MANAGER.getModule(SpeedModule.class).setToggled(false);
            setToggled(false);
            return;
        }

        MovementUtils.moveTowards(event, hole.getCenter(), MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
    }

    private List<HoleUtils.Hole> getHoles() {
        List<HoleUtils.Hole> holes = new ArrayList<>();

        for (int i = 0; i < EUClient.WORLD_MANAGER.getRadius(range.getValue().doubleValue()); i++) {
            BlockPos position = mc.player.getBlockPos().add(EUClient.WORLD_MANAGER.getOffset(i));

            if(position.getY() > mc.player.getY()) continue;

            HoleUtils.Hole singleHole = HoleUtils.getSingleHole(position, 1);
            if (singleHole != null) {
                holes.add(singleHole);
                continue;
            }

            if (doubleHoles.getValue()) {
                HoleUtils.Hole doubleHole = HoleUtils.getDoubleHole(position, 1);
                if (doubleHole != null) {
                    holes.add(doubleHole);
                    continue;
                }
            }

            if (quadHoles.getValue()) {
                HoleUtils.Hole quadHole = HoleUtils.getQuadHole(position, 1);
                if (quadHole != null) {
                    holes.add(quadHole);
                }
            }
        }

        return holes.stream().sorted(Comparator.comparing(h -> mc.player.squaredDistanceTo(h.box().getCenter().x, h.box().getCenter().y, h.box().getCenter().z))).toList();
    }
}
