import metrum.world.SteamEngine;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Team;

public class SteamEngineGroupTest {
    static SteamEngine.SteamEngineBuild engine(SteamEngine block, Team team) {
        SteamEngine.SteamEngineBuild b = block.new SteamEngineBuild();
        b.block = block;
        b.team = team;
        return b;
    }
    static void check(boolean condition) {
        if (!condition) throw new AssertionError("Incorrect steam engine group");
    }
    static boolean near(float x, float y) { return Math.abs(x - y) < 0.001f; }
    static float power(SteamEngine.SteamEngineBuild b) {
        b.warmup = 0.5f; // below the steam particle threshold
        ((SteamEngine) b.block).warmupSpeed = 0f;
        b.updateTile();
        b.warmup = 1f;
        return b.productionEfficiency * 2f;
    }
    /** steam drawn by one engine in one tick, with 1 unit in its own tank and others empty */
    static float used(SteamEngine.SteamEngineBuild b, mindustry.type.Liquid steam) {
        for (SteamEngine.SteamEngineBuild m : b.connectedEngines()) m.liquids.clear();
        b.liquids.add(steam, 1f);
        new metrum.world.SharedSteamConsumer(steam, 0.2f).update(b);
        return 1f - b.liquids.get(steam);
    }
    /** warmup reached after a long run; efficiency alternates between the two values */
    static float settled(SteamEngine.SteamEngineBuild b, float even, float odd) {
        ((SteamEngine) b.block).warmupSpeed = 0.004f;
        for (int i = 0; i < 2000; i++) {
            b.efficiency = i % 2 == 0 ? even : odd;
            if (b.warmup > 0.49f) b.warmup = 0.49f; // stay below the particle threshold
            b.updateTile();
        }
        return b.warmup;
    }
    public static void main(String[] args) {
        Vars.content = new ContentLoader();
        Vars.state = new mindustry.core.GameState();
        SteamEngine block = new SteamEngine("test-engine");
        SteamEngine.SteamEngineBuild a = engine(block, Team.sharded),
            b = engine(block, Team.sharded), c = engine(block, Team.sharded),
            enemy = engine(block, Team.crux);
        check(a.connectedEngines().size == 1);
        a.proximity.add(b); b.proximity.add(a);
        b.proximity.add(c); c.proximity.add(b);
        c.proximity.add(enemy); enemy.proximity.add(c);
        check(a.connectedEngines().size == 3 && c.connectedEngines().size == 3);
        // A cycle must not count engines twice.
        a.proximity.add(c); c.proximity.add(a);
        check(b.connectedEngines().size == 3);
        a.proximity.clear(); b.proximity.remove(a, true); c.proximity.remove(a, true);
        check(a.connectedEngines().size == 1 && b.connectedEngines().size == 2);
        mindustry.type.Liquid steam = new mindustry.type.Liquid("test-steam");
        for (SteamEngine.SteamEngineBuild member : new SteamEngine.SteamEngineBuild[]{a, b, c, enemy}) {
            member.liquids = new mindustry.world.modules.LiquidModule();
            member.efficiency = 1f;
        }
        arc.util.Time.delta = 1f;
        metrum.world.SharedSteamConsumer consumer = new metrum.world.SharedSteamConsumer(steam, 0.2f);
        c.liquids.add(steam, 1f);
        check(consumer.efficiency(b) == 1f);
        check(consumer.efficiency(a) == 0f);
        consumer.update(b);
        check(near(b.liquids.get(steam), 0.31f) && near(c.liquids.get(steam), 0.5f));
        // After a split nobody can reach the other half any more.
        b.proximity.remove(c, true); c.proximity.remove(b, true);
        b.liquids.clear();
        Vars.state.updateId++;
        check(consumer.efficiency(b) == 0f && near(c.liquids.get(steam), 0.5f));
        // Scarce steam piped into one end is shared equally and conserved.
        a.proximity.clear(); b.proximity.clear(); c.proximity.clear();
        a.proximity.add(b); b.proximity.add(a); b.proximity.add(c); c.proximity.add(b);
        SteamEngine.SteamEngineBuild[] row = {a, b, c};
        float[] burned = new float[3];
        for (SteamEngine.SteamEngineBuild m : row) m.liquids.clear();
        for (int tick = 0; tick < 100; tick++) {
            Vars.state.updateId++;
            c.liquids.add(steam, 0.2f);
            for (int k = 0; k < 3; k++) {
                consumer.efficiency(row[k]);
                float before = row[k].liquids.get(steam);
                consumer.update(row[k]);
                burned[k] += before - row[k].liquids.get(steam);
            }
        }
        check(near(burned[0], burned[1]) && near(burned[1], burned[2]));
        check(Math.abs(burned[0] + burned[1] + burned[2] - 20f) < 0.01f);
        // Bonus: lost after a split, flat for two or three engines, never shared with enemies.
        block.heatOutput = 12f;
        for (SteamEngine.SteamEngineBuild m : new SteamEngine.SteamEngineBuild[]{a, b, c, enemy}) { m.warmup = 1f; m.proximity.clear(); }
        c.proximity.add(enemy);
        check(near(c.heat(), 12f) && near(power(c), 1f) && near(used(c, steam), 0.2f));
        a.proximity.add(b); b.proximity.add(a);
        check(near(a.heat() + b.heat(), 26.4f) && near(power(a), 1.1f));
        check(near((used(a, steam) + used(b, steam)) * 60f, 22.8f));
        b.proximity.add(c); c.proximity.add(b);
        check(near(b.heat(), 13.2f) && near(power(b), 1.1f) && near(used(b, steam), 0.19f));
        // Output follows the steam actually supplied, also when it arrives in bursts.
        check(near(settled(a, 0.2f, 0.2f), 0.2f) && Math.abs(settled(a, 0.6f, 0f) - 0.3f) < 0.03f);
        check(settled(a, 1f, 1f) > 0.48f && near(settled(a, 0f, 0f), 0f));
        System.out.println("OK: isolated, chained, cyclic, split and enemy engine groups");
    }
}
