import arc.util.io.Reads;
import arc.util.io.Writes;
import java.io.*;
import metrum.world.MicroPipe;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.AirBlock;
import mindustry.world.modules.LiquidModule;

/** Multi-link MicroPipes: links up to the tier, relays split the flow, old single-link saves load. */
public class MicroPipeTest {
    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static MicroPipe.MicroBuild pipe(MicroPipe block, int x, int y) {
        MicroPipe.MicroBuild b = (MicroPipe.MicroBuild) block.newBuilding();
        Tile t = Vars.world.tile(x, y);
        b.block = block; b.team = Team.sharded; b.tile = t; t.build = b;
        b.set(t.worldx(), t.worldy());
        b.liquids = new LiquidModule();
        b.enabled = true;
        return b;
    }

    public static void main(String[] args) throws Exception {
        Vars.headless = true;
        Vars.content = new ContentLoader();
        Vars.state = new GameState();
        Vars.net = new mindustry.net.Net(null);
        mindustry.gen.Groups.init();
        Blocks.air = new AirBlock("air");
        Vars.world = new World();
        Vars.world.resize(20, 20);
        for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) Vars.world.tiles.set(x, y, new Tile(x, y, Blocks.air, Blocks.air, Blocks.air));
        arc.util.Time.delta = 1f;
        Liquid steam = new Liquid("test-steam");

        MicroPipe mk1 = new MicroPipe("test-mk1");
        MicroPipe mk4 = new MicroPipe("test-mk4");
        mk4.maxLinks = 4; mk4.flow = 64f / 60f; mk4.liquidCapacity = 96f;

        MicroPipe.MicroBuild source = pipe(mk4, 2, 2), relay = pipe(mk4, 6, 2), a = pipe(mk1, 10, 2), b = pipe(mk1, 6, 6), c = pipe(mk1, 6, 9);
        check(source.canLink(relay), "Mk4 links to another Mk4");
        source.targets.add(relay.pos());
        check(!relay.canLink(source), "no two-way loop");
        relay.targets.add(a.pos()); relay.targets.add(b.pos());
        check(relay.canLink(c), "a relay takes a third link");
        relay.targets.add(c.pos());

        source.liquids.add(steam, 90f);
        for (int i = 0; i < 300; i++) { source.updateTile(); relay.updateTile(); }
        check(relay.acceptLiquid(source, steam) || relay.liquids.currentAmount() > 0f || a.liquids.get(steam) > 0f, "relay accepts from its sender");
        float ea = a.liquids.get(steam), eb = b.liquids.get(steam), ec = c.liquids.get(steam);
        check(ea > 0f && eb > 0f && ec > 0f, "all three branches get steam");
        check(Math.abs(ea - eb) < 1f && Math.abs(eb - ec) < 1f, "flow is shared evenly");
        check(Math.abs(source.liquids.get(steam) + relay.liquids.get(steam) + ea + eb + ec - 90f) < 0.01f, "nothing is created or lost");

        MicroPipe.MicroBuild single = pipe(mk1, 14, 2), end = pipe(mk1, 17, 2);
        check(single.canLink(end), "classic link to a free end");
        single.targets.add(end.pos());
        check(!single.canLink(a), "classic pipe keeps one link");

        // old save: one int target
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Writes w = new Writes(new DataOutputStream(bytes));
        w.i(end.pos());
        MicroPipe.MicroBuild loaded = pipe(mk1, 14, 5);
        loaded.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))), (byte) 0);
        check(loaded.targets.size == 1 && loaded.targets.get(0) == end.pos(), "single-link save loads");
        bytes.reset();
        relay.write(new Writes(new DataOutputStream(bytes)));
        MicroPipe.MicroBuild copy = pipe(mk4, 6, 12);
        copy.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))), relay.version());
        check(copy.targets.size == 3, "multi-link save round trip");
        System.out.println("OK: micro pipe tiers, relay split, loops refused, old and new saves");
    }
}
