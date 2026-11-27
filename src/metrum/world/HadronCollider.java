package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/** Collides atomic items using the first available recipe. */
public class HadronCollider extends Block {
    public static class Recipe {
        public final Item a, b, output;
        public final int amount;

        public Recipe(Item a, Item b, Item output, int amount) {
            this.a = a;
            this.b = b;
            this.output = output;
            this.amount = amount;
        }

        boolean uses(Item item) {
            return item == a || item == b;
        }
    }

    public Seq<Recipe> recipes = new Seq<>();
    public float craftTime = 90f;
    public Effect craftEffect = Fx.massiveExplosion;
    public Color ringColor = Color.valueOf("78c8ff");

    public HadronCollider(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 20;
        buildType = HadronColliderBuild::new;
    }

    public HadronCollider recipe(Item a, Item b, Item output, int amount) {
        recipes.add(new Recipe(a, b, output, amount));
        return this;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        stats.add(Stat.input, table -> {
            table.row();
            for (Recipe r : recipes) {
                table.image(r.a.uiIcon).size(24f);
                table.add(" + ");
                table.image(r.b.uiIcon).size(24f);
                table.add(" -> ");
                table.image(r.output.uiIcon).size(24f).padRight(4f);
                table.add(r.output.localizedName + (r.amount > 1 ? " x" + r.amount : "")).left();
                table.row();
            }
        });
    }

    public class HadronColliderBuild extends Building {
        public float progress, spin;
        private Recipe activeRecipe;

        private boolean ready(Recipe r) {
            if (r == null || items == null) return false;
            return items.get(r.a) >= (r.a == r.b ? 2 : 1)
                && items.get(r.b) >= 1
                && items.get(r.output) + r.amount <= itemCapacity;
        }

        Recipe current() {
            if (ready(activeRecipe)) return activeRecipe;
            for (Recipe r : recipes) if (ready(r)) return r;
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (items.get(item) >= itemCapacity) return false;
            for (Recipe r : recipes) if (r.uses(item)) return true;
            return false;
        }

        @Override
        public boolean shouldConsume() {
            return enabled && current() != null;
        }

        @Override
        public void updateTile() {
            for (Recipe rec : recipes) {
                if (items.get(rec.output) > 0 && !rec.uses(rec.output)) dump(rec.output);
            }
            Recipe r = current();
            if (r != activeRecipe) {
                progress = 0f;
                activeRecipe = r;
            }
            if (enabled && r != null && efficiency > 0f) {
                spin += edelta() * 6f;
                progress += getProgressIncrease(craftTime);
                if (progress >= 1f && ready(r)) {
                    progress = 0f;
                    items.remove(r.a, 1);
                    items.remove(r.b, 1);
                    items.add(r.output, r.amount);
                    craftEffect.at(x, y, r.output.color);
                    dump(r.output);
                    activeRecipe = null;
                }
            }
        }

        @Override
        public float warmup() {
            return current() != null ? 1f : 0f;
        }

        @Override
        public void draw() {
            super.draw();
            if (current() == null) return;
            Draw.z(Layer.effect);
            Draw.color(ringColor, 0.8f);
            Lines.stroke(1.5f);
            float r = size * 2.6f;
            for (int i = 0; i < 2; i++) {
                float ang = spin * (i == 0 ? 1f : -1f);
                Fill.circle(x + Mathf.cosDeg(ang) * r, y + Mathf.sinDeg(ang) * r, 1.6f);
            }
            Lines.circle(x, y, r);
            Draw.color(Color.white, Mathf.absin(Time.time, 3f, 0.6f));
            Fill.circle(x, y, 1.5f + progress * 2f);
            Draw.reset();
        }

        @Override public void display(arc.scene.ui.layout.Table table) {
            MetrumBuildingUI.display(table,this,t -> {
                MetrumBuildingUI.section(t,"chambers");
                MetrumBuildingUI.chamber(t,"chamber1",()->current()==null?null:current().a,()->current()==null?0f:items.get(current().a)/(float)itemCapacity);
                MetrumBuildingUI.chamber(t,"chamber2",()->current()==null?null:current().b,()->current()==null?0f:items.get(current().b)/(float)itemCapacity);
                MetrumBuildingUI.chamber(t,"output",()->current()==null?null:current().output,()->current()==null?0f:items.get(current().output)/(float)itemCapacity);
                MetrumBuildingUI.progress(t,()->progress);
                MetrumBuildingUI.section(t,"diagnostics");
                MetrumBuildingUI.status(t,this,()->{
                    if(MetrumBuildingUI.noPower(this))return MetrumBuildingUI.tr("no-power");
                    if(current()!=null)return MetrumBuildingUI.tr("running");
                    for(Recipe r:recipes)if(items.get(r.a)>=(r.a==r.b?2:1)&&items.get(r.b)>=1)return MetrumBuildingUI.blocked(r.output);
                    return MetrumBuildingUI.tr("no-input");
                });
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
            read.f(); // Old saves do not identify the active recipe.
            progress = 0f;
            activeRecipe = null;
        }
    }
}
