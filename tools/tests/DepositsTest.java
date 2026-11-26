import java.io.*;
import metrum.world.Deposits;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.AirBlock;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.production.BurstDrill;
import mindustry.world.blocks.production.Drill;

public class DepositsTest {
    static void check(boolean ok) {
        if (!ok) throw new AssertionError("Incorrect deposits");
    }

    static Drill.DrillBuild drill(Drill block, int x, int y, Item item) {
        Drill.DrillBuild d = (Drill.DrillBuild) block.newBuilding();
        d.block = block;
        d.team = Team.sharded;
        d.tile = Vars.world.tile(x, y);
        for (Tile t : d.tile.getLinkedTilesAs(block, new arc.struct.Seq<>())) t.build = d;
        d.dominantItem = item;
        d.dominantItems = 4;
        return d;
    }

    /** one mining cycle as seen by the tracker */
    static void cycle(Drill.DrillBuild d) {
        d.progress = 90f;
        Deposits.update();
        d.progress = 1f;
        Deposits.update();
    }

    static int stock(Item item) {
        int sum = 0;
        for (arc.struct.IntMap.Entry<Deposits.Deposit> e : Deposits.all.entries()) if (e.value.item == item) sum += e.value.amount;
        return sum;
    }

    public static void main(String[] args) throws Exception {
        Vars.headless = true;
        Vars.content = new ContentLoader();
        Vars.state = new GameState();
        Vars.net = new mindustry.net.Net(null);
        Groups.init();
        Blocks.air = new AirBlock("air");
        Item ore = new Item("ore");
        OreBlock oreBlock = new OreBlock("ore-test", ore);
        Vars.world = new World();
        Vars.world.resize(12, 12);
        for (int x = 0; x < 12; x++) for (int y = 0; y < 12; y++) {
            Vars.world.tiles.set(x, y, new Tile(x, y, Blocks.air, Blocks.air, Blocks.air));
        }
        // The single-player overlay packet needs a running client: apply it directly instead.
        int[] removed = {0};
        Deposits.removeOre = t -> {
            removed[0]++;
            t.setOverlayQuiet(Blocks.air);
        };
        Deposits.Chunk chunk = new Deposits.Chunk();
        check(Deposits.oreFor(ore) == oreBlock);

        // Save data round trip, including unknown items being skipped.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        Drill small = new Drill("test-drill");
        small.size = 2;
        out.writeInt(3);
        Tile t1 = Vars.world.tile(4, 4), t2 = Vars.world.tile(5, 5);
        out.writeInt(t1.pos()); out.writeUTF("ore"); out.writeInt(3);
        out.writeInt(t2.pos()); out.writeUTF("ore"); out.writeInt(4);
        out.writeInt(Vars.world.tile(9, 9).pos()); out.writeUTF("gone"); out.writeInt(4);
        byte[] save = bytes.toByteArray();
        chunk.read(new DataInputStream(new ByteArrayInputStream(save)));
        check(Deposits.all.size == 2);
        ByteArrayOutputStream again = new ByteArrayOutputStream();
        chunk.write(new DataOutputStream(again));
        chunk.read(new DataInputStream(new ByteArrayInputStream(again.toByteArray())));
        check(Deposits.all.size == 2 && Deposits.all.get(t1.pos()).amount == 3 && Deposits.all.get(t2.pos()).amount == 4);

        // Seven items exhaust both tiles; waiting drills take nothing.
        t1.setOverlayQuiet(oreBlock);
        t2.setOverlayQuiet(oreBlock);
        Drill.DrillBuild d = drill(small, 4, 4, ore);
        d.progress = 50f;
        check(Deposits.remaining(t1) == 7 && Deposits.remaining(Vars.world.tile(8, 8)) == 0 && Deposits.remaining(null) == 0);
        for (int i = 0; i < 100; i++) Deposits.update();
        check(Deposits.all.get(t1.pos()).amount + Deposits.all.get(t2.pos()).amount == 7);
        for (int i = 0; i < 6; i++) cycle(d);
        int left = 0;
        for (arc.struct.IntMap.Entry<Deposits.Deposit> e : Deposits.all.entries()) left += e.value.amount;
        check(left == 1);
        cycle(d);
        check(Deposits.all.size == 0 && removed[0] == 2 && t1.drop() == null && t2.drop() == null);

        // Absence: charged by saved rate and time, never below zero, nothing for idle drills or no time away.
        chunk.read(new DataInputStream(new ByteArrayInputStream(save)));
        t1.setOverlayQuiet(oreBlock);
        t2.setOverlayQuiet(oreBlock);
        Deposits.AwayChunk away = new Deposits.AwayChunk();
        for (float[] c : new float[][]{{0f, 500, 7}, {0.01f, 0, 7}, {0.01f, 500, 2}, {0.01f, 9000000, 0}}) {
            d.lastDrillSpeed = c[0] / 60f;
            ByteArrayOutputStream rates = new ByteArrayOutputStream();
            away.write(new DataOutputStream(rates));
            away.read(new DataInputStream(new ByteArrayInputStream(rates.toByteArray())));
            Deposits.mineAway((int) c[1]);
            check(Math.abs(stock(ore) - c[2]) <= (c[2] == 2 ? 1 : 0));
        }
        // Burst drills mine one item per ore tile in each cycle.
        BurstDrill burst = new BurstDrill("test-burst");
        burst.size = 2;
        chunk.read(new DataInputStream(new ByteArrayInputStream(save)));
        t1.setOverlayQuiet(oreBlock);
        t2.setOverlayQuiet(oreBlock);
        Drill.DrillBuild b = drill(burst, 4, 4, ore);
        cycle(b);
        left = 0;
        for (arc.struct.IntMap.Entry<Deposits.Deposit> e : Deposits.all.entries()) left += e.value.amount;
        check(left == 3);

        // Ore replaced from outside drops the record; a fresh world starts clean.
        t1.setOverlayQuiet(Blocks.air);
        t2.setOverlayQuiet(Blocks.air);
        Deposits.update();
        check(Deposits.all.size == 0);
        // Collapsed pits reveal the exact discovered amounts on free ground only.
        Item second = new Item("second"), oreless = new Item("oreless");
        OreBlock secondBlock = new OreBlock("ore-second", second);
        Floor ground = new Floor("ground");
        mindustry.world.blocks.environment.StaticWall wall = new mindustry.world.blocks.environment.StaticWall("wall");
        Floor water = new Floor("water");
        water.isLiquid = true;
        Deposits.setOre = Tile::setOverlayQuiet;
        arc.struct.ObjectIntMap<Item> found = new arc.struct.ObjectIntMap<>();
        found.put(ore, 610);
        found.put(second, 407);
        found.put(oreless, 99);
        Vars.world.resize(60, 60);
        for (int x = 0; x < 60; x++) for (int y = 0; y < 60; y++) {
            Vars.world.tiles.set(x, y, new Tile(x, y, x < 28 ? water : ground, Blocks.air, x > 34 ? wall : Blocks.air));
        }
        Tile natural = Vars.world.tile(30, 30);
        natural.setOverlayQuiet(secondBlock);
        Deposits.scatter(30, 30, found, 25, 8, 24);
        // Independent compact patches receive the density bonus at their respective sizes.
        int firstTileCount = 25, secondTileCount = 17;
        if (stock(ore) != firstTileCount * 273 || stock(second) != secondTileCount * 117) {
            throw new AssertionError("Density: " + stock(ore) + ", " + stock(second) + ", tiles: " + Deposits.all.size);
        }
        check(stock(oreless) == 0 && !Deposits.all.containsKey(natural.pos()));
        check(Deposits.all.size == firstTileCount + secondTileCount);
        for (arc.struct.IntMap.Entry<Deposits.Deposit> e : Deposits.all.entries()) {
            Tile t = Vars.world.tile(e.key);
            check(t.x >= 28 && t.x <= 34 && t.drop() == e.value.item);
        }
        // Almost no room: nothing is lost, the few tiles are just richer.
        Deposits.clear();
        for (int x = 0; x < 60; x++) for (int y = 0; y < 60; y++) {
            Vars.world.tiles.set(x, y, new Tile(x, y, x == 30 && y < 33 && y > 29 ? ground : water, Blocks.air, Blocks.air));
        }
        Deposits.scatter(30, 30, found, 25, 8, 24);
        check(stock(ore) == 610 && stock(second) == 407 && Deposits.all.size == 3);
        for (Deposits.Deposit deposit : Deposits.all.values()) check(deposit.layers == 1);
        for (int layer = 2; layer <= 3; layer++) {
            Deposits.scatter(30, 30, found, 25, 8, 24);
            check(stock(ore) == 610 * layer && stock(second) == 407 * layer && Deposits.all.size == 3);
            for (Deposits.Deposit deposit : Deposits.all.values()) check(deposit.layers == layer);
        }
        Deposits.scatter(30, 30, found, 25, 8, 24);
        check(stock(ore) == 610 * 3 && stock(second) == 407 * 3);
        // No room at all must not crash.
        Deposits.clear();
        for (int y = 30; y < 33; y++) Vars.world.tiles.set(30, y, new Tile(30, y, water, Blocks.air, Blocks.air));
        Deposits.scatter(30, 30, found, 25, 8, 24);
        check(Deposits.all.size == 0);
        System.out.println("OK: deposit mining, exhaustion and save/load");
    }
}
