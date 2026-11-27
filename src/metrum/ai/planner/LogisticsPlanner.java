package metrum.ai.planner;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Duct.DuctBuild;
import mindustry.world.blocks.distribution.DuctRouter.DuctRouterBuild;
import mindustry.world.blocks.distribution.DirectionalUnloader.DirectionalUnloaderBuild;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.production.Drill.DrillBuild;
import mindustry.world.blocks.production.BeamDrill.BeamDrillBuild;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import java.util.*;

/** Item logistics. Plans physical connections, never transfers inventory itself. */
public final class LogisticsPlanner {
    private LogisticsPlanner() {}
    private static final int radius = 24, maxTrace = 128;

    private static Building at(Tile t) { return t == null ? null : t.build; }
    private static boolean transport(Building b) {
        return b instanceof DuctBuild || b instanceof DuctBridgeBuild || b instanceof DuctRouterBuild || b instanceof DirectionalUnloaderBuild;
    }
    private static boolean sends(Building a, Building b, Item item) {
        if (a == null || b == null || a.team != b.team || !a.enabled || a == b) return false;
        if (b instanceof DuctRouterBuild && at(b.tile.nearby((b.rotation+2)%4)) != a) return false;
        if (b instanceof DirectionalUnloaderBuild && at(b.tile.nearby((b.rotation+2)%4)) != a) return false;
        if (a instanceof DirectionalUnloaderBuild) return ((DirectionalUnloaderBuild)a).unloadItem == item && at(a.tile.nearby(a.rotation)) == b;
        if (a instanceof DuctBuild) return ((DuctBuild)a).next == b;
        if (a instanceof DuctBridgeBuild) {
            Building link = ((DuctBridgeBuild)a).findLink();
            return link == b || (link == null && at(a.tile.nearby(a.rotation)) == b);
        }
        if (a instanceof DuctRouterBuild) {
            int d = a.tile.relativeTo(b.tile);
            Item filter = ((DuctRouterBuild)a).sortItem;
            return d >= 0 && d != (a.rotation + 2)%4 && (filter == null || (item == filter) == (d == a.rotation));
        }
        if (a instanceof DrillBuild) return ((DrillBuild)a).dominantItem == item;
        if (a instanceof BeamDrillBuild) return ((BeamDrillBuild)a).lastItem == item;
        if (a.block instanceof WallCrafter) return ((WallCrafter)a.block).output == item;
        return BasePlanner.producesItem(a.block, item);
    }
    private static boolean source(Building b, Item item, Set<Building> seen) {
        if (b == null || !b.enabled || seen.size() >= maxTrace || !seen.add(b)) return false;
        if (b instanceof DrillBuild) return ((DrillBuild)b).dominantItem == item;
        if (b instanceof BeamDrillBuild) return ((BeamDrillBuild)b).lastItem == item;
        if (b.block instanceof WallCrafter) return ((WallCrafter)b.block).output == item;
        if (!transport(b)) return BasePlanner.producesItem(b.block, item);
        for (Building a : b.proximity) if (sends(a,b,item) && source(a,item,seen)) return true;
        if (b instanceof DuctBridgeBuild) {
            int range = ((DuctBridge)b.block).range;
            for (int d=0; d<4; d++) for(int n=1;n<=range;n++) {
                Building a = at(b.tile.nearby(arc.math.geom.Geometry.d4x(d)*n,arc.math.geom.Geometry.d4y(d)*n));
                if (a instanceof DuctBridgeBuild && sends(a,b,item) && source(a,item,seen)) return true;
            }
        }
        return false;
    }
    private static boolean feeds(Building b, Building target, Item item, Set<Building> seen) {
        if (b == target) return true;
        if (!transport(b) || !b.enabled || seen.size() >= maxTrace || !seen.add(b)) return false;
        if (b instanceof DuctBridgeBuild) {
            Building link = ((DuctBridgeBuild)b).findLink();
            if (link != null) return feeds(link,target,item,seen);
        }
        for (Building next : b.proximity) if (sends(b,next,item) && feeds(next,target,item,seen)) return true;
        return false;
    }
    private static boolean free(CoreBuild core, Tile t, Block block, int rotation) {
        return t != null && t.build == null && t.block() == Blocks.air && BasePlanner.transportSiteClear(core.team,block,t)
            && Build.validPlace(block,core.team,t.x,t.y,rotation,false);
    }
    /** Search a new route without touching live transport; bounded breadth-first search. */
    private static Seq<BuildPlan> walk(CoreBuild core, Tile start, Building target, Set<Tile> excluded) {
        if (start == null || excluded.contains(start)) return null;
        if (start.build == target) return new Seq<>();
        if (!free(core,start,Blocks.duct,0)) return null;
        ArrayDeque<Tile> q = new ArrayDeque<>();
        Map<Tile,Tile> prev = new HashMap<>();
        q.add(start); prev.put(start,null);
        while (!q.isEmpty()) {
            Tile t=q.remove();
            for(int d=0;d<4;d++) {
                Tile n=t.nearby(d);
                if(n==null || excluded.contains(n) || prev.containsKey(n)) continue;
                if (!free(core,t,Blocks.duct,d)) continue;
                if(n.build==target) {
                    Seq<BuildPlan> out=new Seq<>();
                    for(Tile a=t,b=n;a!=null;b=a,a=prev.get(a)) out.add(new BuildPlan(a.x,a.y,a.relativeTo(b),Blocks.duct));
                    return out; // consumer first, source last
                }
                if(!start.within(n,radius*Vars.tilesize) || !free(core,n,Blocks.duct,0)) continue;
                prev.put(n,t); q.add(n);
            }
        }
        return null;
    }
    private static Seq<BuildPlan> route(CoreBuild core, Tile start, Building target, Tile excluded) {
        Set<Tile> blocked = new HashSet<>();
        if(excluded!=null) blocked.add(excluded);
        return route(core,start,target,blocked);
    }
    private static Seq<BuildPlan> route(CoreBuild core, Tile start, Building target, Set<Tile> blocked) {
        Seq<BuildPlan> ordinary=walk(core,start,target,blocked);
        if(ordinary!=null) return ordinary;
        if(start==null || blocked.contains(start) || !(Blocks.ductBridge instanceof DuctBridge)
            || !Blocks.ductBridge.environmentBuildable()) return null;
        DuctBridge bridge=(DuctBridge)Blocks.ductBridge;
        // Conservative fallback: jump directly from the branch outlet, across an obstacle.
        for(int d=0;d<4;d++) {
            if(!free(core,start,bridge,d) || blocked.contains(start.nearby(d)) || !isolatedBridge(start,bridge)) continue;
            boolean obstacle=false;
            for(int n=1;n<=bridge.range;n++) {
                Tile end=start.nearby(arc.math.geom.Geometry.d4x(d)*n,arc.math.geom.Geometry.d4y(d)*n);
                if(end==null) break;
                if(!free(core,end,bridge,d) || blocked.contains(end)) { obstacle=true; continue; }
                if(!obstacle || !isolatedBridge(end,bridge)) continue;
                Set<Tile> avoid=new HashSet<>(blocked); avoid.add(start); avoid.add(end);
                Seq<BuildPlan> tail=walk(core,end.nearby(d),target,avoid);
                if(tail==null) continue;
                tail.add(new BuildPlan(end.x,end.y,d,bridge));
                tail.add(new BuildPlan(start.x,start.y,d,bridge));
                return tail;
            }
        }
        return null;
    }
    private static boolean isolatedBridge(Tile t,DuctBridge bridge) {
        for(int d=0;d<4;d++) for(int n=1;n<=bridge.range;n++) {
            Building b=at(t.nearby(arc.math.geom.Geometry.d4x(d)*n,arc.math.geom.Geometry.d4y(d)*n));
            if(b instanceof DuctBridgeBuild) return false;
        }
        return true;
    }
    /** New extractor routes use the same search, including bridge fallback. */
    static Seq<BuildPlan> extractor(CoreBuild core,Block block,Tile site,int rotation,Building target) {
        Set<Tile> footprint=new HashSet<>();
        int min=-(block.size-1)/2,max=block.size/2;
        for(int x=min;x<=max;x++) for(int y=min;y<=max;y++) {
            Tile t=site.nearby(x,y); if(t!=null) footprint.add(t);
        }
        Seq<BuildPlan> best=null;
        for(Tile t:footprint) for(int d=0;d<4;d++) {
            Tile outlet=t.nearby(d);
            if(outlet==null || footprint.contains(outlet)) continue;
            Seq<BuildPlan> plans=route(core,outlet,target,footprint);
            if(plans!=null && (best==null || plans.size<best.size)) best=plans;
        }
        if(best!=null) best.add(new BuildPlan(site.x,site.y,rotation,block));
        return best;
    }
    private static boolean affordable(CoreBuild core, Seq<BuildPlan> plans) {
        for(Map.Entry<Item,Integer> cost: BasePlanner.planMaterials(plans).entrySet())
            if(core.items.get(cost.getKey())-BasePlanner.constructionReserve(core).getOrDefault(cost.getKey(),0)<cost.getValue()) return false;
        return true;
    }
    private static Seq<BuildPlan> bestBranch(CoreBuild core, Building target, Item item, Seq<Building> lines) {
        Seq<BuildPlan> best=null;
        for(Building b:lines) {
            if(!(b instanceof DuctBuild) || b.block!=Blocks.duct || !source(b,item,new HashSet<Building>())) continue;
            DuctBuild duct=(DuctBuild)b;
            if(duct.next==null || duct.next==target) continue;
            // A router accepts from exactly one direction. Reject ambiguous input geometry.
            int incoming=-1; boolean ambiguous=false;
            for(Building a:b.proximity) {
                if(a==duct.next || a.team!=core.team || !a.block.outputsItems()) continue;
                boolean contributes=false;
                for(Item i:Vars.content.items()) if(sends(a,b,i) && source(a,i,new HashSet<Building>())) { contributes=true; break; }
                if(!contributes) continue;
                if(!(a instanceof DuctBuild) || ((DuctBuild)a).next!=b || incoming!=-1) { ambiguous=true; break; }
                incoming=a.rotation;
            }
            if(ambiguous || incoming<0 || duct.rotation==(incoming+2)%4) continue;
            if(!Build.validPlace(Blocks.ductRouter,core.team,b.tileX(),b.tileY(),incoming,false)) continue;
            Set<Item> materials=new HashSet<>();
            for(Item i:Vars.content.items()) if(source(b,i,new HashSet<Building>()) || b.items.has(i)) materials.add(i);
            boolean single=materials.size()==1;
            for(int d=0;d<4;d++) {
                if(d==duct.rotation || d==(incoming+2)%4) continue;
                Item filter=null;
                if(!single) {
                    if(d==incoming && duct.rotation!=incoming) filter=item;
                    else if(duct.rotation==incoming && materials.size()==2) {
                        for(Item i:materials) if(i!=item) filter=i;
                    } else continue;
                }
                // No accidental third output into another inventory.
                boolean safe=true;
                for(int side=0;side<4;side++) if(side!=d && side!=duct.rotation && side!=(incoming+2)%4) {
                    Building extra=at(b.tile.nearby(side));
                    if(extra!=null && extra.block.hasItems) safe=false;
                }
                if(!safe) continue;
                Seq<BuildPlan> plans=route(core,b.tile.nearby(d),target,b.tile);
                if(plans==null) continue;
                plans.add(new BuildPlan(b.tileX(),b.tileY(),incoming,Blocks.ductRouter,filter));
                if(affordable(core,plans) && (best==null || plans.size<best.size)) best=plans;
            }
        }
        return best;
    }
    static boolean extracts(Building b, Item item) {
        return b instanceof DrillBuild && ((DrillBuild)b).dominantItem == item
            || b instanceof BeamDrillBuild && ((BeamDrillBuild)b).lastItem == item;
    }

