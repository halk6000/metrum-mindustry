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
        System.out.println("OK: double iron, single sand, storage boundaries");
    }
}

