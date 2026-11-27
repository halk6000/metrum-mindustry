package metrum.ai.planner.task;

import arc.Events;
import arc.func.Prov;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.blocks.storage.StorageBlock.StorageBuild;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;
import java.util.*;

/** A saved, per-enemy-team capacity bonus. Never changes the shared itemCapacity of a block. */
public final class CoreCapacityTask {
    private static String key(Team t){return "metrum-ai-capacity-extra-"+t.id;}
    private static int extra(Team t){try{return Math.max(0,Integer.parseInt(Vars.state.rules.tags.get(key(t),"0")));}catch(NumberFormatException e){return 0;}}
    private static boolean eligible(Team t){return Vars.state!=null&&Vars.state.rules!=null&&!Vars.state.rules.pvp&&t==Vars.state.rules.waveTeam&&t!=Vars.state.rules.defaultTeam;}
    public static void install(){
        for(Block b:new Block[]{Blocks.coreBastion,Blocks.coreCitadel,Blocks.coreAcropolis,Blocks.coreShard,Blocks.coreFoundation,Blocks.coreNucleus}){
            if(!(b instanceof CoreBlock))continue;
            CoreBlock type=(CoreBlock)b;
            Prov<Building> old=type.buildType;
            // Do not replace another mod's specialised building class.
            if(old==null||old.get().getClass()!=CoreBuild.class)continue;
            type.buildType=()->new CapacityCoreBuild(type);
        }
        Events.on(WorldLoadEvent.class,e->{if(!Vars.net.client())apply(Vars.state.rules.waveTeam);});
    }
    public static void apply(Team t){
        if(!eligible(t)||extra(t)==0)return;
        long base=0;Set<Building> seen=Collections.newSetFromMap(new IdentityHashMap<Building,Boolean>());
        for(CoreBuild c:t.cores()){
            base+=c.block.itemCapacity;
            for(Building b:c.proximity)if(b instanceof StorageBuild&&((StorageBuild)b).linkedCore==c&&seen.add(b))base+=b.block.itemCapacity;
        }
        int cap=(int)Math.min(Integer.MAX_VALUE/4,base+extra(t));for(CoreBuild c:t.cores())c.storageCapacity=cap;
    }
    public static void drop(Team t){
        if(!eligible(t)||Vars.net.client()||t.core()==null)return;
        apply(t);int increase=(int)Math.ceil(t.core().storageCapacity*.2d);
        Vars.state.rules.tags.put(key(t),Long.toString(Math.min(Integer.MAX_VALUE/4,(long)extra(t)+increase)));apply(t);
    }
    /** Vanilla recalculates and clamps inventory on each proximity change. Preserve legitimate stock in the bonus range. */
    public static class CapacityCoreBuild extends CoreBuild {
        public CapacityCoreBuild(CoreBlock type){type.super();}
        @Override public void onProximityUpdate(){
            if(Vars.net.client()||!eligible(team)||extra(team)==0||Vars.world.isGenerating()){
                super.onProximityUpdate();if(eligible(team))apply(team);return;
            }
            // Match the vanilla core network selection before linking adjacent stores.
            ItemModule network=items;
            for(CoreBuild other:Vars.state.teams.cores(team))if(other.tile!=tile)network=other.items;
            ItemModule before=network==null?null:network.copy();
            // Include each newly joining storage module only once; never restore detached stock.
            if(before!=null){
                Set<ItemModule> seen=Collections.newSetFromMap(new IdentityHashMap<ItemModule,Boolean>());seen.add(network);
                for(Building b:proximity)if(owns(this,b)&&b.items!=null&&seen.add(b.items))before.add(b.items);
            }
            super.onProximityUpdate();apply(team);
            if(before!=null&&items==network){
                for(Item i:Vars.content.items())items.set(i,Math.max(items.get(i),Math.min(before.get(i),storageCapacity)));
            }
        }
    }
}
