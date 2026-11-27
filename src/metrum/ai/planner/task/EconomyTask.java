package metrum.ai.planner.task;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import metrum.ai.planner.*;
import metrum.ai.planner.goal.StrategicPlanner;

/** Extraction, electricity, silicon and rate-based improvements. */
public final class EconomyTask {
    private EconomyTask() {}
    private static Seq<Building> drills(CoreBuild core) {
        Seq<Building> out = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.closestCore() == core && b.enabled && (b instanceof Drill.DrillBuild || b instanceof BeamDrill.BeamDrillBuild), out::add);
        return out;
    }
    private static Item output(Building b) {
        if (b instanceof Drill.DrillBuild) return ((Drill.DrillBuild)b).dominantItem;
        return b instanceof BeamDrill.BeamDrillBuild ? ((BeamDrill.BeamDrillBuild)b).lastItem : null;
    }
    private static boolean powered(Building b) {
        return b.block.consPower == null || (b.power != null && b.power.status >= 0.95f);
    }
    private static boolean connected(CoreBuild core, Building b) {
        Item item = output(b);
        return item != null && !metrum.ai.SmartMining.burnsItem(core, item) && powered(b)
            && LogisticsPlanner.producerFeeds(b, core, item);
    }
    public static boolean infrastructureReady(CoreBuild core) {
        int supplied = 0;
        for (Building b : drills(core)) if (connected(core, b)) supplied++;
        return supplied >= 2 && PowerTask.generating(core) && BasePlanner.powerGridDone(core);
    }
    public static boolean infrastructure(CoreBuild core, AiLevel level) {
        if ((!BasePlanner.hasGenerator(core) || (BasePlanner.hasPowerDeficit(core) && !BasePlanner.hasIdleGenerator(core))) && BasePlanner.buildPower(core)) return true;
        if (!BasePlanner.powerGridDone(core) && BasePlanner.extendPowerGrid(core)) return true;
        if (BasePlanner.pipeWater(core) || BasePlanner.buildGeothermalForWater(core) || BasePlanner.pipeSteam(core)) return true;
        for (Building b : drills(core)) {
            Item item = output(b);
            if (item != null && !metrum.ai.SmartMining.burnsItem(core,item) && !LogisticsPlanner.producerFeeds(b,core,item)
                && LogisticsPlanner.connect(core,core,item)) return true;
        }
        if (BasePlanner.hasDemand(core)) return BasePlanner.recoverEconomy(core, level);
        if (BasePlanner.buildDrillLine(core,level)) return true;
        return BasePlanner.stalled(StrategicPlanner.text("brak sprawnego wydobycia lub zasilania", "missing working extraction or power"));
    }
    private static Building siliconFactory(CoreBuild core) {
        return Vars.indexer.findTile(core.team,core.x,core.y,40f*Vars.tilesize,
            b -> b.enabled && b.closestCore() == core && b.items != null && BasePlanner.producesItem(b.block,Items.silicon));
    }
    public static void observe(CoreBuild core) {
        // A running factory and a real delivery to the core are stronger evidence than a placed block.
        Building b = siliconFactory(core);
        if (b instanceof GenericCrafter.GenericCrafterBuild && b.efficiency > 0.01f
            && (LogisticsPlanner.producerFeeds(b,core,Items.silicon) || core.proximity.contains(b))) {
            if (b.items.get(Items.silicon) > 0 || core.items.get(Items.silicon) >= 150)
                StrategicPlanner.flag(core,"silicon-produced",true);
        }
    }
    public static boolean siliconReady(CoreBuild core) {
        return siliconFactory(core) != null && StrategicPlanner.flag(core,"silicon-produced")
            && core.items.get(Items.silicon) >= 150;
    }
    public static boolean silicon(CoreBuild core, AiLevel level) {
        if (!BasePlanner.hasDemand(core)) {
            // Even preloaded maps must establish a factory; a stockpile alone is not a production chain.
            if (core.items.get(Items.silicon) >= 150 && siliconFactory(core) == null) {
                if (BasePlanner.queueCoreWorkshop(core,mindustry.content.Blocks.siliconArcFurnace)) return true;
                return BasePlanner.stalled(StrategicPlanner.text("brak miejsca na piec krzemowy", "no silicon furnace site"));
            }
            if (core.items.get(Items.silicon) < 150) BasePlanner.requestMaterial(core,Items.silicon,150);
            else {
                Building b = siliconFactory(core);
                if (b != null) {
                    for (Item item : new Item[]{Items.sand,Items.graphite})
                        if (BasePlanner.requiredPerBatch(b,item) > b.items.get(item) && LogisticsPlanner.connect(core,b,item)) return true;
                    if (!powered(b) && BasePlanner.extendPowerGrid(core)) return true;
                }
                return BasePlanner.stalled(StrategicPlanner.text("oczekiwanie na rzeczywistą produkcję krzemu", "waiting for real silicon production"));
            }
        }
        return BasePlanner.recoverEconomy(core,level);
    }
    public static boolean optimized(CoreBuild core) {
        if (!infrastructureReady(core)) return false;
        Building silicon = siliconFactory(core);
        if (silicon == null) return false;
        ProductionPlanner.Rates sand = ProductionPlanner.inspect(core,Items.sand);
        // If a line is starved, recover its supply/route instead of buying another silicon furnace.
        return sand.demand <= 0.001f || (!sand.disconnected && !sand.shortage() && sand.active + 0.05f >= sand.demand)
            || (silicon.items.get(Items.sand) >= BasePlanner.requiredPerBatch(silicon,Items.sand)*2 && sand.nominal >= sand.demand);
    }
    public static boolean optimize(CoreBuild core, AiLevel level) {
        if (!infrastructureReady(core)) return infrastructure(core,level);
        Building silicon = siliconFactory(core);
        if (silicon == null) return silicon(core,level);
        if (LogisticsPlanner.connect(core,silicon,Items.sand)) return true;
        if (LogisticsPlanner.connect(core,silicon,Items.graphite)) return true;
        ProductionPlanner.Rates sand = ProductionPlanner.inspect(core,Items.sand);
        if (sand.demand > 0.001f && (sand.shortage() || sand.disconnected || sand.active + 0.05f < sand.demand)) {
            if (BasePlanner.ensureProductionInput(core,silicon,Items.sand)) return true;
            if (!BasePlanner.hasDemand(core)) BasePlanner.requestMaterial(core,Items.sand,Math.max(1,BasePlanner.requiredPerBatch(silicon,Items.sand)));
            if (BasePlanner.hasDemand(core)) return BasePlanner.recoverEconomy(core,level);
        }
        return BasePlanner.stalled(StrategicPlanner.text("sprawdzanie wydajności i dostaw", "checking throughput and supplies"));
    }
}
