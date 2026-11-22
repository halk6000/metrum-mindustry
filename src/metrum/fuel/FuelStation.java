package metrum.fuel;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.struct.ObjectIntMap;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Passively refuels friendly units within {@link #range}.
 * Burns the best fuel liquid it holds; the liquid decides the speed boost units get.
 */
public class FuelStation extends Block {
    /** fuel liquid -> quality (1..3) */
    public ObjectIntMap<Liquid> fuels = new ObjectIntMap<>();
    public float range = 8f * Vars.tilesize;
    /** fuel units pumped per tick into each unit */
    public float pumpRate = 1f;
    /** liquid used per fuel unit */
    public float liquidPerFuel = 0.05f;

    public FuelStation(String name) {
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 100f;
        size = 2;
    }

    public FuelStation fuel(Liquid liquid, int quality) {
        fuels.put(liquid, quality);
        return this;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, range / Vars.tilesize, StatUnit.blocks);
        stats.add(Stat.input, t -> {
            t.row();
            for (ObjectIntMap.Entry<Liquid> e : fuels) {
                t.image(e.key.uiIcon).size(24f).padRight(4f);
                t.add(e.key.localizedName + "  +" + (e.value == 1 ? 10 : e.value == 2 ? 30 : 50) + "%").left();
                t.row();
            }
        });
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * Vars.tilesize + offset, y * Vars.tilesize + offset, range, Pal.accent);
    }

    public class FuelStationBuild extends Building {
        public float pulse;

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return fuels.containsKey(liquid) && liquids.get(liquid) < liquidCapacity;
        }

        /** best stocked fuel, or null */
        Liquid best() {
            Liquid best = null;
            int bq = 0;
            for (ObjectIntMap.Entry<Liquid> e : fuels) {
                if (liquids.get(e.key) >= liquidPerFuel && e.value > bq) {
                    best = e.key;
                    bq = e.value;
                }
            }
            return best;
        }

        @Override
        public void updateTile() {
            Liquid fuel = best();
            if (fuel == null || !enabled) return;
            int q = fuels.get(fuel, 1);
            float amount = pumpRate * edelta();
            Units.nearby(team, x, y, range, u -> {
                if (!FuelSystem.usesFuel(u)) return;
                float can = Math.min(amount, liquids.get(fuel) / liquidPerFuel);
                if (can <= 0f) return;
                float used = FuelSystem.refuel(u, can, q);
                if (used > 0f) {
                    liquids.remove(fuel, used * liquidPerFuel);
                    pulse = 1f;
                }
            });
            pulse = Math.max(pulse - 0.02f * edelta(), 0f);
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public void draw() {
            super.draw();
            if (pulse > 0f) {
                Draw.color(FuelSystem.barColor, pulse * 0.5f);
                Lines.stroke(1.2f);
                Lines.circle(x, y, range * (1f - pulse * 0.5f));
                Draw.reset();
            }
        }
    }
}
