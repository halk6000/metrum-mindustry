import arc.util.io.Reads;
import arc.util.io.Writes;
import java.io.*;
import metrum.world.OpenPitMine;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.type.Item;
import mindustry.type.ItemStack;

public class OpenPitMineTest {
    static void check(boolean ok) {
        if (!ok) throw new AssertionError("Incorrect open-pit mine");
    }

    static OpenPitMine.OpenPitMineBuild mine(OpenPitMine block) {
        OpenPitMine.OpenPitMineBuild b = block.new OpenPitMineBuild();
        b.block = block;
        b.team = mindustry.game.Team.sharded;
        return b;
    }

    static void run(OpenPitMine.OpenPitMineBuild b, int ticks) {
        for (int i = 0; i < ticks; i++) b.updateTile();
    }

    static int total(OpenPitMine.OpenPitMineBuild b) {
        int t = 0;
        for (Item i : Vars.content.items()) t += b.found.get(i);
        return t;
    }

    public static void main(String[] args) throws Exception {
        Vars.content = new ContentLoader();
        Vars.state = new mindustry.core.GameState();
        Vars.net = new mindustry.net.Net(null);
        arc.util.Time.delta = 1f;
        Item good = new Item("good"), junk = new Item("junk"), never = new Item("never");
        OpenPitMine block = new OpenPitMine("test-pit");
        block.collapses = false; // scattering is covered by DepositsTest
        block.discoveries = new ItemStack[]{new ItemStack(good, 3), new ItemStack(junk, 1), new ItemStack(never, 0)};
        OpenPitMine.OpenPitMineBuild b = mine(block);
        run(b, 590);
        check(b.step == 0);
        run(b, 20);
        check(b.step == 1);
        block.discoveryChance = 1f;
        run(b, 600 * 120);
        check(b.step == 100 && b.fraction() == 1f && b.found.get(never) == 0);
        check(b.found.get(good) > b.found.get(junk) && b.found.get(junk) > 0);
        int done = total(b);
        check(done >= 99 * 50 && done <= 100 * 200);
        run(b, 5000);
        check(total(b) == done); // finished pits stop rolling
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        b.write(new Writes(new DataOutputStream(bytes)));
        OpenPitMine.OpenPitMineBuild copy = mine(block);
        copy.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))), (byte) 0);
        check(copy.step == 100 && total(copy) == done && copy.found.get(junk) == b.found.get(junk));
        block.discoveryChance = 0f;
        OpenPitMine.OpenPitMineBuild unlucky = mine(block);
        run(unlucky, 600 * 120);
        check(unlucky.step == 100 && unlucky.found.isEmpty());
        // Long-run discovery rate stays near the configured 20%.
        block.discoveryChance = 0.2f;
        block.stepTime = 1f;
        block.steps = 20000;
        block.minDiscovery = block.maxDiscovery = 1;
        OpenPitMine.OpenPitMineBuild many = mine(block);
        run(many, 20100);
        check(total(many) > 3600 && total(many) < 4400);
        System.out.println("OK: open-pit timing, discoveries, completion and save/load");
    }
}
