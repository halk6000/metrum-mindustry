package metrum.ai.planner;

/** Follow explicit planner jobs without vanilla BuilderAI replacing the unit's queue. */
public class MetrumBuilderAI extends mindustry.ai.types.BuilderAI {
    public MetrumBuilderAI() { super(true, 600f); }
    @Override
    public void updateMovement() {
        if (unit.plans.isEmpty()) { super.updateMovement(); return; }
        mindustry.entities.units.BuildPlan plan = unit.buildPlan();
        unit.updateBuilding = true;
        moveTo(plan, unit.type.buildRange * 0.8f);
        unit.lookAt(plan);
    }
}