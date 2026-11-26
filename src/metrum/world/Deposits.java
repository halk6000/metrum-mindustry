package metrum.world;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.io.SaveFileReader;
import mindustry.io.SaveVersion;
import mindustry.type.Item;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.production.BurstDrill;
import mindustry.world.blocks.production.Drill;

/**
 * Exhaustible ore tiles. Ordinary drills and mining units use them like any ore;
 * every mined item lowers the stock and an empty tile loses its ore.
 * Only the server tracks mining; removed ore is synced through the normal overlay packet.
 */
public class Deposits {
    public static class Deposit {
        public final Item item;
        public int amount;
        public int layers = 1;

        Deposit(Item item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }

    /** packed tile position -> remaining stock */
    public static final IntMap<Deposit> all = new IntMap<>();
    /** shows the ore of a new deposit */
    public static arc.func.Cons2<Tile, Block> setOre = Tile::setOverlayNet;
    /** strips the ore from an exhausted tile */
    public static Cons<Tile> removeOre = tile -> tile.setOverlayNet(Blocks.air);

    private static IntFloatMap drillProgress = new IntFloatMap();
    private static IntFloatMap unitTimers = new IntFloatMap();
    private static IntIntMap unitTiles = new IntIntMap();
    /** packed drill position -> items per second when the sector was saved */
    static final IntFloatMap awayRates = new IntFloatMap();
    static int savedSeconds = -1;

    public static void load() {
        SaveVersion.addCustomChunk("metrum-deposits-v1", new Chunk());
        SaveVersion.addCustomChunk("metrum-deposits-away-v1", new AwayChunk());
        Events.on(ResetEvent.class, e -> clear());
        Events.run(Trigger.draw, Deposits::drawHovered);
        Events.run(Trigger.update, () -> {
            if (!Vars.state.isPlaying() || Vars.net.client()) return;
            if (awayRates.size > 0) {
                // drills have counted their ore by now
                if (Vars.state.isCampaign() && savedSeconds >= 0) mineAway(Vars.universe.seconds() - savedSeconds);
                awayRates.clear();
            }
            if (all.size > 0) update();
        });
    }

    /** Stock of the ore tile, or the stock left under the whole building standing on it; 0 = no deposit. */
    public static int remaining(Tile tile) {
        if (tile == null) return 0;
        if (tile.build == null || tile.build.tile == null) return all.containsKey(tile.pos()) ? all.get(tile.pos()).amount : 0;
        Item mined = tile.build instanceof Drill.DrillBuild ? ((Drill.DrillBuild) tile.build).dominantItem : null;
        int total = 0;
        for (Tile t : tile.build.tile.getLinkedTilesAs(tile.build.block, new Seq<>())) {
            Deposit d = all.get(t.pos());
            if (d != null && (mined == null || d.item == mined)) {
                if (d.amount == Integer.MAX_VALUE) return Integer.MAX_VALUE;
                total = (int)Math.min(Integer.MAX_VALUE, (long)total + d.amount);
            }
        }
        return total;
    }

