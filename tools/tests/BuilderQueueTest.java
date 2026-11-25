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
    static void verifyRepairSequence(arc.struct.Seq<BuildPlan> plans, mindustry.world.Tile start,
                                     mindustry.gen.Building furnace, mindustry.gen.Building input) {
        for (BuildPlan plan : plans) {
            check(!BasePlanner.sandOutletReady(start, furnace, input));
            mindustry.world.Tile site = Vars.world.tile(plan.x, plan.y);
            check(site.build == null);
            site.build = mindustry.gen.Building.create();
            site.build.tile = site;
            site.build.block = plan.block;
            site.build.rotation = plan.rotation;
        }
        check(BasePlanner.sandOutletReady(start, furnace, input));
    }
    public static void main(String[] args) throws Exception {
        Vars.content=new ContentLoader(); Vars.state=new mindustry.core.GameState();
        mindustry.content.Blocks.air=new mindustry.world.blocks.environment.Floor("air");
        Vars.world=new mindustry.core.World(); Vars.world.tiles=new mindustry.world.Tiles(8,8);
        for(int x=0;x<8;x++) for(int y=0;y<8;y++) Vars.world.tiles.set(x,y,new mindustry.world.Tile(x,y));
        arc.struct.Seq<mindustry.world.Tile> route = new arc.struct.Seq<>();
        for (int x = 0; x < 8; x++) route.add(Vars.world.tile(x, 2));
        mindustry.gen.Building furnace = mindustry.gen.Building.create();
        check(BasePlanner.stepsToEntrance(route, furnace) == 0);
        route.get(6).build = route.get(7).build = furnace;
        check(BasePlanner.stepsToEntrance(route, furnace) == 7);
        route.get(6).build = route.get(7).build = null;
        Block drill=new Block("test-drill"), duct=new Block("test-duct"); drill.rotate=true; duct.rotate=true;
        // Approach east, turn north into the furnace edge, then reach its anchor to the east.
        route = arc.struct.Seq.with(Vars.world.tile(2, 2), Vars.world.tile(3, 2),
            Vars.world.tile(3, 3), Vars.world.tile(4, 3));
        route.get(2).build = route.get(3).build = furnace;
        arc.struct.Seq<BuildPlan> connector = BasePlanner.planSandConnector(route,
            BasePlanner.stepsToEntrance(route, furnace), duct);
        check(connector.size == 2 && connector.first().rotation == 0);
        BuildPlan outlet = connector.peek();
        check(outlet.block == duct && outlet.x == 3 && outlet.y == 2 && outlet.rotation == 1);
        check(Vars.world.tile(outlet.x, outlet.y).nearby(outlet.rotation).build == furnace);
        mindustry.gen.Building sandInput = mindustry.gen.Building.create();
        sandInput.block = duct;
        for (BuildPlan segment : connector) {
            check(!BasePlanner.sandOutletReady(route.first(), furnace, sandInput));
            mindustry.gen.Building installed = mindustry.gen.Building.create();
            installed.block = duct;
            installed.tile = Vars.world.tile(segment.x, segment.y);
            installed.tile.build = installed;
            installed.rotation = segment.rotation;
        }
        check(BasePlanner.sandOutletReady(route.first(), furnace, sandInput));
        mindustry.gen.Building lastDuct = route.get(1).build;
        route.get(1).build = null;
        BuildPlan repair = BasePlanner.sandGapPlan(route.first(), furnace, sandInput, site -> true).first();
        check(repair != null && repair.block == duct && repair.x == 3 && repair.y == 2 && repair.rotation == 1);
        route.get(1).build = lastDuct;
        // A surviving upstream duct points at a gap before the two installed outlet ducts.
        mindustry.world.Tile upstream = Vars.world.tile(0, 2);
        upstream.build = mindustry.gen.Building.create();
        upstream.build.tile = upstream;
        upstream.build.block = duct;
        upstream.build.rotation = 0;
        check(!BasePlanner.sandOutletReady(upstream, furnace, sandInput));
        BuildPlan middleRepair = BasePlanner.sandGapPlan(upstream, furnace, sandInput, site -> true).first();
        check(middleRepair != null && middleRepair.x == 1 && middleRepair.y == 2
            && middleRepair.rotation == 0 && middleRepair.block == duct);
        mindustry.world.Tile gap = Vars.world.tile(middleRepair.x, middleRepair.y);
        gap.build = mindustry.gen.Building.create();
        gap.build.tile = gap;
        gap.build.block = middleRepair.block;
        gap.build.rotation = middleRepair.rotation;
        check(BasePlanner.sandOutletReady(upstream, furnace, sandInput));
        check(BasePlanner.sandGapPlan(upstream, furnace, sandInput, site -> true) == null);
        gap.build = route.first().build = null;
        arc.struct.Seq<BuildPlan> pair = BasePlanner.sandGapPlan(upstream, furnace, sandInput, site -> true);
        check(pair != null && pair.size == 2);
        check(pair.first().x == 2 && pair.first().y == 2 && pair.first().rotation == 0);
        check(pair.peek().x == 1 && pair.peek().y == 2 && pair.peek().rotation == 0);
        verifyRepairSequence(pair, upstream, furnace, sandInput);
        // Remove the corner and its approach, keeping the upstream ducts in service.
        route.first().build = route.get(1).build = null;
        arc.struct.Seq<BuildPlan> corner = BasePlanner.sandGapPlan(upstream, furnace, sandInput, site -> true);
        check(corner != null && corner.size == 2);
        check(corner.first().x == 3 && corner.first().y == 2 && corner.first().rotation == 1);
        check(corner.peek().x == 2 && corner.peek().y == 2 && corner.peek().rotation == 0);
        mindustry.world.Tile routeCorner = Vars.world.tile(3, 2);
        arc.struct.Seq<BuildPlan> detour = BasePlanner.sandGapPlan(upstream, furnace, sandInput,
            site -> site != routeCorner);
        check(detour != null && detour.size == 2);
        check(detour.first().x == 2 && detour.first().y == 3 && detour.first().rotation == 0);
        check(detour.peek().x == 2 && detour.peek().y == 2 && detour.peek().rotation == 1);
        mindustry.gen.Groups.init();
        Unit otherBuilder = UnitEntity.create();
        otherBuilder.team = sandInput.team;
        otherBuilder.plans.addLast(new BuildPlan(routeCorner.x, routeCorner.y, 0, drill));
        mindustry.gen.Groups.unit.add(otherBuilder);
        arc.struct.Seq<BuildPlan> unreserved = BasePlanner.sandGapPlan(upstream, furnace, sandInput,
            site -> !BasePlanner.overlapsQueuedBuild(sandInput.team, duct, site.x, site.y));
        check(unreserved != null && unreserved.size == detour.size);
        for (int i = 0; i < detour.size; i++) {
            BuildPlan expected = detour.get(i), actual = unreserved.get(i);
            check(actual.x == expected.x && actual.y == expected.y && actual.rotation == expected.rotation);
        }
        mindustry.gen.Groups.unit.remove(otherBuilder);
        verifyRepairSequence(detour, upstream, furnace, sandInput);
        Vars.world.tile(2, 3).build = route.first().build = null;
        verifyRepairSequence(corner, upstream, furnace, sandInput);
        upstream.build = gap.build = null;
        route.get(1).build.rotation = 2;
        check(!BasePlanner.sandOutletReady(route.first(), furnace, sandInput));
        route.get(0).build = route.get(1).build = null;
        route.get(2).build = route.get(3).build = null;
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
        checkTwoBuilders(drill, duct, iron);
        checkDeliveryTarget(iron);
        checkMiningStall();
        checkExpiredDroneWait(iron);
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

    static void checkExpiredDroneWait(mindustry.type.Item ore) throws Exception {
        mindustry.gen.Groups.init();
        mindustry.world.blocks.storage.CoreBlock block = new mindustry.world.blocks.storage.CoreBlock("timeout-core");
        mindustry.world.blocks.storage.CoreBlock.CoreBuild core = block.new CoreBuild();
        core.team = mindustry.game.Team.crux;
        Class<?> aiClass = Class.forName("metrum.ai.planner.BasePlanner$ResourceDroneAI");
        java.lang.reflect.Constructor<?> constructor = aiClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        mindustry.entities.units.UnitController ai = (mindustry.entities.units.UnitController)
            constructor.newInstance(core, new MetrumBuilderAI());
        Unit drone = UnitEntity.create();
        drone.type = new UnitType("timeout-drone");
        drone.team = core.team;
        drone.stack = new mindustry.type.ItemStack(ore, 1);
        drone.controller(ai);
        java.lang.reflect.Field goal = aiClass.getDeclaredField("tungstenGoal");
        java.lang.reflect.Field elapsed = aiClass.getDeclaredField("tungstenStalledTime");
        goal.setAccessible(true); elapsed.setAccessible(true);
        goal.set(ai, new mindustry.type.ItemStack(ore, 10));
        mindustry.gen.Groups.unit.add(drone);
        try {
            elapsed.setFloat(ai, 1199f);
            check(BasePlanner.hasLocalTungstenDrone(core, ore));
            elapsed.setFloat(ai, 1200f);
            check(!BasePlanner.hasLocalTungstenDrone(core, ore));
            elapsed.setFloat(ai, 0f);
            check(BasePlanner.hasLocalTungstenDrone(core, ore));
        } finally { mindustry.gen.Groups.unit.clear(); }
        System.out.println("OK: expired tungsten wait stops blocking recovery even with cargo aboard");
    }

    static void checkMiningStall() {
        float limit = 1200f;
        float elapsed = BasePlanner.advanceMiningStall(0f, true, false, 480f, limit);
        check(elapsed == 480f);
        // A minute spent on prerequisites must neither consume nor replenish the budget.
        elapsed = BasePlanner.advanceMiningStall(elapsed, false, false, 3600f, limit);
        check(elapsed == 480f);
        elapsed = BasePlanner.advanceMiningStall(elapsed, true, false, 719f, limit);
        check(elapsed == 1199f && elapsed < limit);
        elapsed = BasePlanner.advanceMiningStall(elapsed, true, false, 1f, limit);
        check(elapsed == limit);
        elapsed = BasePlanner.advanceMiningStall(elapsed, false, false, 3600f, limit);
        check(elapsed == limit);
        check(BasePlanner.advanceMiningStall(elapsed, true, false, 60f, limit) == limit);
        elapsed = BasePlanner.advanceMiningStall(elapsed, true, true, 1f, limit);
        check(elapsed == 0f);
        check(BasePlanner.advanceMiningStall(elapsed, true, false, 60f, limit) == 60f);
        System.out.println("OK: tungsten timeout pauses, resumes, expires and resets on progress");
    }

    static void checkDeliveryTarget(mindustry.type.Item cargo) {
        boolean[] valid = {true};
        int[] room = {10};
        mindustry.world.blocks.storage.CoreBlock block = new mindustry.world.blocks.storage.CoreBlock("delivery-target-core");
        mindustry.world.blocks.storage.CoreBlock.CoreBuild target = block.new CoreBuild() {
            @Override public boolean isValid() { return valid[0]; }
            @Override public int acceptStack(mindustry.type.Item item, int amount, mindustry.gen.Teamc source) {
                return Math.min(room[0], amount);
            }
        };
        Unit drone = UnitEntity.create();
        drone.team = target.team = mindustry.game.Team.crux;
        drone.stack = new mindustry.type.ItemStack(cargo, 5);
        check(BasePlanner.retainedDeliveryTarget(drone, target) == target);
        room[0] = 1;
        check(BasePlanner.retainedDeliveryTarget(drone, target) == target);
        room[0] = 0;
        check(BasePlanner.retainedDeliveryTarget(drone, target) == null);
        room[0] = 10; valid[0] = false;
        check(BasePlanner.retainedDeliveryTarget(drone, target) == null);
        valid[0] = true; target.team = mindustry.game.Team.sharded;
        check(BasePlanner.retainedDeliveryTarget(drone, target) == null);
        target.team = drone.team; drone.stack.amount = 0;
        check(BasePlanner.retainedDeliveryTarget(drone, target) == null);
        System.out.println("OK: delivery target retained with partial room, released when full, lost, hostile or cargo empty");
    }

    static void checkTwoBuilders(Block drill, Block duct, mindustry.type.Item iron) {
        mindustry.gen.Groups.init();
        mindustry.world.blocks.storage.CoreBlock coreBlock = new mindustry.world.blocks.storage.CoreBlock("two-builder-core");
        mindustry.world.blocks.storage.CoreBlock.CoreBuild core = coreBlock.new CoreBuild();
        core.tile = Vars.world.tile(1, 1);
        core.team = mindustry.game.Team.crux;
        UnitType previous = metrum.content.MetrumUnits.monoMetT1;
        Block previousDock = metrum.content.MetrumUnitBlocks.airMinerDock;
        UnitType type = new UnitType("two-builder-unit");
        type.buildSpeed = 1f;
        metrum.content.MetrumUnits.monoMetT1 = type;
        try {
            Unit first = UnitEntity.create(), second = UnitEntity.create();
            for (Unit builder : new Unit[]{first, second}) {
                builder.type = type;
                builder.team = core.team;
                builder.flag = BasePlanner.builderHomeFlag(core);
                builder.controller(new MetrumBuilderAI());
                mindustry.gen.Groups.unit.add(builder);
            }
            first.addBuild(new BuildPlan(4, 5, 0, drill));
            check(!BasePlanner.miningDockExitClear(core.team, Vars.world.tile(4, 5)));
            check(!BasePlanner.miningDockExitClear(core.team, null));
            check(BasePlanner.hasQueuedBlock(core, drill));
            check(!BasePlanner.hasQueuedBlock(core, duct));
            check(BasePlanner.assignedBuilder(core) == second);
            check(BasePlanner.constructionReserve(core).get(iron) == 7);
            second.addBuild(new BuildPlan(5, 5, 0, duct));
            check(BasePlanner.assignedBuilder(core) == first);
            check(BasePlanner.constructionReserve(core).get(iron) == 9);
            check(BasePlanner.overlapsQueuedBuild(core.team, drill, 4, 5));
            first.plans.clear();
            check(BasePlanner.miningDockExitClear(core.team, Vars.world.tile(4, 5)));
            check(!BasePlanner.hasQueuedBlock(core, drill));
            check(BasePlanner.hasQueuedBlock(core, duct));
            check(BasePlanner.assignedBuilder(core) == first);
            check(BasePlanner.constructionReserve(core).get(iron) == 2);
            second.plans.clear();
            check(BasePlanner.constructionReserve(core).isEmpty());
            metrum.content.MetrumUnitBlocks.airMinerDock = drill;
            BuildPlan dockPlan = new BuildPlan(4, 5, 0, drill);
            BuildPlan obstruction = new BuildPlan(5, 5, 0, duct);
            check(BasePlanner.batchBlocksDockExit(arc.struct.Seq.with(dockPlan, obstruction)));
            check(BasePlanner.batchBlocksDockExit(arc.struct.Seq.with(obstruction, dockPlan)));
            check(!BasePlanner.batchBlocksDockExit(arc.struct.Seq.with(dockPlan, new BuildPlan(4, 6, 0, duct))));
            check(!BasePlanner.batchBlocksDockExit(arc.struct.Seq.with(dockPlan, new BuildPlan(5, 5))));
            first.addBuild(new BuildPlan(4, 5, 0, drill));
            check(BasePlanner.blocksMiningDockExit(core.team, duct, 5, 5));
            check(!BasePlanner.blocksMiningDockExit(core.team, duct, 4, 6));
            check(!BasePlanner.blocksMiningDockExit(mindustry.game.Team.sharded, duct, 5, 5));
            first.plans.clear();
            check(!BasePlanner.blocksMiningDockExit(core.team, duct, 5, 5));
            System.out.println("OK: two constructors, idle selection, combined reservations and released budgets");
        } finally {
            mindustry.gen.Groups.unit.clear();
            metrum.content.MetrumUnits.monoMetT1 = previous;
            metrum.content.MetrumUnitBlocks.airMinerDock = previousDock;
        }
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

        mindustry.world.Tile siteTile = Vars.world.tile(plan.x, plan.y);
        mindustry.world.blocks.ConstructBlock constructBlock = new mindustry.world.blocks.ConstructBlock(1);
        mindustry.world.blocks.ConstructBlock.ConstructBuild site = constructBlock.new ConstructBuild();
        site.team = builder.team;
        site.current = block;
        site.progress = 0.5f;
        siteTile.build = site;
        builder.type.buildRange = 100f;
        core.items.set(ore, 0);
        builder.set(siteTile.worldx() + 2f * builder.type.buildRange, siteTile.worldy());
        controller.updateMovement();
        for (int tick = 0; tick < 60; tick++) controller.updateMovement();
        check(builder.updateBuilding && builder.mineTile == null && builder.buildPlan() == plan);

        builder.set(oreTile.worldx(), oreTile.worldy());
        for (int tick = 0; tick < 25; tick++) controller.updateMovement();
        check(builder.updateBuilding && builder.mineTile == null);
        System.out.println("OK: travel outside build range does not count as stalled construction");
        site.progress = 0.6f;
        controller.updateMovement();
        for (int tick = 0; tick < 25; tick++) controller.updateMovement();
        check(builder.updateBuilding && builder.buildPlan() == plan);
        arc.util.Time.time += 60f;
        for (int tick = 0; tick < 5; tick++) controller.updateMovement();
        check(!builder.updateBuilding && builder.mineTile == oreTile && builder.buildPlan() == plan);
        core.items.set(ore, 1);
        controller.updateMovement();
        check(builder.updateBuilding && site.progress == 0.6f && builder.buildPlan() == plan);
        siteTile.build = null;
        System.out.println("OK: started construction keeps its progress while the builder replenishes an empty stock");

        core.items.set(ore, 2);
        arc.util.Time.time += 240f;
        controller.updateMovement();
        check(builder.mineTile == oreTile);

        builder.stack.amount = 3;
        builder.stack.item = ore;
        builder.plans.clear();
        core.items.set(ore, core.getMaximumAccepted(ore));
        controller.updateMovement();
        check(builder.stack.amount == 3 && builder.mineTile == null);
        core.items.remove(ore, 3);
        controller.updateMovement();
        check(builder.stack.amount == 0 && core.items.get(ore) == core.getMaximumAccepted(ore));
        core.items.set(ore, 2);
        System.out.println("OK: builder keeps mined cargo until the core has room again");

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
