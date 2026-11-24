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
        System.out.println("OK: isolated, chained, cyclic, split and enemy engine groups");
    }
}
