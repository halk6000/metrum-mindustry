package metrum.ai.planner;

/** Follow explicit planner jobs without vanilla BuilderAI replacing the unit's queue. */
public class MetrumBuilderAI extends mindustry.ai.types.BuilderAI {
    private boolean enteringIdle = true;
    private mindustry.type.Item constructionOre;
    private final metrum.ai.SmartMining.SmartMinerAI idleMiner = new metrum.ai.SmartMining.SmartMinerAI() {
        @Override
        public void updateMovement() {
            mindustry.gen.Building core = unit.closestCore();
            if (unit.stack.amount > 0 && core != null) {
                int accepted = core.acceptStack(unit.stack.item, unit.stack.amount, unit);
                if (mining && targetItem != null && core.acceptStack(targetItem, 1, unit) == 0) mining = false;
                // Handle partial deliveries here because vanilla MinerAI clears the entire stack.
                if (accepted < unit.stack.amount) {
                    mining = false;
                    unit.mineTile = null;
                    if (accepted > 0 && unit.within(core, unit.type.range)) {
                        mindustry.gen.Call.transferItemTo(unit, unit.stack.item, accepted, unit.x, unit.y, core);
                    }
                    moveTo(core, unit.type.range * 0.8f);
                    return;
                }
            }
            super.updateMovement();
        }
        @Override
        protected mindustry.type.Item desired() {
            if (constructionOre != null) return constructionOre;
            mindustry.world.blocks.storage.CoreBlock.CoreBuild home = unit.team.cores().find(
                core -> BasePlanner.builderHomeFlag(core) == unit.flag);
            mindustry.type.ItemStack demand = home == null ? null : BasePlanner.miningDemand.get(home);
            if (demand != null && unit.canMine(demand.item)
                && metrum.ai.SmartMining.canStoreMined(unit.closestCore(), demand.item)
                && mindustry.Vars.indexer.findClosestOre(unit, demand.item) != null) return demand.item;
            return super.desired();
        }
    };
    public MetrumBuilderAI() { super(true, 600f); }
    private void mineWhileIdle(boolean deliverCargo) {
        unit.updateBuilding = false;
        idleMiner.unit(unit);
        if (enteringIdle) {
            idleMiner.mining = unit.stack.amount == 0;
            enteringIdle = false;
        }
        if (deliverCargo) idleMiner.mining = false;
        idleMiner.updateMovement();
    }

    @Override
    public void updateMovement() {
        constructionOre = null;
        if (unit.plans.isEmpty()) {
            mineWhileIdle(false);
            return;
        }
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
            mindustry.world.blocks.storage.CoreBlock.CoreBuild core = unit.closestCore();
            if (!inProgress && core != null && !mindustry.Vars.state.rules.isInfiniteResources(unit.team)) {
                float multiplier = mindustry.Vars.state.rules.buildCostMultiplier;
                for (mindustry.type.ItemStack cost : plan.block.requirements) {
                    int missing = Math.round(cost.amount * multiplier) - core.items.get(cost.item);
                    if (missing <= 0 || !metrum.ai.SmartMining.canStoreMined(core, cost.item)) continue;
                    boolean deliverCargo = unit.stack.item == cost.item && unit.stack.amount >= missing;
                    if (!deliverCargo && (!unit.canMine(cost.item)
                        || mindustry.Vars.indexer.findClosestOre(unit, cost.item) == null)) continue;
                    constructionOre = cost.item;
                    mineWhileIdle(deliverCargo);
                    return;
                }
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
        enteringIdle = true;
        unit.mineTile = null;
        idleMiner.ore = null;
        idleMiner.targetItem = null;
        unit.updateBuilding = true;
        moveTo(plan, unit.type.buildRange * 0.8f);
        unit.lookAt(plan);
    }
}
