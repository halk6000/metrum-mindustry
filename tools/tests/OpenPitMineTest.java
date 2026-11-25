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
        b.optionalEfficiency = 0f;
        return b;
    }

    static mindustry.gen.Building radar(mindustry.world.Block block, mindustry.game.Team team) {
        mindustry.gen.Building b = mindustry.gen.Building.create();
        b.block = block;
        b.team = team;
        b.efficiency = 0f;
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

    static mindustry.type.Planet planet(String name) {
        return new mindustry.type.Planet(name, null, 1f);
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
        copy.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))), b.version());
        check(copy.step == 100 && total(copy) == done && copy.found.get(junk) == b.found.get(junk));
        block.discoveryChance = 0f;
        OpenPitMine.OpenPitMineBuild unlucky = mine(block);
        run(unlucky, 600 * 120);
        check(unlucky.step == 100 && unlucky.found.isEmpty());
        // Every planet has its own list; unknown planets use the default one.
        mindustry.type.Planet home = planet("home"), other = planet("other");
        block.discoveryChance = 1f;
        block.byPlanet.put(home, new ItemStack[]{new ItemStack(never, 1)});
        Vars.state.rules.planet = home;
        OpenPitMine.OpenPitMineBuild local = mine(block);
        run(local, 6100);
        check(local.found.get(never) > 0 && local.found.get(good) == 0);
        Vars.state.rules.planet = other;
        OpenPitMine.OpenPitMineBuild abroad = mine(block);
        run(abroad, 6100);
        check(abroad.found.get(never) == 0 && abroad.found.get(good) > 0);
        block.byPlanet.put(other, new ItemStack[]{new ItemStack(good, 0)});
        OpenPitMine.OpenPitMineBuild barren = mine(block);
        run(barren, 6100);
        check(barren.found.isEmpty() && barren.step > 5);
        block.byPlanet.clear();
        // Radar: adjacent, own team, powered; several count once.
        mindustry.world.Block radar = new mindustry.world.Block("test-radar");
        block.radar = radar;
        OpenPitMine.OpenPitMineBuild fast = mine(block);
        check(fast.currentStepTime() == 600f);
        mindustry.gen.Building r1 = radar(radar, mindustry.game.Team.sharded), r2 = radar(radar, mindustry.game.Team.sharded);
        fast.proximity.add(r1);
        check(fast.currentStepTime() == 600f); // unpowered
        r1.efficiency = 1f;
        check(fast.currentStepTime() == 480f);
        r2.efficiency = 1f;
        fast.proximity.add(r2);
        check(fast.currentStepTime() == 480f);
        run(fast, 490);
        check(fast.step == 1);
        fast.proximity.clear();
        mindustry.gen.Building foe = radar(radar, mindustry.game.Team.crux);
        foe.efficiency = 1f;
        fast.proximity.add(foe);
        check(fast.currentStepTime() == 600f);
        // Water: only the share of the step dug with water counts.
        block.discoveryChance = 0f;
        block.boostedChance = 1f;
        OpenPitMine.OpenPitMineBuild wet = mine(block);
        wet.optionalEfficiency = 1f;
        run(wet, 300);
        wet.optionalEfficiency = 0f;
        run(wet, 200);
        check(Math.abs(wet.chance(wet.boostedProgress) - 0.5f) < 0.01f);
        wet.optionalEfficiency = 1f;
        run(wet, 6000);
        check(total(wet) > 0);
        // Saves from before the boosters still load.
        ByteArrayOutputStream legacy = new ByteArrayOutputStream();
        DataOutputStream old = new DataOutputStream(legacy);
        old.writeInt(42); old.writeFloat(0.5f); old.writeByte(0);
        OpenPitMine.OpenPitMineBuild veteran = mine(block);
        veteran.read(new Reads(new DataInputStream(new ByteArrayInputStream(legacy.toByteArray()))), (byte) 0);
        check(veteran.step == 42 && veteran.stepProgress == 0.5f && veteran.boostedProgress == 0f);
        block.boostedChance = 0.2f;
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
