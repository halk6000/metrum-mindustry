package metrum.ai.planner;

/** Follow explicit planner jobs without vanilla BuilderAI replacing the unit's queue. */
public class MetrumBuilderAI extends mindustry.ai.types.BuilderAI {
    public MetrumBuilderAI() { super(true, 600f); }
    @Override
    public void updateMovement() {
        if (unit.plans.isEmpty()) { super.updateMovement(); return; }
        mindustry.entities.units.BuildPlan plan = unit.buildPlan();
        // A queued cleanup job may become unsafe if the network changes before the drone arrives.
        mindustry.world.Tile tile = mindustry.Vars.world.tile(plan.x, plan.y);
        if (plan.breaking && tile != null && tile.block() == mindustry.content.Blocks.reinforcedConduit) {
            if (tile.build == null || !BasePlanner.unusedWaterPipes(arc.struct.Seq.with(tile.build), unit.team).contains(tile.build)) {
                unit.plans.removeFirst();
                return;
            }
        }
        unit.updateBuilding = true;
        moveTo(plan, unit.type.buildRange * 0.8f);
        unit.lookAt(plan);
    }
}