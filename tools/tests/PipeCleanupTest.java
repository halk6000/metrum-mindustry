package metrum.ai.planner;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.modules.LiquidModule;

public class PipeCleanupTest {
    static Building pipe() {
        Building b = Building.create(); b.block = Blocks.reinforcedConduit; b.team = Team.crux;
        b.liquids = new LiquidModule(); return b;
    }
    static void link(Building a, Building b) { a.proximity.add(b); b.proximity.add(a); }
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        Vars.content = new ContentLoader();
        Blocks.reinforcedConduit = new Block("test-pipe"); Blocks.reinforcedConduit.hasLiquids = true;
        Liquid water = new Liquid("test-water");
        Building a = pipe(), b = pipe(); link(a,b);
        check(BasePlanner.unusedWaterPipes(Seq.with(a,b), Team.crux).size == 2);
        b.liquids.add(water, 1f);
        check(BasePlanner.unusedWaterPipes(Seq.with(a,b), Team.crux).isEmpty());
        b.liquids.clear();
        Building consumer = pipe(); consumer.block = new Block("consumer"); consumer.block.hasLiquids = true;
        link(b,consumer);
        check(BasePlanner.unusedWaterPipes(Seq.with(a,b), Team.crux).isEmpty());
        b.proximity.remove(consumer); consumer.proximity.clear(); b.team = Team.sharded;
        check(BasePlanner.unusedWaterPipes(Seq.with(a), Team.crux).isEmpty());
        Seq<Building> longLine = new Seq<>();
        for (int i=0;i<9;i++) { Building p=pipe(); if(i>0) link(longLine.peek(),p); longLine.add(p); }
        check(BasePlanner.unusedWaterPipes(longLine, Team.crux).size == 6);
        System.out.println("OK: isolated, wet, consumer-connected, foreign-connected, removal limit");
    }
}