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
        check(Math.abs(c.liquids.get(steam) - 0.8f) < 0.0001f);
        b.proximity.remove(c, true); c.proximity.remove(b, true);
        check(consumer.efficiency(b) == 0f);
        check(Math.abs(c.liquids.get(steam) - 0.8f) < 0.0001f);
        System.out.println("OK: isolated, chained, cyclic, split and enemy engine groups");
    }
}
