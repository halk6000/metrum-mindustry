package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.graphics.Layer;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Steam engine: burns steam into power and slowly warms up.
 * It is a non-rotating heat source, so EVERY adjacent block that needs heat gets it (no facing required).
 */
public class SteamEngine extends PowerGenerator {
    public float heatOutput = 6f;
    public float warmupSpeed = 0.004f;
    /** flat group bonus: applied once, regardless of how many engines are connected */
    public float groupOutputMultiplier = 1.1f, groupSteamMultiplier = 0.95f;

    public SteamEngine(String name) {
        super(name);
        rotate = false;
        hasLiquids = true;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("heat", (SteamEngineBuild b) -> new mindustry.ui.Bar("bar.heat", mindustry.graphics.Pal.lightOrange, () -> b.warmup));
    }

    public class SteamEngineBuild extends GeneratorBuild implements HeatBlock {
        public float warmup;
        /** recent steam satisfaction, smoothed so liquid arriving in bursts does not stall the engine */
        public float steamSupply;

        /** Current connected component; rebuilt on demand so splits cannot retain stale members. */
        public arc.struct.Seq<SteamEngineBuild> connectedEngines() {
            arc.struct.Seq<SteamEngineBuild> group = new arc.struct.Seq<>();
            arc.struct.ObjectSet<SteamEngineBuild> visited = new arc.struct.ObjectSet<>();
            group.add(this);
            visited.add(this);
            for (int index = 0; index < group.size; index++) {
                SteamEngineBuild current = group.get(index);
                for (mindustry.gen.Building neighbour : current.proximity) {
                    if (neighbour instanceof SteamEngineBuild && neighbour.block == block
                        && neighbour.team == team) {
                        SteamEngineBuild engine = (SteamEngineBuild) neighbour;
                        if (visited.add(engine)) group.add(engine);
                    }
                }
            }
            return group;
        }

        /** Any matching neighbour means a group of at least two; no need to walk the whole group. */
        public boolean grouped() {
            for (mindustry.gen.Building neighbour : proximity) {
                if (neighbour instanceof SteamEngineBuild && neighbour.block == block && neighbour.team == team) return true;
            }
            return false;
        }

        public float outputMultiplier() {
            return grouped() ? groupOutputMultiplier : 1f;
        }

        public float steamMultiplier() {
            return grouped() ? groupSteamMultiplier : 1f;
        }

        @Override
        public void updateTile() {
            // efficiency comes from the steam consumer; the engine heats up / cools down gradually
            steamSupply = Mathf.lerpDelta(steamSupply, Mathf.clamp(efficiency), 0.05f);
            warmup = Mathf.approachDelta(warmup, steamSupply, warmupSpeed);
            productionEfficiency = warmup * outputMultiplier();
            if (warmup > 0.5f && Mathf.chance(0.03 * Time.delta)) Fx.steam.at(x + Mathf.range(size * 3f), y + Mathf.range(size * 3f));
        }

        /** heat ramps up in steps: 3 -> 6 -> 9 -> 12 as the engine spins up */
        @Override
        public float heat() {
            if (warmup < 0.05f) return 0f;
            int stage = Math.min(4, 1 + (int) (warmup * 4f));
            return heatOutput / 4f * stage * outputMultiplier();
        }

        @Override
        public float heatFrac() {
            return warmup;
        }

        @Override
        public void draw() {
            super.draw();
            if (warmup <= 0.01f) return;
            Draw.z(Layer.blockAdditive);
            Draw.color(Color.valueOf("ff8a3c"), 0.35f * warmup + Mathf.absin(Time.time, 5f, 0.1f) * warmup);
            Fill.circle(x, y, size * 2.2f);
            Draw.reset();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            warmup = read.f();
            steamSupply = warmup;
        }
    }
}