    public static boolean producerFeeds(Building producer, Building target, Item item) {
        for (Building next : producer.proximity)
            if (sends(producer,next,item) && feeds(next,target,item,new HashSet<Building>())) return true;
        return false;
    }

    public static boolean hasRoute(Building target, Item item) {
        if (target == null) return false;
        for (Building b : target.proximity)
            if (sends(b, target, item) && source(b, item, new HashSet<Building>())) return true;
        return false;
    }

    public static boolean connect(CoreBuild core, Building target, Item item) {
        if(target==null || item==null || !target.enabled || (target.items==null && !(target instanceof mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild)) || !Blocks.ductRouter.environmentBuildable()) return false;
        for(Building b:target.proximity) if(sends(b,target,item) && source(b,item,new HashSet<Building>())) return false;
        Seq<Building> lines=new Seq<>();
        Vars.indexer.eachBlock(core.team,target.x,target.y,radius*Vars.tilesize,b->b.enabled && transport(b),lines::add);
        for(Building b:lines) if(source(b,item,new HashSet<Building>()) && feeds(b,target,item,new HashSet<Building>())) return false;
        // An existing wall extractor can get a new physical outlet without rebuilding it.
        Seq<Building> extractors = new Seq<>();
        Vars.indexer.eachBlock(core.team, target.x, target.y, radius * Vars.tilesize,
            b -> b.enabled && b != target && !transport(b) && source(b,item,new HashSet<Building>()),
            extractors::add);
        Seq<BuildPlan> direct = null;
        for (Building extractor : extractors) {
            int min = -(extractor.block.size - 1) / 2, max = extractor.block.size / 2;
            for (int x = min; x <= max; x++) for (int y = min; y <= max; y++) {
                if (x != min && x != max && y != min && y != max) continue;
                Tile edge = extractor.tile.nearby(x,y);
                if (edge == null) continue;
                for (int d = 0; d < 4; d++) {
                    Tile outlet = edge.nearby(d);
                    if (outlet == null || outlet.build != null) continue;
                    Seq<BuildPlan> candidate = route(core,outlet,target,(Tile)null);
                    if (candidate != null && !candidate.isEmpty() && affordable(core,candidate)
                        && (direct == null || candidate.size < direct.size)) direct = candidate;
                }
            }
        }
        if (direct != null && BasePlanner.queueConstructionBatch(core,direct)) return true;
        Seq<BuildPlan> best=bestBranch(core,target,item,lines);
        if(best!=null && BasePlanner.queueConstructionBatch(core,best)) return true;
        // Existing bridge provides a filtered side extraction when a router is unsafe.
        for(Building b:lines) {
            if(!(b instanceof metrum.world.FilteredDuctBridgeBuild) || !source(b,item,new HashSet<Building>())) continue;
            for(int d:new int[]{(b.rotation+1)%4,(b.rotation+3)%4}) {
                Tile slot=b.tile.nearby(d);
                if(!free(core,slot,Blocks.ductUnloader,d)) continue;
                Seq<BuildPlan> plans=route(core,slot.nearby(d),target,slot);
                if(plans==null) continue;
                plans.add(new BuildPlan(slot.x,slot.y,d,Blocks.ductUnloader,item));
                if(affordable(core,plans) && BasePlanner.queueConstructionBatch(core,plans)) return true;
            }
        }
        return false;
    }
}