    /** Shows the remaining stock above the hovered deposit tile or drill. */
    private static void drawHovered() {
        // clients only know the stock from the moment they joined, so they get no misleading number
        if (Vars.headless || !Vars.state.isGame() || Vars.net.client() || all.size == 0 || Core.scene.hasMouse()) return;
        Tile tile = Vars.world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
        int amount = remaining(tile);
        if (amount <= 0) return;
        float x = tile.build == null ? tile.worldx() : tile.build.x;
        float y = (tile.build == null ? tile.worldy() + Vars.tilesize / 2f : tile.build.y + tile.build.block.size * Vars.tilesize / 2f) + 6f;
        Draw.z(Layer.overlayUI);
        Font font = Fonts.outline;
        boolean ints = font.usesIntegerPositions();
        float scaleX = font.getScaleX(), scaleY = font.getScaleY();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f));
        font.setColor(Pal.accent);
        font.draw(Core.bundle.format("metrum.deposit.left", amount == Integer.MAX_VALUE ? "∞" : amount), x, y, Align.center);
        font.setColor(Color.white);
        font.getData().setScale(scaleX, scaleY);
        font.setUseIntegerPositions(ints);
        Draw.reset();
    }

    public static void clear() {
        awayRates.clear();
        savedSeconds = -1;
        all.clear();
        drillProgress.clear();
        unitTimers.clear();
        unitTiles.clear();
    }

    /** Charges the time spent on other sectors, at the rates the drills had when the player left. */
    public static void mineAway(int seconds) {
        if (seconds <= 0) return;
        for (IntFloatMap.Entry e : awayRates.entries()) {
            Tile tile = Vars.world.tile(e.key);
            if (tile == null || !(tile.build instanceof Drill.DrillBuild)) continue;
            Drill.DrillBuild drill = (Drill.DrillBuild) tile.build;
            if (drill.dominantItem != null) mined(drill, (int) Math.min((double) e.value * seconds, 100000));
        }
    }

    /** ore block dropping this item, or null when the item can't form a deposit */
    public static Block oreFor(Item item) {
        if (item == mindustry.content.Items.thorium && Blocks.oreCrystalThorium != null) return Blocks.oreCrystalThorium;
        return Vars.content.blocks().find(b -> b instanceof OreBlock && b.itemDrop == item && !((OreBlock) b).wallOre);
    }

    /** Puts a deposit on a tile without ore, or enlarges an existing deposit of the same item. */
    public static boolean place(Tile tile, Item item, int amount) {
        if (tile == null || amount <= 0) return false;
        Deposit old = all.get(tile.pos());
        if (old != null) {
            if (old.item != item) return false;
            if (old.amount == Integer.MAX_VALUE || old.layers >= 3) return false;
            old.amount = (int)Math.min(Integer.MAX_VALUE - 1L, (long)old.amount + amount);
            old.layers++;
            return true;
        }
        Block ore = oreFor(item);
        if (ore == null || tile.overlay() != Blocks.air) return false;
        setOre.get(tile, ore);
        all.put(tile.pos(), new Deposit(item, amount));
        return true;
    }

    /** Free ground that can hold a new deposit: no building, wall, liquid, vent or existing ore. */
    public static boolean free(Tile tile) {
        return tile != null && tile.block() == Blocks.air && tile.build == null && tile.overlay() == Blocks.air
            && !tile.floor().isLiquid && !tile.floor().isDeep() && tile.floor() != Blocks.empty && tile.floor() != Blocks.space
            && !(tile.floor() instanceof mindustry.world.blocks.environment.SteamVent);
    }

    /**
     * Scatters finds around a point as compact patches, so that ordinary drills fit on them.
     * The search widens up to {@code maxRadius} when there is little room; crowded finds get richer tiles instead of vanishing.
     */
    public static void scatter(int cx, int cy, arc.struct.ObjectIntMap<Item> found, int perTile, int radius, int maxRadius) {
        if (perTile <= 0) return;
        Seq<ScatterPatch> patches = new Seq<>();
        for (Item item : Vars.content.items()) {
            int amount = found.get(item);
            if (amount <= 0 || oreFor(item) == null) continue;
            ScatterPatch patch = new ScatterPatch(item, amount);
            Tile seed = nearestDeposit(item, cx, cy, radius);
            if (seed != null) collectPatch(seed, item, patch.tiles);
            patch.originalSize = patch.tiles.size;
            for (Tile tile : patch.tiles) {
                if (patch.remaining == 0) break;
                patch.add(tile, perTile);
            }
            patches.add(patch);
        }
        // Expand each mineral in turn so the first patch cannot consume all available ground.
        boolean expanded;
        do {
            expanded = false;
            for (ScatterPatch patch : patches) {
                if (patch.remaining == 0) continue;
                Tile next = nearestFree(cx, cy, maxRadius, patch.tiles);
                if (next == null || !patch.add(next, perTile)) continue;
                collectPatch(next, patch.item, patch.tiles);
                expanded = true;
            }
        } while (expanded);
        for (ScatterPatch patch : patches) {
            // Store the rest in a tile that already received this discovery, without adding a layer.
            if (patch.remaining > 0 && patch.stocked != null) {
                Deposit deposit = all.get(patch.stocked.pos());
                deposit.amount = (int)Math.min(Integer.MAX_VALUE - 1L, (long)deposit.amount + patch.remaining);
            }
            // Density is a growth bonus, not merely redistribution of the discovery.
            int n = patch.tiles.size;
            if (n > patch.originalSize && n >= 9) {
                int density = perTile + (n - 9) * (n + 6) / 2;
                for (Tile t : patch.tiles) {
                    Deposit d = all.get(t.pos());
                    d.amount = n >= 30 ? Integer.MAX_VALUE : Math.max(d.amount, density);
                }
            }
        }
    }

    private static class ScatterPatch {
        final Item item;
        final Seq<Tile> tiles = new Seq<>();
        int remaining, originalSize;
        Tile stocked;

        ScatterPatch(Item item, int amount) {
            this.item = item;
            remaining = amount;
        }

        boolean add(Tile tile, int perTile) {
            int amount = Math.min(remaining, perTile);
            if (!place(tile, item, amount)) return false;
            remaining -= amount;
            stocked = tile;
            return true;
        }
    }

    private static Tile nearestDeposit(Item item, int cx, int cy, int radius) {
        Tile best = null;
        float bestDistance = Float.MAX_VALUE;
        for (IntMap.Entry<Deposit> entry : all.entries()) {
            if (entry.value.item != item) continue;
            Tile tile = Vars.world.tile(entry.key);
            if (tile == null) continue;
            float distance = Mathf.dst2(cx, cy, tile.x, tile.y);
            if (distance <= radius * radius && distance < bestDistance) {
                best = tile;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Tile nearestFree(int cx, int cy, int radius, Seq<Tile> patch) {
        Tile best = null;
        float bestDistance = Float.MAX_VALUE;
        for (int x = cx - radius; x <= cx + radius; x++) for (int y = cy - radius; y <= cy + radius; y++) {
            Tile tile = Vars.world.tile(x, y);
            if (!free(tile) || !Mathf.within(cx, cy, x, y, radius + 0.5f)) continue;
            if (!patch.isEmpty() && !touches(tile, patch)) continue;
            float distance = Mathf.dst2(cx, cy, x, y);
            if (distance < bestDistance) {
                best = tile;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean touches(Tile t, Seq<Tile> patch) {
        for (Tile p : patch) if (Math.abs(t.x - p.x) + Math.abs(t.y - p.y) == 1) return true;
        return false;
    }

    private static void collectPatch(Tile seed, Item item, Seq<Tile> patch) {
        if (!patch.contains(seed)) patch.add(seed);
        for (int i = 0; i < patch.size; i++) {
            Tile t = patch.get(i);
            for (int j = 0; j < 4; j++) {
                Tile next = Vars.world.tile(t.x + (j == 0 ? 1 : j == 1 ? -1 : 0), t.y + (j == 2 ? 1 : j == 3 ? -1 : 0));
                if (next == null || patch.contains(next)) continue;
                Deposit d = all.get(next.pos());
                if (d != null && d.item == item) patch.add(next);
            }
        }
    }

    static int tilesFor(int amount, int perTile) {
        return Math.max(1, Mathf.ceil(amount / (float) perTile));
    }

    /** Takes up to {@code amount} from the tile; strips its ore when nothing is left. Returns the amount taken. */
    public static int take(Tile tile, int amount) {
        Deposit d = all.get(tile.pos());
        if (d == null) return 0;
        if (d.amount == Integer.MAX_VALUE) return amount;
        int taken = Math.min(amount, d.amount);
        d.amount -= taken;
        if (d.amount <= 0) {
            all.remove(tile.pos());
            removeOre.get(tile);
        }
        return taken;
    }

    public static void update() {
        ObjectSet<Drill.DrillBuild> drills = new ObjectSet<>();
        Seq<Integer> stale = new Seq<>();
        for (IntMap.Entry<Deposit> e : all.entries()) {
            Tile tile = Vars.world.tile(e.key);
            if (tile == null || tile.overlay().itemDrop != e.value.item) stale.add(e.key); // ore replaced from outside
            else if (tile.build instanceof Drill.DrillBuild) drills.add((Drill.DrillBuild) tile.build);
        }
        for (int pos : stale) all.remove(pos);

        IntFloatMap next = new IntFloatMap();
        Seq<Drill.DrillBuild> list = drills.toSeq();
        for (Drill.DrillBuild drill : list) {
            float now = drill.progress;
            // drill progress only ever falls when a batch has just been mined
            if (now < drillProgress.get(drill.id, -1f) && drill.dominantItem != null) {
                mined(drill, drill.block instanceof BurstDrill ? drill.dominantItems : 1);
            }
            next.put(drill.id, now);
        }
        drillProgress = next;
        updateUnits();
    }

    /** Charges mined items to the deposit tiles under the drill; natural ore tiles carry their share for free. */
    static void mined(Drill.DrillBuild drill, int count) {
        Seq<Tile> tiles = new Seq<>();
        int natural = 0;
        for (Tile t : drill.tile.getLinkedTilesAs(drill.block, new Seq<>())) {
            Deposit d = all.get(t.pos());
            if (d != null && d.item == drill.dominantItem) tiles.add(t);
            else if (t.drop() == drill.dominantItem) natural++;
        }
        for (int i = 0; i < count && !tiles.isEmpty(); i++) {
            if (natural > 0 && !Mathf.chance((double) tiles.size / (tiles.size + natural))) continue;
            Tile tile = tiles.random();
            take(tile, 1);
            if (!all.containsKey(tile.pos())) tiles.remove(tile);
        }
    }

    private static void updateUnits() {
        IntFloatMap timers = new IntFloatMap();
        IntIntMap tiles = new IntIntMap();
        for (Unit unit : Groups.unit) {
            Tile tile = unit.mineTile;
            if (tile == null || !all.containsKey(tile.pos())) continue;
            boolean same = unitTiles.get(unit.id, -1) == tile.pos();
            timers.put(unit.id, unit.mineTimer);
            tiles.put(unit.id, tile.pos());
            if (same && unit.mineTimer < unitTimers.get(unit.id, -1f)) take(tile, 1);
        }
        unitTimers = timers;
        unitTiles = tiles;
    }

    /** Universe time and drill rates at the moment of saving; kept apart so that older saves still load. */
    public static class AwayChunk implements SaveFileReader.CustomChunk {
        @Override
        public boolean writeNet() {
            return false;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            ObjectSet<Drill.DrillBuild> drills = new ObjectSet<>();
            for (IntMap.Entry<Deposit> e : all.entries()) {
                Tile tile = Vars.world.tile(e.key);
                if (tile != null && tile.build instanceof Drill.DrillBuild) drills.add((Drill.DrillBuild) tile.build);
            }
            out.writeInt(Vars.universe == null ? -1 : Vars.universe.seconds());
            out.writeInt(drills.size);
            for (Drill.DrillBuild d : drills) {
                out.writeInt(d.tile.pos());
                out.writeFloat(d.lastDrillSpeed * 60f * d.timeScale());
            }
        }

        @Override
        public void read(DataInput in) throws IOException {
            awayRates.clear();
            savedSeconds = in.readInt();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int pos = in.readInt();
                awayRates.put(pos, in.readFloat());
            }
        }
    }

    public static class Chunk implements SaveFileReader.CustomChunk {
        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(-all.size - 1);
            for (IntMap.Entry<Deposit> e : all.entries()) {
                out.writeInt(e.key);
                out.writeUTF(e.value.item.name);
                out.writeInt(e.value.amount);
                out.writeByte(e.value.layers);
            }
        }

        @Override
        public void read(DataInput in) throws IOException {
            all.clear();
            int count = in.readInt();
            boolean layered = count < 0;
            if (layered) count = -count - 1;
            for (int i = 0; i < count; i++) {
                int pos = in.readInt();
                Item item = Vars.content.item(in.readUTF());
                int amount = in.readInt();
                int layers = layered ? in.readUnsignedByte() : 1;
                if (item != null && amount > 0) {
                    Deposit d = new Deposit(item, amount);
                    d.layers = layers;
                    all.put(pos, d);
                }
            }
        }
    }
}
