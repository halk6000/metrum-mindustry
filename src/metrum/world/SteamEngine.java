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
import mindustry.world.blocks.heat.HeatConsumer;
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
        addBar("heat", (SteamEngineBuild b) -> new mindustry.ui.Bar(
            () -> arc.Core.bundle.format("metrum.steamengine.heat", arc.util.Strings.autoFixed(b.groupHeat(), 1), b.connectedEngines().size),
            () -> mindustry.graphics.Pal.lightOrange, () -> b.warmup));
    }

    public class SteamEngineBuild extends GeneratorBuild implements HeatBlock {
        public float warmup;
        /** recent steam satisfaction, smoothed so liquid arriving in bursts does not stall the engine */
        public float steamSupply;
        /** tick of the last steam levelling, see SharedSteamConsumer */
        public long lastBalance = -1;
        /** heat handed to each neighbour, recomputed for the whole group once per tick */
        private float sharedHeat;
        private long lastHeat = -1;

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
        public float ownHeat() {
            if (warmup < 0.05f) return 0f;
            int stage = Math.min(4, 1 + (int) (warmup * 4f));
            return heatOutput / 4f * stage * outputMultiplier();
        }

        /** combined heat of the connected group */
        public float groupHeat() {
            float total = 0f;
            for (SteamEngineBuild member : connectedEngines()) total += member.ownHeat();
            return total;
        }

        /** true when a block that needs heat touches this engine */
        public boolean heatOutlet() {
            for (mindustry.gen.Building neighbour : proximity) {
                if (neighbour.team == team && neighbour instanceof HeatConsumer
                    && ((HeatConsumer) neighbour).heatRequirement() > 0f) return true;
            }
            return false;
        }

        /**
         * The group heats like one machine: its total heat leaves through the engines touching heat consumers,
         * split between them so that several outlets never multiply the heat.
         */
        @Override
        public float heat() {
            if (!grouped()) return ownHeat();
            long tick = mindustry.Vars.state.updateId;
            if (lastHeat != tick) {
                arc.struct.Seq<SteamEngineBuild> group = connectedEngines();
                float total = 0f;
                int outlets = 0;
                for (SteamEngineBuild member : group) {
                    total += member.ownHeat();
                    if (member.heatOutlet()) outlets++;
                }
                for (SteamEngineBuild member : group) {
                    member.sharedHeat = outlets == 0 ? member.ownHeat() : member.heatOutlet() ? total / outlets : 0f;
                    member.lastHeat = tick;
                }
            }
            return sharedHeat;
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

        @Override public void display(arc.scene.ui.layout.Table table) {
            MetrumBuildingUI.display(table,this,t -> {
                MetrumBuildingUI.section(t,"reactor");
                MetrumBuildingUI.bar(t,()->MetrumBuildingUI.tr("steam-supply")+": "+Math.round(steamSupply*100f)+"%",()->arc.graphics.Color.lightGray,()->steamSupply);
                MetrumBuildingUI.bar(t,()->MetrumBuildingUI.tr("warmup")+": "+Math.round(warmup*100f)+"%",()->arc.graphics.Color.orange,()->warmup);
                MetrumBuildingUI.value(t,"heat",()->arc.util.Strings.autoFixed(ownHeat(),1));
                MetrumBuildingUI.value(t,"group",()->grouped()?"ON":"OFF");
                MetrumBuildingUI.value(t,"speed",()->Math.round(outputMultiplier()*100f)+"%");
                MetrumBuildingUI.value(t,"steam-use",()->Math.round(steamMultiplier()*100f)+"%");
                MetrumBuildingUI.section(t,"diagnostics");
                MetrumBuildingUI.status(t,this,()->MetrumBuildingUI.tr(steamSupply<0.01f?"no-input":steamSupply<0.99f?"low-supply":"running"));
            });
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
