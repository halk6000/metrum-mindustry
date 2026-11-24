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

    public static void load() {
        SaveVersion.addCustomChunk("metrum-deposits-v1", new Chunk());
        Events.on(ResetEvent.class, e -> clear());
        Events.run(Trigger.draw, Deposits::drawHovered);
        Events.run(Trigger.update, () -> {
            if (Vars.state.isPlaying() && !Vars.net.client() && all.size > 0) update();
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
            if (d != null && (mined == null || d.item == mined)) total += d.amount;
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
        font.draw(Core.bundle.format("metrum.deposit.left", amount), x, y, Align.center);
        font.setColor(Color.white);
        font.getData().setScale(scaleX, scaleY);
        font.setUseIntegerPositions(ints);
        Draw.reset();
    }

    public static void clear() {
        all.clear();
        drillProgress.clear();
        unitTimers.clear();
        unitTiles.clear();
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
            old.amount += amount;
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
        Seq<Item> items = new Seq<>();
        int wanted = 0;
        for (Item item : Vars.content.items()) {
            if (found.get(item) <= 0 || oreFor(item) == null) continue;
            items.add(item);
            wanted += tilesFor(found.get(item), perTile);
        }
        Seq<Tile> ground = new Seq<>();
        for (int r = radius; ; r = Math.min(r + 4, maxRadius)) {
            ground.clear();
            for (int x = cx - r; x <= cx + r; x++) for (int y = cy - r; y <= cy + r; y++) {
                Tile t = Vars.world.tile(x, y);
                if (Mathf.within(cx, cy, x, y, r + 0.5f) && free(t)) ground.add(t);
            }
            if (ground.size >= wanted || r >= maxRadius) break;
        }
        for (int i = 0; i < items.size && !ground.isEmpty(); i++) {
            Item item = items.get(i);
            int amount = found.get(item);
            // leave room for the remaining finds
            int count = Mathf.clamp(tilesFor(amount, perTile), 1, Math.max(1, ground.size / (items.size - i)));
            Tile seed = ground.min(t -> Mathf.dst2(cx, cy, t.x, t.y));
            ground.sort(t -> Mathf.dst2(seed.x, seed.y, t.x, t.y));
            for (int k = 0; k < count; k++) {
                // spread the remainder so that the patch holds exactly the discovered amount
                place(ground.get(k), item, amount / count + (k < amount % count ? 1 : 0));
            }
            ground.removeRange(0, count - 1);
        }
    }

    static int tilesFor(int amount, int perTile) {
        return Math.max(1, Mathf.ceil(amount / (float) perTile));
    }

    /** Takes up to {@code amount} from the tile; strips its ore when nothing is left. Returns the amount taken. */
    public static int take(Tile tile, int amount) {
        Deposit d = all.get(tile.pos());
        if (d == null) return 0;
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

    public static class Chunk implements SaveFileReader.CustomChunk {
        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(all.size);
            for (IntMap.Entry<Deposit> e : all.entries()) {
                out.writeInt(e.key);
                out.writeUTF(e.value.item.name);
                out.writeInt(e.value.amount);
            }
        }

        @Override
        public void read(DataInput in) throws IOException {
            clear();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int pos = in.readInt();
                Item item = Vars.content.item(in.readUTF());
                int amount = in.readInt();
                if (item != null && amount > 0) all.put(pos, new Deposit(item, amount));
            }
        }
    }
}
