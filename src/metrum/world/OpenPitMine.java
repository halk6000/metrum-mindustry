package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Open-pit mine: digs {@link #steps} steps; each one may reveal an underground deposit.
 * Discoveries add up. The finished pit collapses and scatters them as exhaustible deposits.
 */
public class OpenPitMine extends Block {
    public int steps = 100;
    public float stepTime = 60f * 10f;
    public float discoveryChance = 0.2f;
    /** discovery chance while the optional liquid is supplied */
    public float boostedChance = 0.24f;
    /** a powered block of this type next to the pit shortens every step; several count once */
    public Block radar;
    public float radarTimeSaved = 60f * 2f;
    public int minDiscovery = 50, maxDiscovery = 200;
    /** deposits left by the collapse: stock per tile and search radius in tiles */
    public int depositPerTile = 25, scatterRadius = 8, maxScatterRadius = 24;
    public boolean collapses = true;
    /** discoveries per planet; amounts are relative weights */
    public final ObjectMap<Planet, ItemStack[]> byPlanet = new ObjectMap<>();
    /** used on planets without their own list */
    public ItemStack[] discoveries = {};

    public OpenPitMine(String name) {
        super(name);
        update = true;
        solid = true;
        destructible = true;
        sync = true; // the server rolls discoveries, clients only display them
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, steps * stepTime / 60f, StatUnit.seconds);
        stats.add(Stat.booster, table -> {
            table.row();
            if (radar != null) {
                table.image(radar.uiIcon).size(24f).padRight(4f);
                table.add(arc.Core.bundle.format("metrum.pit.radar", arc.util.Strings.autoFixed(radarTimeSaved / 60f, 1))).left().row();
            }
            table.add(arc.Core.bundle.format("metrum.pit.water", Math.round(discoveryChance * 100f), Math.round(boostedChance * 100f))).left().colspan(2).row();
        });
        stats.add(Stat.output, table -> {
            table.row();
            for (ObjectMap.Entry<Planet, ItemStack[]> e : byPlanet) {
                int total = Math.max(1, totalWeight(e.value));
                table.add("[accent]" + e.key.localizedName + "[]").left().colspan(2).padTop(6f).row();
                for (ItemStack s : e.value) {
                    if (s.amount <= 0) continue;
                    table.image(s.item.uiIcon).size(24f).padRight(4f);
                    table.add(s.item.localizedName + " " + Math.round(100f * s.amount / total) + "%").left();
                    table.row();
                }
            }
        });
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("metrum-pit", (OpenPitMineBuild b) -> new mindustry.ui.Bar(
            () -> arc.Core.bundle.format("metrum.pit.progress", b.step * 100 / steps), () -> Pal.ammo, () -> b.fraction()));
        addBar("metrum-pit-step", (OpenPitMineBuild b) -> new mindustry.ui.Bar(
            () -> arc.Core.bundle.format("metrum.pit.step", arc.util.Strings.autoFixed(b.currentStepTime() / 60f, 1), Math.round(b.chance(b.optionalEfficiency) * 100f)),
            () -> Pal.accent, () -> b.stepProgress));
    }

    /** discoveries possible on the current planet */
    public ItemStack[] available() {
        Planet planet = Vars.state.rules.sector != null ? Vars.state.rules.sector.planet : Vars.state.rules.planet;
        return planet == null ? discoveries : byPlanet.get(planet, discoveries);
    }

    static int totalWeight(ItemStack[] list) {
        int total = 0;
        for (ItemStack s : list) total += s.amount;
        return total;
    }

    /** weighted choice, or null when every weight is zero */
    static Item randomItem(ItemStack[] list) {
        int total = totalWeight(list);
        if (total <= 0) return null;
        int roll = Mathf.random(total - 1);
        for (ItemStack s : list) {
            roll -= s.amount;
            if (roll < 0) return s.item;
        }
        return null;
    }

    public class OpenPitMineBuild extends Building {
        public int step;
        public float stepProgress;
        /** part of the current step dug with the liquid booster */
        public float boostedProgress;
        public final ObjectIntMap<Item> found = new ObjectIntMap<>();

        public float fraction() {
            return Mathf.clamp((step + stepProgress) / steps);
        }

        /** power satisfaction of the best adjacent radar, 0 without one */
        public float radarEfficiency() {
            float best = 0f;
            for (Building other : proximity) {
                if (other.block == radar && other.team == team) best = Math.max(best, other.efficiency);
            }
            return Mathf.clamp(best);
        }

        public float currentStepTime() {
            return Math.max(1f, stepTime - radarTimeSaved * radarEfficiency());
        }

        public float chance(float boost) {
            return Mathf.lerp(discoveryChance, boostedChance, Mathf.clamp(boost));
        }

        @Override
        public boolean shouldConsume() {
            return step < steps;
        }

        @Override
        public void updateTile() {
            if (step >= steps) {
                if (collapses && !Vars.net.client()) collapse();
                return;
            }
            float advance = delta() / currentStepTime();
            stepProgress += advance;
            boostedProgress += advance * optionalEfficiency;
            if (stepProgress < 1f) return;
            if (Vars.net.client()) { // wait for the server's result
                stepProgress = 1f;
                return;
            }
            stepProgress -= 1f;
            step++;
            if (step >= steps) stepProgress = 0f;
            Item item = Mathf.chance(chance(boostedProgress)) ? randomItem(available()) : null;
            boostedProgress = 0f;
            if (item != null) {
                found.increment(item, 0, Mathf.random(minDiscovery, maxDiscovery));
            }
        }

        /** Removes the pit without leaving a rebuild plan, then reveals the deposits. */
        void collapse() {
            int cx = tile.x, cy = tile.y;
            mindustry.gen.Call.effect(mindustry.content.Fx.reactorExplosion, x, y, 0f, Color.valueOf("8a6a52"));
            tile.removeNet();
            Deposits.scatter(cx, cy, found, depositPerTile, scatterRadius, maxScatterRadius);
        }

        @Override
        public float progress() {
            return fraction();
        }

        @Override public void display(Table table) {
            MetrumBuildingUI.display(table, this, this::panelModules);
        }
        private void panelModules(Table t) {
            MetrumBuildingUI.section(t,"excavation");
            MetrumBuildingUI.bar(t, () -> arc.Core.bundle.format("metrum.pit.progress", Math.round(fraction()*100f)), () -> Pal.ammo, this::fraction);
            MetrumBuildingUI.progress(t, () -> stepProgress);
            MetrumBuildingUI.value(t,"step-time", () -> arc.util.Strings.autoFixed(currentStepTime()/60f,1)+" s");
            MetrumBuildingUI.value(t,"discovery", () -> Math.round(chance(optionalEfficiency)*100f)+"%");
            MetrumBuildingUI.section(t,"boosters");
            MetrumBuildingUI.bar(t, () -> "Radar: "+(radarEfficiency()>0f?"ON":"OFF"), () -> radarEfficiency()>0f?Color.green:Color.scarlet, this::radarEfficiency);
            MetrumBuildingUI.bar(t, () -> mindustry.content.Liquids.water.localizedName+": "+Math.round(optionalEfficiency*100f)+"%", () -> mindustry.content.Liquids.water.color, () -> optionalEfficiency);
            t.table(list -> {
                String[] shown = {null};
                list.left();
                list.update(() -> {
                    String state = found.toString();
                    if (state.equals(shown[0])) return;
                    shown[0] = state;
                    list.clearChildren();
                    list.add(arc.Core.bundle.get(found.isEmpty() ? "metrum.pit.empty" : "metrum.pit.found")).color(Pal.accent).left().colspan(2).row();
                    for (Item item : Vars.content.items()) {
                        int amount = found.get(item);
                        if (amount <= 0) continue;
                        list.image(item.uiIcon).size(24f).padRight(6f);
                        list.add(item.localizedName + ": " + amount).left().row();
                    }
                });
            }).growX().left().row();
            MetrumBuildingUI.section(t,"diagnostics");
            MetrumBuildingUI.status(t,this,()->MetrumBuildingUI.tr(step>=steps?"complete":"running"));
        }

        @Override
        public void draw() {
            super.draw();
            // the pit gets deeper: dark terraces grow from the centre
            float radius = size * Vars.tilesize * 0.32f * fraction();
            for (int i = 0; i < 3; i++) {
                Draw.color(Color.valueOf("17110f"), 0.35f);
                Fill.poly(x, y, 8, radius * (1f - i * 0.28f), 22.5f);
            }
            Draw.reset();
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(step);
            write.f(stepProgress);
            write.f(boostedProgress);
            Seq<Item> keys = new Seq<>();
            for (Item item : Vars.content.items()) if (found.get(item) > 0) keys.add(item);
            write.b(keys.size);
            for (Item item : keys) {
                write.str(item.name);
                write.i(found.get(item));
            }
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            step = read.i();
            stepProgress = read.f();
            boostedProgress = revision >= 1 ? read.f() : 0f;
            found.clear();
            int count = read.b();
            for (int i = 0; i < count; i++) {
                Item item = Vars.content.item(read.str());
                int amount = read.i();
                if (item != null) found.put(item, amount);
            }
        }
    }
}
