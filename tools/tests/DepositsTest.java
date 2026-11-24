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
        for (int i = 0; i < 100; i++) Deposits.update();
        check(Deposits.all.get(t1.pos()).amount + Deposits.all.get(t2.pos()).amount == 7);
        for (int i = 0; i < 6; i++) cycle(d);
        int left = 0;
        for (arc.struct.IntMap.Entry<Deposits.Deposit> e : Deposits.all.entries()) left += e.value.amount;
        check(left == 1);
        cycle(d);
        check(Deposits.all.size == 0 && removed[0] == 2 && t1.drop() == null && t2.drop() == null);

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
        System.out.println("OK: deposit mining, exhaustion and save/load");
    }
}
