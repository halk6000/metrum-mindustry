import metrum.content.*;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.modules.ItemModule;

/** Exercises the actual mod building with a blocked output, without opening a save. */
public class SalvagerOutputTest {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Vars.headless = true;
        Vars.content = new ContentLoader();
        Vars.state = new GameState();
        Vars.content.createBaseContent();
        MetrumItems.load();
        MetrumLiquids.load();
        MetrumProduction.load();
        MetrumProduction.salvagerOverclocked.init();
        Building b = MetrumProduction.salvagerOverclocked.buildType.get();
        b.block = MetrumProduction.salvagerOverclocked;
        b.team = Team.sharded;
        b.enabled = true;
        b.items = new ItemModule();
        b.offload(MetrumItems.iron);
        check(b.items.get(MetrumItems.iron) == 2, "iron roll must yield two");
        b.offload(Items.sand);
        check(b.items.get(Items.sand) == 1, "sand roll must yield one");
        b.items.clear();
        b.items.add(MetrumItems.iron, 18);
        check(b.shouldConsume(), "two free slots must allow crafting");
        b.offload(MetrumItems.iron);
        check(b.items.total() == 20 && !b.shouldConsume(), "full output must stop");
        b.items.remove(MetrumItems.iron, 1);
        check(!b.shouldConsume(), "one free slot must not start a double batch");
        // A limited receiver models a belt that becomes blocked mid-batch.
        final int[] freeSlots = {2};
        Building receiver = new Building() {
            @Override
            public boolean acceptItem(Building source, mindustry.type.Item item) {
                return freeSlots[0] > 0;
            }
            @Override
            public void handleItem(Building source, mindustry.type.Item item) {
                freeSlots[0]--;
                items.add(item, 1);
            }
        };
        receiver.items = new ItemModule();
        b.proximity.add(receiver);
        b.items.clear();
        b.offload(MetrumItems.iron);
        check(receiver.items.get(MetrumItems.iron) == 2 && b.items.total() == 0,
            "open receiver must get both iron items");
        receiver.items.clear();
        freeSlots[0] = 1;
        b.offload(MetrumItems.iron);
        check(receiver.items.get(MetrumItems.iron) == 1 && b.items.get(MetrumItems.iron) == 1,
            "partially blocked receiver must leave the second iron in storage");
        b.items.clear();
        receiver.items.clear();
        freeSlots[0] = 1;
        b.offload(Items.sand);
        check(receiver.items.get(Items.sand) == 1 && b.items.total() == 0,
            "sand must be delivered once");
        Vars.world = new mindustry.core.World();
        Vars.world.tiles = new mindustry.world.Tiles(8, 8);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++)
            Vars.world.tiles.set(x, y, new mindustry.world.Tile(x, y));
        b.tile = Vars.world.tile(2, 2);
        b.tile.build = b;
        mindustry.world.blocks.distribution.Conveyor conveyor =
            (mindustry.world.blocks.distribution.Conveyor) mindustry.content.Blocks.conveyor;
        conveyor.init();
        mindustry.world.blocks.distribution.Conveyor.ConveyorBuild belt = conveyor.new ConveyorBuild();
        belt.block = conveyor;
        belt.team = b.team;
        belt.tile = Vars.world.tile(4, 2);
        belt.tile.build = belt;
        belt.rotation = 0;
        belt.items = new ItemModule();
        b.proximity.clear();
        b.proximity.add(belt);
        b.items.clear();
        b.offload(MetrumItems.iron);
        check(belt.items.get(MetrumItems.iron) >= 1, "actual conveyor must receive iron");
        check(belt.items.get(MetrumItems.iron) + b.items.get(MetrumItems.iron) == 2,
            "actual conveyor must preserve both iron items");
        belt.items.clear();
        belt.len = 0;
        belt.minitem = 1f;
        b.items.clear();
        b.offload(Items.sand);
        check(belt.items.get(Items.sand) == 1 && b.items.total() == 0,
            "actual conveyor must receive exactly one sand");
        belt.items.clear();
        belt.len = 3;
        belt.minitem = 0f;
        b.offload(MetrumItems.iron);
        check(b.items.get(MetrumItems.iron) == 2, "blocked belt must retain the entire batch");
        check(!b.dump(MetrumItems.iron), "blocked belt must refuse stored iron");
        belt.len = 0;
        belt.minitem = 1f;
        check(b.dump(MetrumItems.iron), "unblocked belt must accept stored iron");
        check(b.items.get(MetrumItems.iron) == 1 && belt.items.get(MetrumItems.iron) == 1,
            "first stored iron must transfer without duplication");
        // Simulate the belt moving its first item away from the input.
        belt.minitem = 1f;
        check(b.dump(MetrumItems.iron), "remaining iron must also transfer");
        check(b.items.total() == 0 && belt.items.get(MetrumItems.iron) == 2,
            "unblocking must drain the batch without losing items");
        check(!b.dump(MetrumItems.iron), "empty storage must not create extra iron");
        b.proximity.clear();
        b.liquids = new mindustry.world.modules.LiquidModule();
        mindustry.world.blocks.production.Separator.SeparatorBuild separator =
            (mindustry.world.blocks.production.Separator.SeparatorBuild) b;
        int ironRolls = 0, sandRolls = 0;
        for (int seed = 0; seed < 100; seed++) {
            b.items.clear();
            b.items.add(MetrumItems.pieceRock, 3);
            separator.seed = seed;
            separator.progress = 1f;
            b.efficiency = 0f; // Complete the prepared cycle without advancing the next one.
            separator.updateTile();
            check(b.items.get(MetrumItems.pieceRock) == 2, "each completed cycle must consume one rock");
            int iron = b.items.get(MetrumItems.iron), sand = b.items.get(Items.sand);
            check((iron == 2 && sand == 0) || (iron == 0 && sand == 1),
                "a full cycle must produce either two iron or one sand");
            if (iron == 2) ironRolls++; else sandRolls++;
        }
        check(ironRolls > 0 && sandRolls > 0, "full cycles must exercise both possible results");
        check(((mindustry.world.blocks.production.Separator) b.block).craftTime
            == ((mindustry.world.blocks.production.Separator) MetrumProduction.salvager).craftTime,
            "overclocked salvager must use the normal cycle duration");
        b.items.clear();
        b.items.add(MetrumItems.pieceRock, 10);
        b.liquids.add(MetrumLiquids.steam, 30f);
        b.power = new mindustry.world.modules.PowerModule();
        b.power.status = 1f;
        separator.progress = 0f;
        arc.util.Time.delta = 1f;
        float steamBefore = b.liquids.get(MetrumLiquids.steam);
        for (int tick = 0; tick < 60; tick++) {
            b.updateConsumption();
            separator.updateTile();
        }
        check(Math.abs(steamBefore - b.liquids.get(MetrumLiquids.steam) - 9f) < 0.01f,
            "one second of operation must consume nine steam");
        check(Math.abs(b.block.consPower.requestedPower(b) * 60f - 180f) < 0.01f,
            "running power demand must be 180 per second");
        check(b.items.get(MetrumItems.pieceRock) < 10, "powered operation must complete cycles");
        b.items.clear();
        b.items.add(MetrumItems.pieceRock, 10);
        b.items.add(MetrumItems.iron, 20);
        steamBefore = b.liquids.get(MetrumLiquids.steam);
        float progressBefore = separator.progress;
        for (int tick = 0; tick < 60; tick++) {
            b.updateConsumption();
            separator.updateTile();
            check(b.block.consPower.requestedPower(b) == 0f,
                "full storage must not request power");
        }
        check(b.liquids.get(MetrumLiquids.steam) == steamBefore,
            "full storage must not consume steam");
        check(b.items.get(MetrumItems.pieceRock) == 10 && b.items.get(MetrumItems.iron) == 20,
            "full storage must not consume rock or produce more iron");
        check(separator.progress == progressBefore, "full storage must pause crafting progress");
        System.out.println("OK: running consumption, zero consumption with full storage; 100 cycles ("
            + ironRolls + " iron / " + sandRolls + " sand rolls)");
    }
}

