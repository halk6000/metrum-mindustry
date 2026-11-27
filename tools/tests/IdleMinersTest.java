package metrum.ai.planner;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.AirBlock;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;

/** Idle miner drones of the AI must start mining; units with orders and other roles are left alone. */
public class IdleMinersTest {
    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Vars.headless = true;
        Vars.content = new ContentLoader();
        Vars.state = new GameState();
        Vars.net = new mindustry.net.Net(null);
        Groups.init();
        Blocks.air = new AirBlock("air");
        Item ore = new Item("test-ore");
        ore.hardness = 1;
        OreBlock oreBlock = new OreBlock("ore-test", ore);
        Floor ground = new Floor("ground");
        Vars.world = new World();
        Vars.world.resize(20, 20);
        for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) Vars.world.tiles.set(x, y, new Tile(x, y, ground, Blocks.air, Blocks.air));
        Vars.world.tile(15, 15).setOverlayQuiet(oreBlock);
        Vars.indexer = new mindustry.ai.BlockIndexer();
        arc.Events.fire(new WorldLoadEvent());

        CoreBlock coreBlock = new CoreBlock("test-core");
        CoreBlock.CoreBuild core = (CoreBlock.CoreBuild) coreBlock.newBuilding();
        core.block = coreBlock;
        core.team = Team.crux;
        core.tile = Vars.world.tile(2, 2);
        core.items = new ItemModule();
        core.set(16f, 16f);
        core.storageCapacity = 1000;
        Vars.state.rules.coreIncinerates = false;
        Team.crux.data().buildingTree = null;
        Team.crux.data().cores.add(core);

        UnitCommand.mineCommand = new UnitCommand("mine", "production", u -> null);
        UnitCommand.moveCommand = new UnitCommand("move", "right", u -> null);
        UnitType miner = new UnitType("test-miner");
        miner.defaultCommand = UnitCommand.mineCommand;
        miner.mineTier = 2;
        miner.mineFloor = true;
        miner.mineItems = arc.struct.Seq.with(ore);
        UnitType fighter = new UnitType("test-fighter");
        fighter.defaultCommand = UnitCommand.moveCommand;
        fighter.mineTier = 2;
        fighter.mineItems = arc.struct.Seq.with(ore);

        Unit idle = unit(miner), squad = unit(miner), soldier = unit(fighter), player = unit(miner);
        ((CommandAI) squad.controller()).targetPos = new Vec2(100f, 100f);
        check(BasePlanner.idleMiner(idle, core), "idle miner is picked");
        check(!BasePlanner.idleMiner(squad, core), "unit with an RTS order is left alone");
        check(!BasePlanner.idleMiner(soldier, core), "combat type is left alone");
        // every idle miner is used, not just one
        Unit second = unit(miner);
        check(BasePlanner.assignIdleMiners(core) == 3, "all idle miners start (idle, second, player-type unit)");
        check(idle.controller() instanceof metrum.ai.SmartMining.SmartMinerAI
            && second.controller() instanceof metrum.ai.SmartMining.SmartMinerAI, "miners received the mining AI");
        check(squad.controller() instanceof CommandAI && soldier.controller() instanceof CommandAI, "others keep their AI");
        check(BasePlanner.assignIdleMiners(core) == 0, "miners are not re-assigned every tick");
        // a full core is not fed further
        Unit late = unit(miner);
        core.items.set(ore, 1000);
        check(!BasePlanner.idleMiner(late, core), "no mining into a full core");
        System.out.println("OK: all idle AI miners mine; ordered, combat and full-core cases left alone");
    }

    static Unit unit(UnitType type) {
        Unit u = UnitEntity.create();
        u.type = type;
        u.team = Team.crux;
        u.set(40f, 40f);
        u.controller(new CommandAI());
        u.add();
        return u;
    }
}
