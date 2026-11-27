package metrum.ai.planner;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumeItems;

/** Rates are items per simulation second. Capacity is distinct from current activity. */
public final class ProductionPlanner {
    private ProductionPlanner() {}
    public static final float reserve = 1.2f;
    private static final float epsilon = 0.001f;

    static float inputRate(Building b, Item item) {
        if (b instanceof mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild) {
            mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild f=(mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild)b;
            mindustry.world.blocks.units.UnitFactory t=(mindustry.world.blocks.units.UnitFactory)b.block;
            if (f.currentPlan<0 || f.currentPlan>=t.plans.size) return 0f;
            float time=t.plans.get(f.currentPlan).time;
            return time<=0 ? 0f : BasePlanner.requiredPerBatch(b,item)*60f/time*b.timeScale()*Vars.state.rules.unitBuildSpeed(b.team);
        }
        if (!(b.block instanceof GenericCrafter)) return 0f;
        float cycle = ((GenericCrafter)b.block).craftTime;
        if (cycle <= 0f) return 0f;
        int amount = 0;
        for (Consume c : b.block.nonOptionalConsumers) {
            if (c instanceof ConsumeItems) for (ItemStack s : ((ConsumeItems)c).items)
                if (s.item == item) amount += s.amount;
        }
        return amount * 60f / cycle * b.timeScale();
    }

    static float capacity(Building b, Item item) {
        if (!b.enabled) return 0f;
        if (b.block instanceof GenericCrafter) {
            GenericCrafter c = (GenericCrafter)b.block;
            if (c.craftTime <= 0f) return 0f;
            int amount = 0;
            if (c.outputItems != null) {
                for (ItemStack s : c.outputItems) if (s.item == item) amount += s.amount;
            } else if (c.outputItem != null && c.outputItem.item == item) amount = c.outputItem.amount;
            return amount * 60f / c.craftTime * b.timeScale();
        }
        if (b.block instanceof WallCrafter) {
            WallCrafter c = (WallCrafter)b.block;
            if (c.output != item || c.drillTime <= 0f) return 0f;
            int x = b.tileX() - (c.size - 1)/2, y = b.tileY() - (c.size - 1)/2;
            float wall = 0f;
            for (int i=0; i<c.size; i++) {
                int rx=x, ry=y;
                switch (b.rotation) {
                    case 0: rx+=c.size; ry+=i; break;
                    case 1: rx+=i; ry+=c.size; break;
                    case 2: rx--; ry+=i; break;
                    case 3: rx+=i; ry--; break;
                    default: continue;
                }
                Tile t=Vars.world.tile(rx,ry);
                if(t!=null && t.solid()) wall+=t.block().attributes.get(c.attribute);
            }
            return Math.max(0f,wall)*60f/c.drillTime*b.timeScale();
        }
        return 0f; // unknown recipes/extractors are not used to justify expansion
    }

    public static final class Rates {
        public float demand, installed, nominal, active;
        public boolean unknown, disconnected;
        // Do not buy another machine just because a sufficient installed source has no route.
        public boolean sufficient() { return demand > epsilon && installed + epsilon >= demand; }
        public float target() { return demand * reserve; }
        public boolean shortage() { return demand > epsilon && installed + epsilon < demand && !unknown && !disconnected; }
    }

    public static Rates inspect(CoreBuild core, Item item) {
        Rates r=new Rates();
        Seq<Building> consumers=new Seq<>(), producers=new Seq<>();
        Vars.indexer.eachBlock(core.team,core.x,core.y,40f*Vars.tilesize,
            b -> b.enabled && b.items!=null && b.closestCore()==core, b -> {
                float rate=inputRate(b,item);
                if(rate>epsilon){ consumers.add(b); r.demand+=rate; }
                if(BasePlanner.producesItem(b.block,item) || LogisticsPlanner.extracts(b,item)) producers.add(b);
            });
        for(Building p:producers){
            float rate=capacity(p,item);
            if(rate<=epsilon){r.unknown=true; continue;}
            r.installed+=rate;
            boolean linked=consumers.contains(c -> LogisticsPlanner.producerFeeds(p,c,item));
            // Core-adjacent workshops may receive storable ingredients through the existing supply system.
            if(!metrum.ai.SmartMining.burnsItem(core,item) && LogisticsPlanner.producerFeeds(p,core,item)
                && consumers.contains(c -> core.proximity.contains(c))) linked=true;
            if(!linked){r.disconnected=true; continue;}
            r.nominal+=rate;
            if(p instanceof WallCrafter.WallCrafterBuild){
                WallCrafter c=(WallCrafter)p.block;
                r.active+=Math.max(0f,((WallCrafter.WallCrafterBuild)p).lastEfficiency)*60f/c.drillTime;
            }else r.active+=rate*Math.max(0f,p.efficiency);
        }
        for(Building c:consumers){
            if(!LogisticsPlanner.hasRoute(c,item) && (metrum.ai.SmartMining.burnsItem(core,item) || !core.proximity.contains(c)))
                r.disconnected=true;
        }
        return r;
    }
}
