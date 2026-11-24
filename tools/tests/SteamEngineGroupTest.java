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
    public static void main(String[] args) {
        Vars.content = new ContentLoader();
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
        check(Math.abs(c.liquids.get(steam) - 0.81f) < 0.0001f);
        b.proximity.remove(c, true); c.proximity.remove(b, true);
        check(consumer.efficiency(b) == 0f);
        check(Math.abs(c.liquids.get(steam) - 0.81f) < 0.0001f);
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
        System.out.println("OK: isolated, chained, cyclic, split and enemy engine groups");
    }
}
