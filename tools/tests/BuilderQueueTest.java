package metrum.ai.planner;
import java.io.*;
import arc.struct.Queue;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.io.TypeIO;
import mindustry.type.UnitType;
import mindustry.world.Block;
public class BuilderQueueTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        Vars.content=new ContentLoader(); Vars.state=new mindustry.core.GameState();
        mindustry.content.Blocks.air=new mindustry.world.blocks.environment.Floor("air");
        Vars.world=new mindustry.core.World(); Vars.world.tiles=new mindustry.world.Tiles(8,8);
        for(int x=0;x<8;x++) for(int y=0;y<8;y++) Vars.world.tiles.set(x,y,new mindustry.world.Tile(x,y));
        Block drill=new Block("test-drill"), duct=new Block("test-duct"); drill.rotate=true; duct.rotate=true;
        Queue<BuildPlan> plans=new Queue<>();
        plans.addLast(new BuildPlan(4,5,1,drill));
        plans.addLast(new BuildPlan(5,5,0,duct));
        plans.addLast(new BuildPlan(6,5));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        Writes out=new Writes(new DataOutputStream(bytes));
        out.i(plans.size);
        for(BuildPlan plan:plans) TypeIO.writePlan(out,plan);
        Queue<BuildPlan> loaded=TypeIO.readPlansQueue(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        check(loaded.size==3 && loaded.get(0).block==drill && loaded.get(0).rotation==1);
        check(loaded.get(1).block==duct && loaded.get(1).x==5 && !loaded.get(1).breaking);
        check(loaded.get(2).breaking && loaded.get(2).x==6);
        Unit unit=UnitEntity.create(); unit.type=new UnitType("test-builder"); unit.type.buildSpeed=1f; unit.plans=loaded;
        unit.controller(new MetrumBuilderAI());
        check(unit.plans==loaded && unit.plans.size==3 && unit.buildPlan().block==drill);
        mindustry.type.Item iron=new mindustry.type.Item("test-iron");
        drill.requirements=new mindustry.type.ItemStack[]{new mindustry.type.ItemStack(iron,7)};
        duct.requirements=new mindustry.type.ItemStack[]{new mindustry.type.ItemStack(iron,2)};
        check(BasePlanner.queuedBuildReserve(unit).get(iron)==9);
        unit.plans.removeFirst();
        check(BasePlanner.queuedBuildReserve(unit).get(iron)==2);
        check(unit.buildPlan().block==duct);
        unit.controller(new MetrumBuilderAI());
        check(unit.plans.size==2 && unit.buildPlan().block==duct);
        unit.plans.clear();
        unit.plans.addLast(new BuildPlan(99,99,0,drill));
        unit.plans.addLast(new BuildPlan(5,5,0,duct));
        ((MetrumBuilderAI)unit.controller()).updateMovement();
        check(unit.plans.isEmpty());
        check(BasePlanner.queuedBuildReserve(unit).isEmpty());
        unit.plans.addLast(new BuildPlan(4,5,1,drill));
        unit.plans.addLast(new BuildPlan(5,5,0,duct));
        arc.struct.Seq<BuildPlan> backup=BasePlanner.copyBuildQueue(unit);
        unit.plans.clear();
        check(backup.size==2);
        check(BasePlanner.planMaterials(backup).get(iron)==9);
        check(BasePlanner.restoreBuildQueue(unit,backup));
        check(unit.plans.size==2 && unit.buildPlan().block==drill);
        check(!BasePlanner.restoreBuildQueue(unit,backup));
        unit.buildPlan().rotation=3;
        check(backup.first().rotation==1);
        bytes.reset(); out.i(1); out.i(42); out.i(mindustry.game.Team.crux.id); out.i(backup.size);
        for(BuildPlan plan:backup) TypeIO.writePlan(out,plan);
        new AbandonedBuilds().read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        check(AbandonedBuilds.pending.size==1 && AbandonedBuilds.pending.first().position==42);
        check(AbandonedBuilds.pending.first().plans.size==2 && AbandonedBuilds.pending.first().plans.first().block==drill);
        check(BasePlanner.planMaterials(AbandonedBuilds.pending.first().plans).get(iron)==9);
        byte[] broken=java.util.Arrays.copyOf(bytes.toByteArray(),bytes.size()-1);
        try { new AbandonedBuilds().read(new DataInputStream(new ByteArrayInputStream(broken))); throw new AssertionError(); }
        catch(EOFException expected) { check(AbandonedBuilds.pending.isEmpty()); }
        System.out.println("OK: saved plan order, rotations, demolition, controller restoration, queue continuation, reserved materials, released materials, invalid batch cancellation, constructor replacement queue, saved abandoned queue, truncated save");
    }
}