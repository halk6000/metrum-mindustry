package metrum.ai.planner.task;
import arc.struct.Seq;
import arc.math.Angles;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.entities.units.BuildPlan;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import metrum.ai.planner.*;
import metrum.ai.planner.goal.StrategicPlanner;
public final class DefenceTask {
    public static int count(CoreBuild c){return BasePlanner.count(c,b->b.closestCore()==c&&b.block instanceof Turret);}
    public static boolean ready(CoreBuild c,int target){return BasePlanner.count(c,b->b.closestCore()==c&&b.enabled&&b instanceof ItemTurret.ItemTurretBuild&&((ItemTurret.ItemTurretBuild)b).totalAmmo>0&&BasePlanner.hasAmmoLine(b,(ItemTurret)b.block))>=target;}
    public static boolean emergency(CoreBuild c,AiLevel l){return BasePlanner.buildEmergencyTurret(c,l);}
    public static boolean turrets(CoreBuild c,AiLevel l,int target){
        if(BasePlanner.supplyTurretAmmo(c))return true;
        if(count(c)<target)return BasePlanner.buildTurret(c,l);
        return BasePlanner.stalled(StrategicPlanner.text("oczekiwanie na amunicję", "waiting for ammunition"));
    }
    private static boolean wall(Block b){return b==Blocks.berylliumWall||b==Blocks.berylliumWallLarge||b==Blocks.titaniumWall||b==Blocks.titaniumWallLarge;}
    private static int points(CoreBuild c){int[] p={0};Vars.indexer.eachBlock(c.team,c.x,c.y,40f*Vars.tilesize,b->b.closestCore()==c&&wall(b.block),b->p[0]+=b.block.size==1?3:5);return p[0];}
    public static boolean wallsReady(CoreBuild c){return points(c)>=60;}
    public static boolean walls(CoreBuild c){
        boolean large=points(c)%2==0; Block[] options=large?new Block[]{Blocks.berylliumWallLarge,Blocks.titaniumWallLarge,Blocks.berylliumWall,Blocks.titaniumWall}:new Block[]{Blocks.berylliumWall,Blocks.titaniumWall,Blocks.berylliumWallLarge,Blocks.titaniumWallLarge};
        float a=BasePlanner.wallGapAngle(c);
        for(Block b:options){if(!b.environmentBuildable()||b.isBanned())continue;
            for(int r=10;r<=20;r+=2)for(int d=-70;d<=70;d+=5){if(Math.abs(d)<12)continue;
                Tile t=Vars.world.tileWorld(c.x+Angles.trnsx(a+d,r*Vars.tilesize),c.y+Angles.trnsy(a+d,r*Vars.tilesize));
                if(t!=null&&BasePlanner.emptyFootprint(t,b)&&BasePlanner.defenseSiteAvailable(t,b,c.team))return BasePlanner.queueConstructionBatch(c,Seq.with(new BuildPlan(t.x,t.y,0,b)),true);
            }
        }return BasePlanner.stalled(StrategicPlanner.text("brak miejsca na mury","no wall site"));
    }
}
