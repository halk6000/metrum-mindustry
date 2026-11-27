package metrum.ai.planner.task;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.entities.units.BuildPlan;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import metrum.ai.planner.BasePlanner;
import metrum.ai.planner.goal.StrategicPlanner;
public final class CoreUpgradeTask {
    public static int tier(CoreBuild c){return c.block==Blocks.coreAcropolis||c.block==Blocks.coreNucleus?3:c.block==Blocks.coreCitadel||c.block==Blocks.coreFoundation?2:1;}
    public static Block next(CoreBuild c){if(c.block==Blocks.coreBastion)return Blocks.coreCitadel;if(c.block==Blocks.coreCitadel)return Blocks.coreAcropolis;if(c.block==Blocks.coreShard)return Blocks.coreFoundation;if(c.block==Blocks.coreFoundation)return Blocks.coreNucleus;return null;}
    public static boolean step(CoreBuild c){Block b=next(c);if(b==null)return false;
        for(ItemStack r:b.requirements){int n=Math.round(r.amount*Vars.state.rules.buildCostMultiplier);if(n>c.storageCapacity)return BasePlanner.stalled(StrategicPlanner.text("za mała pojemność rdzenia: ","core capacity too small: ")+n);if(c.items.get(r.item)<n){BasePlanner.requestMaterial(c,r.item,n);return BasePlanner.stalled(StrategicPlanner.text("zbieranie: ","collecting: ")+r.item.localizedName);}}
        if(!Build.validPlace(b,c.team,c.tileX(),c.tileY(),0,false))return BasePlanner.stalled(StrategicPlanner.text("ulepszenie rdzenia: zablokowane miejsce lub teren","core upgrade: blocked site or terrain"));
        return BasePlanner.queueConstructionBatch(c,Seq.with(new BuildPlan(c.tileX(),c.tileY(),0,b)));
    }
}
