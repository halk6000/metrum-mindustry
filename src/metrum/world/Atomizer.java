package metrum.world;

import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Converts any supported mineral into its atomic form (one recipe per input item).
 * Feeds the hadron collider.
 */
public class Atomizer extends Block {
    /** input mineral -> atomic form */
    public ObjectMap<Item, Item> conversions = new ObjectMap<>();
    public float craftTime = 60f;
    public int inputAmount = 2;
    public Effect craftEffect = Fx.smeltsmoke;

    public Atomizer(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 20;
    }

    public Atomizer convert(Item from, Item to) {
        conversions.put(from, to);
        return this;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        stats.add(Stat.input, table -> {
            table.row();
            for (ObjectMap.Entry<Item, Item> e : conversions) {
                table.image(e.key.uiIcon).size(24f).padRight(4f);
                table.add("x" + inputAmount + " -> ");
                table.image(e.value.uiIcon).size(24f).padRight(4f);
                table.add(e.value.localizedName).left();
                table.row();
            }
        });
    }

    public class AtomizerBuild extends Building {
        public float progress;

        /** first stored input that has a recipe and enough amount */
        Item current() {
            for (ObjectMap.Entry<Item, Item> e : conversions) {
                if (items.get(e.key) >= inputAmount && items.get(e.value) < itemCapacity) return e.key;
            }
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return conversions.containsKey(item) && items.get(item) < itemCapacity;
        }

        @Override
        public boolean shouldConsume() {
            return current() != null && enabled;
        }

        @Override
        public void updateTile() {
            Item in = current();
            if (in != null && efficiency > 0f) {
                progress += getProgressIncrease(craftTime);
                if (progress >= 1f) {
                    progress %= 1f;
                    items.remove(in, inputAmount);
                    items.add(conversions.get(in), 1);
                    craftEffect.at(x, y);
                }
            }
            // push out atomic forms only, never the raw inputs
            for (Item out : conversions.values()) {
                if (items.get(out) > 0 && timer(timerDump, dumpTime / 4f)) dump(out);
            }
        }

        @Override
        public float warmup() {
            return current() != null ? 1f : 0f;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
        }
    }
}
