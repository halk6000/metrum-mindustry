package metrum.ai;

import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.BuilderAI;
import mindustry.content.Fx;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;

/**
 * "Take care" command: rebuilds destroyed blocks (like the rebuild command) and, when nothing
 * needs rebuilding, flies to the most damaged friendly block nearby and repairs it.
 */
public class CaretakerAI extends AIController {
    public static UnitCommand command;
    /** HP per second restored by a caretaker, scaled by the unit's build speed */
    public static float repairPerSecond = 40f;

    final BuilderAI builder = new BuilderAI(true, 400f);
    Building patient;
    float retarget;

    public static void init(mindustry.type.UnitType... types) {
        command = new UnitCommand("metrum-caretaker", "hammer", u -> new CaretakerAI());
        for (mindustry.type.UnitType t : types) if (!t.commands.contains(command)) t.commands.add(command);
    }

    @Override
    public void updateMovement() {
        boolean rebuildWork = unit.canBuild() && !unit.team.data().plans.isEmpty();
        if (rebuildWork || unit.activelyBuilding()) {
            builder.unit(unit);
            builder.updateUnit();
            return;
        }
        if ((retarget -= Time.delta) <= 0f || patient == null || !patient.isValid() || !patient.damaged()) {
            retarget = 60f;
            patient = Vars.indexer.getDamaged(unit.team).min(b -> b.dst(unit));
        }
        if (patient == null) return;
        moveTo(patient, Vars.tilesize * 3f);
        unit.lookAt(patient);
        if (unit.within(patient, Vars.tilesize * 5f)) {
            patient.heal(repairPerSecond / 60f * Math.max(unit.type.buildSpeed, 0.5f) * Time.delta);
            if (arc.math.Mathf.chance(0.05 * Time.delta)) Fx.healBlockFull.at(patient.x, patient.y, patient.block.size, mindustry.graphics.Pal.heal, patient.block);
        }
    }
}
