package metrum.world;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/** Four production slots drawing from per-item buffers shared by all sides. */
public class AtomizerFactory extends Block {
    private static final int slots = 4, inputPerAtom = 2;
    public final ObjectMap<Item, Item> conversions = new ObjectMap<>();
    public final int capacity = 20;
    public final float craftTime = 30f;
    private Seq<Item> recipes;
    private TextureRegion bottom, top, ring, flash, chamber;
    private static final int[] vx = {-1,-1,1,1}, vy = {1,-1,1,-1};

    public AtomizerFactory(String name) {
        super(name);
        update = solid = hasItems = hasPower = true;
        itemCapacity = capacity;
        unloadable = false;
        buildType = FactoryBuild::new;
    }

    @Override public void init() {
        super.init();
        recipes = conversions.keys().toSeq();
        recipes.sort(item -> item.id);
    }

    @Override public void load() {
        super.load();
        bottom = Core.atlas.find(name + "-bottom");
        top = Core.atlas.find(name + "-top");
        ring = Core.atlas.find(name + "-ring");
        flash = Core.atlas.find(name + "-flash");
        chamber = Core.atlas.find(name + "-chamber");
    }

    @Override public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, 0.5f, StatUnit.seconds);
        stats.add(Stat.input, t -> {
            for (ObjectMap.Entry<Item, Item> e : conversions) {
                t.image(e.key.uiIcon).size(24f); t.add(" x2 -> ");
                t.image(e.value.uiIcon).size(24f); t.row();
            }
        });
    }

    public class FactoryBuild extends Building {
        private final Item[] material = new Item[slots], recentProduct = new Item[slots];
        private final float[] progress = new float[slots], glow = new float[slots];
        private int nextRecipe;

        private int reservedInputs(Item item) {
            int count = 0;
            for (Item active : material) if (active == item) count += inputPerAtom;
            return count;
        }

        private int reservedOutputs(Item item) {
            int count = 0;
            for (Item active : material) if (active != null && conversions.get(active) == item) count++;
            return count;
        }

        private Item takeRecipe() {
            for (int offset = 0; offset < recipes.size; offset++) {
                int index = (nextRecipe + offset) % recipes.size;
                Item input = recipes.get(index), output = conversions.get(input);
                if (items.get(input) - reservedInputs(input) < inputPerAtom
                    || items.get(output) + reservedOutputs(output) >= capacity) continue;
                nextRecipe = (index + 1) % recipes.size;
                return input;
            }
            return null;
        }

        private boolean ready(Item input) {
            return input != null && items.get(input) >= inputPerAtom && items.get(conversions.get(input)) < capacity;
        }

        public int activeSlots() {
            if (!enabled) return 0;
            int count = 0;
            for (Item input : material) if (ready(input)) count++;
            return count;
        }

        @Override public void updateConsumption() {
            if (enabled) {
                for (int slot = 0; slot < slots; slot++) {
                    if (material[slot] == null) material[slot] = takeRecipe();
                }
            }
            super.updateConsumption();
        }

        @Override public boolean shouldConsume() { return activeSlots() > 0; }

        @Override public boolean acceptItem(Building source, Item item) {
            return source != null && source.team == team && proximity.contains(source, true)
                && conversions.containsKey(item) && items.get(item) < capacity;
        }

        @Override public void handleItem(Building source, Item item) {
            if (acceptItem(source, item)) items.add(item, 1);
        }

        @Override public int acceptStack(Item item, int amount, mindustry.gen.Teamc source) { return 0; }

        @Override public boolean canDump(Building destination, Item item) {
            return destination.team == team && super.canDump(destination, item);
        }

        @Override public void updateTile() {
            for (int slot = 0; slot < slots; slot++) {
                glow[slot] = Math.max(0f, glow[slot] - delta() / 12f);
                Item input = material[slot];
                if (!enabled || efficiency <= 0f || !ready(input)) continue;
                progress[slot] += edelta() / craftTime;
                if (progress[slot] >= 1f) {
                    Item product = conversions.get(input);
                    items.remove(input, inputPerAtom);
                    items.add(product, 1);
                    material[slot] = null;
                    progress[slot] = 0f;
                    recentProduct[slot] = product;
                    glow[slot] = 1f;
                    Fx.smeltsmoke.at(x + vx[slot] * 5.5f, y + vy[slot] * 5.5f);
                }
            }
            for (Item input : recipes) {
                Item product = conversions.get(input);
                if (items.has(product)) dump(product);
            }
        }
        @Override public void draw() {
            if(!bottom.found() || !top.found()) { super.draw(); return; }
            Draw.rect(bottom,x,y,24f,24f);
            for(int i=0;i<slots;i++) {
                float px=x+vx[i]*5.5f, py=y+vy[i]*5.5f;
                if(chamber.found()) Draw.rect(chamber,px,py,10f,10f);
                if (material[i] != null || glow[i] > 0.35f) {
                    float p=Mathf.clamp(progress[i]);
                    if(ring.found()) Draw.rect(ring,px,py,8f,8f,p*360f);
                    Item shown=glow[i]>0.35f?recentProduct[i]:material[i];
                    float sz=glow[i]>0.35f?4f:5f*(1f-0.7f*p);
                    Draw.rect(shown.uiIcon,px,py,sz,sz);
                }
                if(glow[i]>0f && flash.found()) { Draw.alpha(glow[i]); Draw.rect(flash,px,py,9f,9f); Draw.reset(); }
            }
            Draw.rect(top,x,y,24f,24f); Draw.reset();
        }

        @Override public byte version() { return 1; }

        @Override public void write(Writes w) {
            super.write(w);
            for (int slot = 0; slot < slots; slot++) {
                w.s(material[slot] == null ? -1 : material[slot].id);
                w.f(progress[slot]);
            }
        }

        @Override public void read(Reads r, byte revision) {
            super.read(r, revision);
            java.util.Arrays.fill(material, null);
            java.util.Arrays.fill(progress, 0f);
            for (int slot = 0; slot < slots; slot++) {
                short id = r.s();
                if (revision == 0) {
                    // Legacy per-port counts are already in the building's saved item module.
                    r.i();
                    r.i();
                }
                float savedProgress = r.f();
                Item input = id < 0 ? null : Vars.content.item(id);
                if (input != null && conversions.containsKey(input) && savedProgress >= 0f && savedProgress < 1f
                    && items.get(input) - reservedInputs(input) >= inputPerAtom) {
                    material[slot] = input;
                    progress[slot] = savedProgress;
                }
            }
        }
    }
}
