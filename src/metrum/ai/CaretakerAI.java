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

    private mindustry.gen.Building home;
    private mindustry.entities.units.UnitController previousController;
    public void delegate(mindustry.gen.Building core, mindustry.entities.units.UnitController previous) {
        home = core;
        previousController = previous;
    }
    public boolean delegatedTo(mindustry.gen.Building core) { return home == core; }
    public void releaseOrder() {
        if (home != null && unit.plans.isEmpty() && !unit.activelyBuilding()) unit.controller(previousController);
    }

    @Override
    public void updateUnit() {
        if (home != null && (!home.isValid() || home.team != unit.team)) {
            releaseOrder();
            if (unit.plans.isEmpty() && !unit.activelyBuilding()) return;
        }
        boolean rebuildWork = unit.canBuild() && !unit.team.data().plans.isEmpty();
        AIController ai = rebuildWork || unit.activelyBuilding() ? builder : repair;
        ai.unit(unit);
        ai.updateUnit();
    }
}
