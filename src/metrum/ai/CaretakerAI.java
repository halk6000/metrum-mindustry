package metrum.ai;

import mindustry.ai.UnitCommand;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.units.AIController;

/**
 * "Take care" command (T2/T3 lines, which carry healing guns): rebuilds destroyed blocks and,
 * when nothing needs rebuilding, repairs damaged blocks by shooting them with its healing guns.
 */
public class CaretakerAI extends AIController {
    public static UnitCommand command;

    final BuilderAI builder = new BuilderAI(true, 400f);

    public static void init(mindustry.type.UnitType... types) {
        command = new UnitCommand("metrum-caretaker", "hammer", u -> new CaretakerAI());
        for (mindustry.type.UnitType t : types) if (!t.commands.contains(command)) t.commands.add(command);
    }

    /** repairing is done only with the unit's own healing guns (vanilla repair behaviour) */
    final mindustry.ai.types.RepairAI repair = new mindustry.ai.types.RepairAI();

    @Override
    public void updateUnit() {
        boolean rebuildWork = unit.canBuild() && !unit.team.data().plans.isEmpty();
        AIController ai = rebuildWork || unit.activelyBuilding() ? builder : repair;
        ai.unit(unit);
        ai.updateUnit();
    }
}
