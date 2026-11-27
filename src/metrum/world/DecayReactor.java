package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Decay Reactor: lets quark matter decay safely. Steady power, no meltdown;
 * every finished decay drops random resources (weighted).
 */
public class DecayReactor extends PowerGenerator {
    public Item fuel;
    /** premium fuel (quark matter): still 1 per decay, but {@link #premiumMultiplier} x the resources */
    public Item premiumFuel;
    public int premiumMultiplier = 3;
    public float decayTime = 60f * 10f;
    public final Seq<ItemStack> products = new Seq<>(); // amount = weight
    public int rolls = 3;

    public DecayReactor(String name) {
        super(name);
        hasItems = true;
        itemCapacity = 30;
    }

    public DecayReactor product(Item item, int weight) {
        products.add(new ItemStack(item, weight));
        return this;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, decayTime / 60f, StatUnit.seconds);
        stats.add(Stat.output, t -> {
            t.row();
            for (ItemStack s : products) t.image(s.item.uiIcon).size(24f).padRight(4f);
        });
    }

    public class DecayReactorBuild extends GeneratorBuild {
        public float progress;

        @Override
        public boolean acceptItem(mindustry.gen.Building source, Item item) {
            return (item == fuel || item == premiumFuel) && items.get(item) < itemCapacity;
        }

        Item roll() {
            int total = 0;
            for (ItemStack s : products) total += s.amount;
            int r = Mathf.random(total - 1);
            for (ItemStack s : products) {
                if ((r -= s.amount) < 0) return s.item;
            }
            return products.first().item;
        }

        @Override
        public void updateTile() {
            Item using = premiumFuel != null && items.get(premiumFuel) > 0 ? premiumFuel : fuel;
            boolean running = items.get(using) > 0 && enabled;
            productionEfficiency = Mathf.approachDelta(productionEfficiency, running ? 1f : 0f, 0.01f);
            if (running) {
                progress += Time.delta / decayTime;
                if (progress >= 1f) {
                    progress = 0f;
                    items.remove(using, 1);
                    int n = using == premiumFuel ? rolls * premiumMultiplier : rolls;
                    for (int i = 0; i < n; i++) {
                        Item it = roll();
                        if (items.get(it) < itemCapacity) items.add(it, 1);
                    }
                    Fx.producesmoke.at(x, y);
                }
            }
            for (ItemStack s : products) if (items.get(s.item) > 0) dump(s.item);
        }

        @Override
        public boolean canDump(mindustry.gen.Building to, Item item) {
            return item != fuel && item != premiumFuel;
        }

        @Override
        public void draw() {
            super.draw();
            Draw.z(Layer.blockAdditive);
            Draw.color(Color.valueOf("7cff9a"), 0.3f * productionEfficiency + Mathf.absin(Time.time, 6f, 0.1f) * productionEfficiency);
            Fill.circle(x, y, size * 2.5f);
            Draw.reset();
            Drawf.light(x, y, size * 22f, Color.valueOf("7cff9a"), 0.5f * productionEfficiency);
        }

        @Override public void display(arc.scene.ui.layout.Table table) {
            MetrumBuildingUI.display(table,this,t -> {
                MetrumBuildingUI.section(t,"reactor");
                MetrumBuildingUI.chamber(t,"fuel",()->premiumFuel!=null&&items.get(premiumFuel)>0?premiumFuel:fuel,()->items.get(premiumFuel!=null&&items.get(premiumFuel)>0?premiumFuel:fuel)/(float)itemCapacity);
                MetrumBuildingUI.value(t,"yield",()->Integer.toString(premiumFuel!=null&&items.get(premiumFuel)>0?rolls*premiumMultiplier:rolls));
                MetrumBuildingUI.progress(t,()->progress);
                MetrumBuildingUI.section(t,"diagnostics");
                MetrumBuildingUI.status(t,this,()->MetrumBuildingUI.tr(items.get(fuel)>0||(premiumFuel!=null&&items.get(premiumFuel)>0)?"running":"no-input"));
            });
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
