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
        unit.stack = new mindustry.type.ItemStack(iron,5);
        ((MetrumBuilderAI)unit.controller()).updateMovement();
        check(unit.stack.item==iron && unit.stack.amount==5);
        check(unit.plans.isEmpty());
        check(BasePlanner.queuedBuildReserve(unit).isEmpty());
        unit.plans.addLast(new BuildPlan(4,5,1,drill));
        unit.plans.addLast(new BuildPlan(5,5,0,duct));
        arc.struct.Seq<BuildPlan> backup=BasePlanner.copyBuildQueue(unit);
        unit.plans.clear();
        check(backup.size==2);
        check(BasePlanner.planMaterials(backup).get(iron)==9);
        checkCargoDelivery(drill, iron);
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

    static void checkCargoDelivery(Block block, mindustry.type.Item ore) {
        mindustry.type.Item alternateOre = new mindustry.type.Item("test-alternate-ore");
        Vars.headless = true;
        mindustry.content.Blocks.air = new mindustry.world.blocks.environment.AirBlock("test-air");
        Vars.world.tiles.set(4, 5, new mindustry.world.Tile(4, 5, new mindustry.world.blocks.environment.Floor("test-floor"), mindustry.content.Blocks.air, mindustry.content.Blocks.air));
        block.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        Vars.net = new mindustry.net.Net(null);
        arc.util.Time.delta = 1f;
        mindustry.world.blocks.storage.CoreBlock coreBlock = new mindustry.world.blocks.storage.CoreBlock("test-core");
        mindustry.world.blocks.storage.CoreBlock.CoreBuild core = coreBlock.new CoreBuild() {
            @Override public int getMaximumAccepted(mindustry.type.Item item) { return 100; }
            @Override public void handleStack(mindustry.type.Item item, int amount, mindustry.gen.Teamc source) {
                items.add(item, amount);
            }
        };
        core.block = coreBlock;
        core.team = mindustry.game.Team.crux;
        core.items = new mindustry.world.modules.ItemModule();
        core.items.add(ore, 2);

        Unit builder = new UnitEntity() {
            @Override public mindustry.world.blocks.storage.CoreBlock.CoreBuild closestCore() { return core; }
        };
        builder.team = core.team;
        builder.type = new UnitType("test-delivery-builder");
        builder.type.buildSpeed = 1f;
        builder.type.mineTier = 1;
        builder.type.mineSpeed = 1f;
        builder.type.range = 20f;
        builder.type.itemCapacity = 15;
        builder.stack = new mindustry.type.ItemStack(ore, 5);
        MetrumBuilderAI controller = new MetrumBuilderAI();
        builder.controller(controller);
        BuildPlan plan = new BuildPlan(4, 5, 1, block);
        builder.plans.addLast(plan);

        controller.updateMovement();
        check(core.items.get(ore) == 7 && builder.stack.amount == 0);
        check(!builder.updateBuilding && builder.plans.size == 1 && builder.buildPlan() == plan);
        controller.updateMovement();
        check(builder.updateBuilding && builder.buildPlan() == plan);
        System.out.println("OK: partial cargo delivered and original construction resumed");

        mindustry.world.blocks.environment.OreBlock deposit = new mindustry.world.blocks.environment.OreBlock("test-deposit", ore);
        mindustry.world.Tile oreTile = new mindustry.world.Tile(1, 1, Vars.world.tile(4, 5).floor(), deposit, mindustry.content.Blocks.air);
        Vars.world.tiles.set(1, 1, oreTile);
        mindustry.world.blocks.environment.OreBlock alternateDeposit =
            new mindustry.world.blocks.environment.OreBlock("test-alternate-deposit", alternateOre);
        mindustry.world.Tile alternateTile = new mindustry.world.Tile(2, 1, oreTile.floor(), alternateDeposit, mindustry.content.Blocks.air);
        Vars.world.tiles.set(2, 1, alternateTile);
        class TestIndexer extends mindustry.ai.BlockIndexer {
            boolean originalAvailable = true;
            boolean alternateAvailable;
            @Override public boolean hasOre(mindustry.type.Item item) {
                return (item == ore && originalAvailable) || (item == alternateOre && alternateAvailable);
            }
            @Override public mindustry.world.Tile findClosestOre(Unit unit, mindustry.type.Item item) {
                return findClosestOre(unit.x, unit.y, item);
            }
            @Override public mindustry.world.Tile findClosestOre(float x, float y, mindustry.type.Item item) {
                if (!hasOre(item)) return null;
                return item == ore ? oreTile : alternateTile;
            }
        }
        TestIndexer indexer = new TestIndexer();
        Vars.indexer = indexer;
        builder.type.mineFloor = true;
        builder.type.mineRange = 30f;
        builder.type.mineItems.add(ore);
        builder.set(oreTile.worldx(), oreTile.worldy());
        core.items.remove(ore, 5);
        arc.util.Time.time += 240f;
        controller.updateMovement();
        check(builder.stack.amount == 0 && builder.mineTile == oreTile);
        check(!builder.updateBuilding && builder.plans.size == 1 && builder.buildPlan() == plan);
        System.out.println("OK: empty builder starts mining the missing ore and retains its plan");

        indexer.originalAvailable = false;
        indexer.alternateAvailable = true;
        builder.type.mineItems.add(alternateOre);
        builder.mineTile = null;
        metrum.ai.SmartMining.SmartMinerAI miner = new metrum.ai.SmartMining.SmartMinerAI();
        miner.unit(builder);
        miner.targetItem = ore;
        arc.util.Time.time = 0f;
        miner.updateMovement();
        check(miner.targetItem == alternateOre);
        arc.util.Time.time += 60f;
        miner.updateMovement();
        check(miner.targetItem == alternateOre && builder.mineTile == alternateTile);
        System.out.println("OK: miner switches to available ore after its previous deposit disappears");

        indexer.originalAvailable = true;
        core.items.add(alternateOre, core.getMaximumAccepted(alternateOre));
        miner.updateMovement();
        check(miner.targetItem == ore && builder.mineTile == null);
        arc.util.Time.time += 60f;
        miner.updateMovement();
        check(builder.mineTile == oreTile && builder.stack.amount == 0);
        System.out.println("OK: empty miner switches ore when the core cannot store its current target");
    }
}
