import arc.util.Time;
import metrum.world.ColorDuctBridge;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tiles;
import mindustry.world.blocks.distribution.DirectionalUnloader.DirectionalUnloaderBuild;
import mindustry.world.modules.ItemModule;

public class BridgeFlowTest {
    static Building place(Block block, int x, int y, int rotation) {
        Building building = block.buildType.get();
        building.block = block;
        building.team = Team.sharded;
        building.tile = Vars.world.tile(x, y);
        building.tile.build = building;
        building.rotation = rotation;
        building.items = new ItemModule();
        building.efficiency = 1f;
        return building;
    }

    public static void main(String[] args) {
        Vars.headless = true;
        Vars.content = new ContentLoader();
        Vars.state = new GameState();
        Vars.content.createBaseContent();
        Vars.world = new World();
        Vars.world.tiles = new Tiles(14, 8);
        Vars.world.tiles.fill();
        Time.delta = 1f;
        ColorDuctBridge bridge = new ColorDuctBridge("flow");
        bridge.range = 4;
        bridge.speed = 4f;
        bridge.init();
        Blocks.ductUnloader.init();
        Blocks.container.init();

        int[] dx = {1, 0, -1, 0};
        int[] dy = {0, 1, 0, -1};
        for (int rotation = 0; rotation < 4; rotation++) {
            Building intake = place(bridge, 10, 4, rotation);
            place(bridge, 10 + 3 * dx[rotation], 4 + 3 * dy[rotation], rotation);
            for (int side = 0; side < 4; side++) {
                Building source = place(Blocks.duct, 10 + dx[side], 4 + dy[side], (side + 2) % 4);
                boolean expected = side != (rotation + 2) % 4;
                if (intake.acceptItem(source, Items.graphite) != expected) {
                    throw new AssertionError("Incorrect intake for rotation " + rotation + ", side " + side);
                }
            }
        }
        checkFlow(bridge);
        Blocks.ductBridge.init();
        checkFlow(Blocks.ductBridge);
    }

    static void checkFlow(Block bridge) {
        Vars.world.tiles.fill();
        Building feed = place(Blocks.container, 2, 1, 1);
        Building start = place(bridge, 2, 2, 0);
        Building middle = place(bridge, 5, 2, 0);
        Building end = place(bridge, 8, 2, 0);
        Building forward = place(Blocks.container, 9, 2, 0);
        Building side = place(Blocks.container, 5, 4, 0);
        DirectionalUnloaderBuild unloader = (DirectionalUnloaderBuild) place(Blocks.ductUnloader, 5, 3, 1);
        unloader.unloadItem = Items.graphite;

        // Leave room for five graphite, then let the full receiver divert the next five.
        int ballast = side.getMaximumAccepted(Items.graphite) - 5;
        side.items.add(Items.graphite, ballast);
        for (int phase = 0; phase < 2; phase++) {
            if (phase == 1) side.items.remove(Items.graphite, ballast);
            int sent = 0;
            for (int tick = 0; tick < 400; tick++) {
                Item item = sent % 2 == 0 ? Items.graphite : Items.beryllium;
                if (sent < 20 && start.acceptItem(feed, item)) {
                    start.handleItem(feed, item);
                    sent++;
                }
                start.updateTile();
                middle.updateTile();
                end.updateTile();
                unloader.updateTile();
            }
            int storedBallast = phase == 0 ? ballast : 0;
            int diverted = side.items.get(Items.graphite) - storedBallast;
            int batches = phase + 1;
            System.out.println(bridge.name + " phase " + phase + ": side graphite "
                + diverted
                + ", forward graphite " + forward.items.get(Items.graphite));
            if (bridge instanceof ColorDuctBridge && diverted != (phase == 0 ? 5 : 15)) {
                throw new AssertionError("Circuit bridge lost side priority");
            }
            if (sent != 20 || diverted <= 0 || side.items.total() != diverted + storedBallast
                || diverted + forward.items.get(Items.graphite) != 10 * batches
                || forward.items.get(Items.beryllium) != 10 * batches
                || start.items.total() + middle.items.total() + end.items.total() != 0) {
                throw new AssertionError("Incorrect delivery after phase " + phase);
            }
        }
    }
}
