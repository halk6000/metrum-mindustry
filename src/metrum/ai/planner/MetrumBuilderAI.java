package metrum.ai.planner;

/** Follow explicit planner jobs without vanilla BuilderAI replacing the unit's queue. */
public class MetrumBuilderAI extends mindustry.ai.types.BuilderAI {
    private final metrum.ai.SmartMining.SmartMinerAI idleMiner = new metrum.ai.SmartMining.SmartMinerAI() {
        @Override
        protected mindustry.type.Item desired() {
            mindustry.world.blocks.storage.CoreBlock.CoreBuild home = unit.team.cores().find(
                core -> BasePlanner.builderHomeFlag(core) == unit.flag);
            mindustry.type.ItemStack demand = home == null ? null : BasePlanner.miningDemand.get(home);
            if (demand != null && unit.canMine(demand.item)
                && mindustry.Vars.indexer.findClosestOre(unit, demand.item) != null) return demand.item;
            return super.desired();
        }
    };
    public MetrumBuilderAI() { super(true, 600f); }
    @Override
    public void updateMovement() {
        if (unit.plans.isEmpty()) {
            idleMiner.unit(unit);
            idleMiner.updateMovement(); // normal mining, carrying capacity and delivery to the core
            return;
        }
        unit.mineTile = null; // explicit construction always preempts mining
        idleMiner.ore = null;
        idleMiner.targetItem = null;
        idleMiner.mining = true;
        mindustry.entities.units.BuildPlan plan = unit.buildPlan();
        // A queued cleanup job may become unsafe if the network changes before the drone arrives.
        mindustry.world.Tile tile = mindustry.Vars.world.tile(plan.x, plan.y);
        if (!plan.breaking) {
            boolean inProgress = tile != null && tile.build instanceof mindustry.world.blocks.ConstructBlock.ConstructBuild
                && tile.build.team == unit.team
                && ((mindustry.world.blocks.ConstructBlock.ConstructBuild) tile.build).current == plan.block;
            boolean completed = tile != null && tile.build != null && tile.build.team == unit.team
                && tile.block() == plan.block && tile.build.tileX() == plan.x && tile.build.tileY() == plan.y
                && (!plan.block.rotate || tile.build.rotation == plan.rotation);
            if (completed) { unit.plans.removeFirst(); return; }
            if (tile == null || plan.block == null || (!inProgress
                && !mindustry.world.Build.validPlace(plan.block, unit.team, plan.x, plan.y, plan.rotation, false))) {
                // The rest of a route may depend on this site: cancel the batch and let the planner retry.
                unit.plans.clear();
                BasePlanner.plannerAction = "przeplanowanie: miejsce budowy niedostępne";
                return;
            }
        }
        if (plan.breaking && tile != null && tile.block() == mindustry.content.Blocks.reinforcedConduit) {
            if (tile.build == null || !BasePlanner.unusedWaterPipes(arc.struct.Seq.with(tile.build), unit.team).contains(tile.build)) {
                unit.plans.removeFirst();
                return;
            }
        }
        if (plan.breaking && tile != null && tile.block() == mindustry.content.Blocks.tungstenWall) {
            mindustry.world.blocks.storage.CoreBlock.CoreBuild home = unit.team.cores().find(
                core -> BasePlanner.builderHomeFlag(core) == unit.flag);
            if (home == null || !BasePlanner.wallNeedsOpening(home, tile)) {
                unit.plans.removeFirst();
                return;
            }
        }
        unit.updateBuilding = true;
        moveTo(plan, unit.type.buildRange * 0.8f);
        unit.lookAt(plan);
    }
}