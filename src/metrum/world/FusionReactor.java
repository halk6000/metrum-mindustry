package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Fusion Reactor: burns quark matter, needs heavy cooling.
 * Warms up to 100%; at full power: {@link #coolantPerSecond} coolant/s, produces power, heat and steam.
 * Without coolant for {@link #heatupDelay} it starts glowing red; after {@link #meltdownTime} it explodes.
 */
public class FusionReactor extends PowerGenerator {
    public Item fuel;
    public float fuelTime = 60f * 10f;
    public Liquid coolant;
    public float coolantPerSecond = 90f;
    public Liquid steam;
    public float steamPerSecond = 30f;
    public float heatOutput = 20f;
    public float warmupSpeed = 0.002f;
    public float heatupDelay = 60f * 5f, meltdownTime = 60f * 30f;
    public float explosionRadiusTiles = 50f, explosionDamage = 1500f;
    /** at 100% output: atom -> next heavier atom (up the periodic table) */
    public final arc.struct.ObjectMap<Item, Item> transmute = new arc.struct.ObjectMap<>();
    public float transmuteTime = 120f;

    public FusionReactor(String name) {
        super(name);
        hasItems = true;
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 400f;
        itemCapacity = 20;
        rotateDraw = false;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.input, coolantPerSecond, StatUnit.liquidSecond);
        stats.add(Stat.output, steamPerSecond, StatUnit.liquidSecond);
        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
        stats.add(Stat.productionTime, fuelTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("metrum-warmup", (FusionReactorBuild b) -> new mindustry.ui.Bar(
            () -> arc.Core.bundle.format("metrum.reactor.output", (int) (b.warmup * 100)), () -> Color.valueOf("ff50c8"), () -> b.warmup));
        addBar("metrum-meltdown", (FusionReactorBuild b) -> new mindustry.ui.Bar(
            () -> arc.Core.bundle.get("metrum.reactor.meltdown"), () -> Color.scarlet, () -> b.dryTime / (heatupDelay + meltdownTime)));
    }

    public class FusionReactorBuild extends GeneratorBuild implements HeatBlock {
        public float warmup, fuelProgress, dryTime;

        public float transmuteProgress;

        @Override
        public boolean acceptItem(mindustry.gen.Building source, Item item) {
            if (item == fuel) return items.get(fuel) < itemCapacity;
            return warmup >= 0.99f && transmute.containsKey(item) && items.get(item) < itemCapacity;
        }

        void updateTransmute() {
            if (warmup < 0.99f) return;
            for (arc.struct.ObjectMap.Entry<Item, Item> e : transmute) {
                if (items.get(e.key) <= 0) continue;
                transmuteProgress += Time.delta / transmuteTime;
                if (transmuteProgress >= 1f) {
                    transmuteProgress = 0f;
                    items.remove(e.key, 1);
                    items.add(e.value, 1);
                    Fx.smeltsmoke.at(x, y, e.value.color);
                }
                return;
            }
        }

        @Override
        public boolean acceptLiquid(mindustry.gen.Building source, Liquid liquid) {
            return liquid == coolant && liquids.get(coolant) < liquidCapacity;
        }

        @Override
        public void updateTile() {
            boolean fuelled = items.get(fuel) > 0 && enabled;
            warmup = Mathf.approachDelta(warmup, fuelled ? 1f : 0f, warmupSpeed);

            if (fuelled) {
                fuelProgress += Time.delta * warmup / fuelTime;
                if (fuelProgress >= 1f) {
                    fuelProgress = 0f;
                    items.remove(fuel, 1);
                }
            }

            // cooling: needs coolantPerSecond * warmup
            float need = coolantPerSecond / 60f * warmup * Time.delta;
            if (warmup > 0.05f) {
                if (liquids.get(coolant) >= need) {
                    liquids.remove(coolant, need);
                    dryTime = Math.max(dryTime - Time.delta * 3f, 0f);
                    // steam by-product
                    liquids.add(steam, Math.min(steamPerSecond / 60f * warmup * Time.delta, liquidCapacity - liquids.get(steam)));
                } else {
                    dryTime += Time.delta;
                }
            } else {
                dryTime = Math.max(dryTime - Time.delta, 0f);
            }
            if (liquids.get(steam) > 0.01f) dumpLiquid(steam);

            productionEfficiency = warmup;
            updateTransmute();
            for (Item out : transmute.values()) if (items.get(out) > 0 && !transmute.containsKey(out)) dump(out);
            if (dryTime > heatupDelay && Mathf.chance(0.08 * Time.delta)) Fx.smoke.at(x + Mathf.range(size * 4f), y + Mathf.range(size * 4f));
            if (dryTime >= heatupDelay + meltdownTime) meltdown();
        }

        void meltdown() {
            float r = explosionRadiusTiles * mindustry.Vars.tilesize;
            Damage.damage(team, x, y, r, explosionDamage);
            Fx.reactorExplosion.at(x, y);
            Fx.impactReactorExplosion.at(x, y);
            Sounds.explosionReactor.at(x, y);
            mindustry.Vars.renderer.shake(20f, 60f);
            kill();
        }

        @Override
        public float heat() {
            return heatOutput * warmup;
        }

        @Override
        public float heatFrac() {
            return warmup;
        }

        @Override
        public void draw() {
            super.draw();
            float danger = Mathf.clamp((dryTime - heatupDelay) / meltdownTime);
            Draw.z(Layer.blockAdditive);
            Draw.color(Color.valueOf("ff50c8"), Color.scarlet, danger);
            Draw.alpha(0.25f + 0.5f * Math.max(warmup, danger) + Mathf.absin(Time.time, 4f, 0.15f));
            Fill.circle(x, y, size * 2.2f + danger * 3f);
            Draw.reset();
            Drawf.light(x, y, size * 30f, danger > 0 ? Color.scarlet : Color.valueOf("ff50c8"), 0.6f * Math.max(warmup, danger));
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(warmup);
            write.f(fuelProgress);
            write.f(dryTime);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            warmup = read.f();
            fuelProgress = read.f();
            dryTime = read.f();
        }
    }
}
