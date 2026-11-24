package metrum.ai.planner;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock;
public class ResumeConstructionTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        Vars.content=new ContentLoader();
        ConstructBlock block=new ConstructBlock(1);
        ConstructBlock.ConstructBuild site=block.new ConstructBuild();
        site.team=Team.crux; site.current=new Block("test-factory"); site.wasConstructing=true;
        Unit builder=UnitEntity.create(); builder.team=Team.crux;
        check(BasePlanner.canResumeConstruction(site,builder));
        site.activeDeconstruct=true; check(!BasePlanner.canResumeConstruction(site,builder)); site.activeDeconstruct=false;
        site.team=Team.sharded; check(!BasePlanner.canResumeConstruction(site,builder)); site.team=Team.crux;
        site.lastBuilder=UnitEntity.create();
        check(!BasePlanner.canResumeConstruction(site,builder));
        site.lastBuilder.dead=true; check(BasePlanner.canResumeConstruction(site,builder));
        site.lastBuilder=builder; check(BasePlanner.canResumeConstruction(site,builder));
        site.wasConstructing=false; check(!BasePlanner.canResumeConstruction(site,builder));
        System.out.println("OK: abandoned, demolition, ownership, active builder, dead builder, own job, inactive site");
    }
}