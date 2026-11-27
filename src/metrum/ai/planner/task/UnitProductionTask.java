package metrum.ai.planner.task;
import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.content.Blocks;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import metrum.content.MetrumUnits;
import metrum.content.MetrumUnitBlocks;
import metrum.ai.planner.*;
import metrum.ai.planner.goal.*;
public final class UnitProductionTask {
    private static final double pending=7346291d, waiting=7346292d, assault=7346293d, supportPending=7346294d;
    public static void init(){
        Events.on(UnitCreateEvent.class,e->{if(!Vars.net.client()&&BasePlanner.active()&&e.unit.team==Vars.state.rules.waveTeam&&e.spawner instanceof UnitFactoryBuild)e.unit.flag=(t1(e.unit.type)||(e.unit.type==MetrumUnits.monoMetT2&&repairNeeded(e.spawner.closestCore())))?supportPending:pending;});
        Events.on(UnitUnloadEvent.class,e->{if(Vars.net.client()||(e.unit.flag!=pending&&e.unit.flag!=supportPending)||e.unit.team!=Vars.state.rules.waveTeam)return;Unit u=e.unit;CoreBuild c=u.closestCore();if(c!=null)StrategicPlanner.flag(c,"unit-produced",true);u.flag=u.flag==pending&&combat(u.type)?waiting:0d;});
    }
    private static boolean t1(UnitType t){return t==MetrumUnits.monoMetT1||t==MetrumUnits.monoMetT1b||t==MetrumUnits.monoMetT1c||t==MetrumUnits.monoMetT1d;}
    private static boolean combat(UnitType t){return !t1(t)&&!t.weapons.isEmpty();}
    private static Seq<UnitFactoryBuild> factories(CoreBuild c){Seq<UnitFactoryBuild> f=new Seq<>();Vars.indexer.eachBlock(c.team,c.x,c.y,40f*Vars.tilesize,b->b instanceof UnitFactoryBuild&&b.enabled&&b.closestCore()==c,b->f.add((UnitFactoryBuild)b));return f;}
    public static boolean ready(CoreBuild c){return !factories(c).isEmpty()&&StrategicPlanner.flag(c,"unit-produced");}
    private static boolean repairNeeded(CoreBuild c){
        return c!=null&&(!c.team.data().plans.isEmpty()||BasePlanner.count(c,b->b.closestCore()==c&&b.health<b.maxHealth-1f)>0);
    }
    private static boolean minerNeeded(CoreBuild c){
        int miners=Groups.unit.count(u->u.team==c.team&&!u.dead()&&!u.isPlayer()&&u.flag==0d&&t1(u.type)&&u.closestCore()==c);
        Item demand=BasePlanner.currentDemand(c);
        return miners<2 || (miners<3&&demand!=null&&metrum.ai.SmartMining.canStoreMined(c,demand)
            &&MetrumUnits.monoMetT1.mineItems.contains(demand));
    }
    private static int plan(UnitFactory f,CoreBuild c){
        if(f==MetrumUnitBlocks.airMinerDock){
            int tier=CoreUpgradeTask.tier(c);
            if(!StrategicPlanner.flag(c,"unit-produced")||minerNeeded(c))return 0;
            if(tier>=2&&repairNeeded(c)&&!Groups.unit.contains(u->u.team==c.team&&!u.dead()&&u.flag==0d&&u.type==MetrumUnits.monoMetT2&&u.closestCore()==c))return 1;
            if(tier>=3)return 2;
            if(tier>=2)return 1;
            return -1;
        }
        for(int i=0;i<f.plans.size;i++)if(!f.plans.get(i).unit.isBanned()&&f.plans.get(i).unit.supportsEnv(Vars.state.rules.env)&&combat(f.plans.get(i).unit))return i;return -1;
    }
    public static void observe(CoreBuild c){
        if(StrategicPlanner.goal(c).ordinal()<DevelopmentGoal.UNIT_FACTORY.ordinal())return;
        // Supply only existing, core-adjacent Metrum dock by its established core inventory mechanism.
        for(UnitFactoryBuild f:factories(c)){
            UnitFactory type=(UnitFactory)f.block;
            int selected=plan(type,c);
            if(f.currentPlan!=selected&&f.progress<=0.01f&&f.payload==null)Call.tileConfig(null,f,selected);
            if(f.block!=MetrumUnitBlocks.airMinerDock||!c.proximity.contains(f)||f.currentPlan<0)continue;if(f.currentPlan>=type.plans.size)continue;
            java.util.Map<Item,Integer> reserve=BasePlanner.economicReserve(c);
            for(ItemStack r:type.plans.get(f.currentPlan).requirements){if(metrum.ai.SmartMining.burnsItem(c,r.item))continue;int n=Math.max(0,BasePlanner.requiredPerBatch(f,r.item)-f.items.get(r.item));n=Math.min(n,Math.max(0,c.items.get(r.item)-reserve.getOrDefault(r.item,0)));if(n>0){c.items.remove(r.item,n);f.items.add(r.item,n);}}
        }
    }
    public static boolean step(CoreBuild c,AiLevel l){
        Seq<UnitFactoryBuild> fs=factories(c);
        if(fs.isEmpty()){
            Seq<UnitFactory> choices=new Seq<>();for(Block b:new Block[]{MetrumUnitBlocks.airMinerDock,Blocks.tankFabricator,Blocks.shipFabricator,Blocks.mechFabricator,Blocks.groundFactory,Blocks.airFactory,Blocks.navalFactory})if(b instanceof UnitFactory&&b.environmentBuildable()&&!b.isBanned()&&plan((UnitFactory)b,c)>=0)choices.add((UnitFactory)b);
            if(choices.isEmpty())return BasePlanner.stalled(StrategicPlanner.text("brak dostępnej fabryki jednostek","no unit factory available"));
            String key="metrum-unit-factory-choice-"+c.team.id+"-"+c.pos();String saved=Vars.state.rules.tags.get(key,"");UnitFactory f=choices.find(x->x.name.equals(saved));if(f==null){f=choices.random();Vars.state.rules.tags.put(key,f.name);}return BasePlanner.queueUnitFactory(c,f,plan(f,c));
        }
        boolean dockExists=fs.contains(f->f.block==MetrumUnitBlocks.airMinerDock);
        if(!dockExists&&(minerNeeded(c)||(CoreUpgradeTask.tier(c)>=2&&repairNeeded(c)))&&MetrumUnitBlocks.airMinerDock instanceof UnitFactory){
            if(BasePlanner.queueUnitFactory(c,(UnitFactory)MetrumUnitBlocks.airMinerDock,0))return true;
            if(BasePlanner.hasDemand(c))return false;
        }
        if(dockExists&&CoreUpgradeTask.tier(c)==1&&StrategicPlanner.flag(c,"unit-produced")&&!minerNeeded(c)&&!fs.contains(f->f.block!=MetrumUnitBlocks.airMinerDock)){
            for(Block b:new Block[]{Blocks.tankFabricator,Blocks.shipFabricator,Blocks.mechFabricator,Blocks.groundFactory,Blocks.airFactory,Blocks.navalFactory}){
                if(b instanceof UnitFactory&&b.environmentBuildable()&&!b.isBanned()&&plan((UnitFactory)b,c)>=0){
                    if(BasePlanner.queueUnitFactory(c,(UnitFactory)b,plan((UnitFactory)b,c)))return true;
                    if(BasePlanner.hasDemand(c))return false;
                }
            }
        }
        for(UnitFactoryBuild f:fs){UnitFactory type=(UnitFactory)f.block;int p=plan(type,c);if(p<0){if(f.currentPlan!=-1&&f.progress<=0.01f&&f.payload==null){Call.tileConfig(null,f,-1);return true;}continue;}
            if(f.currentPlan!=p&&f.progress<=0.01f&&f.payload==null){Call.tileConfig(null,f,p);return true;}
            if(f.currentPlan<0||f.currentPlan>=type.plans.size)continue;
            for(ItemStack r:type.plans.get(f.currentPlan).requirements){if(f.items.get(r.item)>=BasePlanner.requiredPerBatch(f,r.item))continue;
                if(LogisticsPlanner.connect(c,f,r.item))return true;
                if(f.block==MetrumUnitBlocks.airMinerDock&&c.proximity.contains(f)&&c.items.get(r.item)>=BasePlanner.requiredPerBatch(f,r.item))continue;
                if(BasePlanner.ensureProductionInput(c,f,r.item))return true;
                if(BasePlanner.hasDemand(c))return BasePlanner.recoverEconomy(c,l);
            }
        }
        if(!BasePlanner.powerGridDone(c)&&BasePlanner.extendPowerGrid(c))return true;
        return BasePlanner.stalled(StrategicPlanner.text("oczekiwanie na produkcję jednostek","waiting for unit production"));
    }
    public static void orderSquads(CoreBuild c){
        Building enemy=Vars.state.rules.defaultTeam.core();if(enemy==null)return;
        Seq<Unit> pool=new Seq<>();for(Unit u:Groups.unit){if(u.team!=c.team||u.dead()||u.isPlayer()||u.closestCore()!=c||!combat(u.type)||!u.isCommandable())continue;if(u.flag==waiting){pool.add(u);CommandAI a=u.command();if(a.command!=UnitCommand.moveCommand)a.command(UnitCommand.moveCommand);if(a.targetPos==null||a.attackTarget!=null)a.commandPosition(new arc.math.geom.Vec2(c.x,c.y));}else if(u.flag==assault&&u.controller() instanceof CommandAI){CommandAI a=(CommandAI)u.controller();if(a.attackTarget==null)a.commandTarget(enemy);}}
        while(pool.size>=4){for(int i=0;i<4;i++){Unit u=pool.pop();u.flag=assault;CommandAI a=u.command();a.command(UnitCommand.moveCommand);a.commandTarget(enemy);}}
    }
}
