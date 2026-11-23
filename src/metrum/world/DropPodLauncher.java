package metrum.world;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;

/**
 * Drop pod launcher: units drive/fly in as payload; pick one of YOUR sectors with a core (any planet);
 * launch drains {@link #launchPowerPerSecond} for {@link #launchTime} - the rest of the base may brown out.
 * The pod lands near the core of the target sector the next time that sector is loaded.
 */
public class DropPodLauncher extends Block {
    public int capacity = 6;
    public float launchTime = 60f * 15f;
    public float launchPowerPerSecond = 1500f;
    static final String pendingKey = "metrum-droppods-";

    public DropPodLauncher(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        acceptsPayload = true;
        config(Integer.class, (DropPodBuild b, Integer id) -> b.target = id);
    }

    @Override
    public void init() {
        consumePower(launchPowerPerSecond / 60f);
        super.init();
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("metrum-pod", (DropPodBuild b) -> new Bar(
            () -> Core.bundle.format("metrum.pod.bar", b.units.size, capacity),
            () -> Color.valueOf("ffd37f"), () -> b.launching ? b.progress : b.units.size / (float) capacity));
    }

    /** spawn pods queued for the sector that just loaded */
    public static void initEvents() {
        Events.on(WorldLoadEvent.class, e -> {
            Sector s = Vars.state.rules.sector;
            if (s == null || Vars.net.client()) return;
            String key = pendingKey + s.planet.name + "-" + s.id;
            String data = Core.settings.getString(key, "");
            if (data.isEmpty()) return;
            Core.settings.remove(key);
            Building core = Vars.state.rules.defaultTeam.core();
            if (core == null) return;
            Time.run(120f, () -> {
                for (String id : data.split(",")) {
                    if (id.isEmpty()) continue;
                    UnitType type = Vars.content.unit(Integer.parseInt(id));
                    if (type == null) continue;
                    float x = core.x + Mathf.range(48f), y = core.y + Mathf.range(48f);
                    Fx.launchPod.at(x, y);
                    Unit u = type.spawn(core.team, x, y);
                    Fx.spawnShockwave.at(u.x, u.y);
                }
                if (!Vars.headless) Vars.ui.hudfrag.showToast(Core.bundle.get("metrum.pod.arrived"));
            });
        });
    }

    static Seq<Sector> targets() {
        Seq<Sector> out = new Seq<>();
        for (Planet p : Vars.content.planets()) for (Sector s : p.sectors) {
            if (s.hasBase() && s != Vars.state.rules.sector) out.add(s);
        }
        return out;
    }

    public class DropPodBuild extends Building {
        public IntSeq units = new IntSeq();
        public int target = -1;
        public boolean launching;
        public float progress;

        Sector targetSector() {
            for (Sector s : targets()) if (s.planet.id * 10000 + s.id == target) return s;
            return null;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload) {
            return !launching && units.size < capacity && payload instanceof UnitPayload;
        }

        @Override
        public void handlePayload(Building source, Payload payload) {
            units.add(((UnitPayload) payload).unit.type.id);
            Fx.unitDrop.at(x, y);
        }

        @Override
        public boolean shouldConsume() {
            return launching;
        }

        @Override
        public void updateTile() {
            if (!launching) return;
            progress += efficiency * Time.delta / launchTime;
            if (progress >= 1f) {
                Sector s = targetSector();
                if (s != null) {
                    String key = pendingKey + s.planet.name + "-" + s.id;
                    StringBuilder sb = new StringBuilder(Core.settings.getString(key, ""));
                    for (int i = 0; i < units.size; i++) sb.append(units.get(i)).append(',');
                    Core.settings.put(key, sb.toString());
                    Fx.launchPod.at(this);
                }
                units.clear();
                launching = false;
                progress = 0f;
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.background(Tex.pane);
            table.add(Core.bundle.get("metrum.pod.target")).row();
            for (Sector s : targets()) {
                int key = s.planet.id * 10000 + s.id;
                table.button(s.planet.localizedName + ": " + s.name(), () -> configure(key))
                    .width(260f).height(40f).checked(b -> target == key).row();
            }
            table.button(Core.bundle.get("metrum.pod.launch"), () -> {
                if (target >= 0 && units.size > 0) launching = true;
            }).width(260f).height(46f).disabled(b -> launching || target < 0 || units.size == 0).row();
        }

        @Override
        public Object config() {
            return target;
        }

        @Override
        public void draw() {
            super.draw();
            if (!launching) return;
            Draw.z(Layer.effect);
            Draw.color(Color.valueOf("ffd37f"), 0.7f);
            Lines.stroke(2f);
            Lines.circle(x, y, size * 4f * (1f - progress) + 2f);
            Draw.reset();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(target);
            write.bool(launching);
            write.f(progress);
            write.s(units.size);
            for (int i = 0; i < units.size; i++) write.s(units.get(i));
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            target = read.i();
            launching = read.bool();
            progress = read.f();
            int n = read.s();
            units.clear();
            for (int i = 0; i < n; i++) units.add(read.s());
        }
    }
}
