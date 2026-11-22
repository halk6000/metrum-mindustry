package metrum.world;

import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Prints drone hulls. The player picks a hull type (config); the printer takes that hull's
 * materials and slowly prints it - the hull is drawn in the middle, fading in with progress.
 * Finished hulls are output as items, ready for part/weapon mounting.
 */
public class HullPrinter extends Block {
    public final ObjectMap<Item, ItemStack[]> recipes = new ObjectMap<>();
    public final Seq<Item> hulls = new Seq<>();
    public float craftTime = 60f * 20f;

    public HullPrinter(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 60;
        saveConfig = true;
        clearOnDoubleTap = true;
        config(Item.class, (HullPrinterBuild b, Item hull) -> {
            if (b.hull != hull) b.progress = 0f;
            b.hull = recipes.containsKey(hull) ? hull : null;
        });
        configClear((HullPrinterBuild b) -> {
            b.hull = null;
            b.progress = 0f;
        });
    }

    public HullPrinter hull(Item hull, ItemStack... cost) {
        recipes.put(hull, cost);
        hulls.add(hull);
        return this;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        stats.add(Stat.output, t -> {
            t.row();
            for (Item h : hulls) {
                t.image(h.uiIcon).size(32f).padRight(6f);
                t.add(h.localizedName).padRight(10f);
                for (ItemStack s : recipes.get(h)) {
                    t.image(s.item.uiIcon).size(20f);
                    t.add(String.valueOf(s.amount)).padRight(6f);
                }
                t.row();
            }
        });
    }

    public class HullPrinterBuild extends Building {
        public Item hull;
        public float progress;

        ItemStack[] cost() {
            return hull == null ? null : recipes.get(hull);
        }

        boolean hasMaterials() {
            ItemStack[] c = cost();
            if (c == null) return false;
            for (ItemStack s : c) if (items.get(s.item) < s.amount) return false;
            return true;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            ItemStack[] c = cost();
            if (c == null) return false;
            for (ItemStack s : c) if (s.item == item) return items.get(item) < s.amount * 2;
            return false;
        }

        @Override
        public boolean shouldConsume() {
            return hull != null && hasMaterials() && items.get(hull) < 10 && enabled;
        }

        @Override
        public void updateTile() {
            if (shouldConsume() && efficiency > 0f) {
                progress += getProgressIncrease(craftTime);
                if (progress >= 1f) {
                    for (ItemStack s : cost()) items.remove(s.item, s.amount);
                    items.add(hull, 1);
                    progress = 0f;
                    Fx.producesmoke.at(x, y);
                }
            }
            if (hull != null && items.get(hull) > 0) dump(hull);
        }

        @Override
        public boolean canDump(Building to, Item item) {
            return item == hull;
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(HullPrinter.this, table, hulls, () -> hull, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Object config() {
            return hull;
        }

        @Override
        public void draw() {
            super.draw();
            if (hull == null) return;
            Draw.z(Layer.blockOver);
            // preview of the hull being printed: faint outline -> solid as progress grows
            Draw.alpha(0.25f + 0.75f * progress);
            Draw.rect(hull.fullIcon, x, y, size * 6f, size * 6f);
            Draw.reset();
            if (progress > 0f) Drawf.light(x, y, size * 10f, hull.color, 0.4f * progress);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(hull == null ? -1 : hull.id);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            short id = read.s();
            hull = id < 0 ? null : mindustry.Vars.content.item(id);
            progress = read.f();
        }
    }
}
