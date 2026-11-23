package metrum.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Builds a modular drone: pick a hull, fill its slots with modules (squares in the config panel),
 * feed the hull + modules, wait {@link #buildTime}; the drone rolls out and asks for a name.
 * Config string: "hullUnitId;moduleId,moduleId,..." (-1 = empty slot).
 */
public class DroneAssembler extends Block {
    public final Seq<ModularUnitType> hulls = new Seq<>();
    public final Seq<Item> modules = new Seq<>();
    public float buildTime = 60f * 60f;

    public DroneAssembler(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        itemCapacity = 10;
        saveConfig = true;
        config(String.class, (DroneAssemblerBuild b, String s) -> b.parse(s));
    }

    @Override
    public void setBars() {
        super.setBars();
        // progress + what is still missing, so a stalled assembler explains itself
        addBar("metrum-progress", (DroneAssemblerBuild b) -> new mindustry.ui.Bar(
            () -> b.statusText(), () -> b.ready() ? Pal.accent : Color.scarlet, () -> b.progress));
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, buildTime / 60f, StatUnit.seconds);
    }

    public class DroneAssemblerBuild extends Building {
        public ModularUnitType hull;
        public Item[] slots = new Item[0];
        public float progress;

        void parse(String s) {
            // Reapplying the same plan (e.g. configuration paste) must not restart assembly.
            if (s != null && s.equals(encode(hull, slots))) return;
            hull = null;
            slots = new Item[0];
            progress = 0f;
            if (s == null || s.isEmpty()) return;
            String[] parts = s.split(";", -1);
            int hid = Integer.parseInt(parts[0]);
            for (ModularUnitType h : hulls) if (h.id == hid) hull = h;
            if (hull == null) return;
            slots = new Item[hull.slots];
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String[] ids = parts[1].split(",");
                for (int i = 0; i < Math.min(ids.length, slots.length); i++) {
                    int id = Integer.parseInt(ids[i]);
                    slots[i] = id < 0 ? null : Vars.content.item(id);
                }
            }
        }

        String encode(ModularUnitType h, Item[] s) {
            if (h == null) return "";
            StringBuilder sb = new StringBuilder().append(h.id).append(';');
            for (int i = 0; i < s.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(s[i] == null ? -1 : s[i].id);
            }
            return sb.toString();
        }

        /** items needed for the current plan: hull + every mounted module */
        int needed(Item item) {
            if (hull == null) return 0;
            int n = item == hull.hull ? 1 : 0;
            for (Item s : slots) if (s == item) n++;
            return n;
        }

        String statusText() {
            if (hull == null) return Core.bundle.get("metrum.assembler.nohull");
            if (items.get(hull.hull) < 1) return Core.bundle.format("metrum.assembler.missing", hull.hull.localizedName);
            for (Item s : slots) if (s != null && items.get(s) < needed(s)) return Core.bundle.format("metrum.assembler.missing", s.localizedName);
            if (efficiency <= 0f) return Core.bundle.get("metrum.assembler.nopower");
            return Core.bundle.format("metrum.assembler.building", (int) (progress * 100));
        }

        boolean ready() {
            if (hull == null || items.get(hull.hull) < 1) return false;
            for (Item s : slots) if (s != null && items.get(s) < needed(s)) return false;
            return true;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return needed(item);
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.get(item) < needed(item);
        }

        @Override
        public boolean shouldConsume() {
            return ready() && enabled;
        }

        @Override
        public void updateTile() {
            if (!ready() || efficiency <= 0f) return;
            progress = Math.min(1f, progress + getProgressIncrease(buildTime));
            if (progress < 1f || Vars.net.client()) return;
            progress = 0f;
            items.remove(hull.hull, 1);
            Seq<Item> mods = new Seq<>();
            for (Item s : slots) if (s != null) {
                items.remove(s, 1);
                mods.add(s);
            }
            Unit u = hull.spawn(team, x, y);
            Loadouts.set(u, mods, null);
            Fx.spawn.at(u);
            if (!Vars.headless && team == Vars.player.team()) askName(u);
        }

        void askName(Unit u) {
            Vars.ui.showTextInput(Core.bundle.get("metrum.assembler.name"), Core.bundle.get("metrum.assembler.nameprompt"),
                24, "", name -> Loadouts.set(u, Loadouts.modules.get(u.id, new Seq<>()), name));
        }

        @Override
        public void buildConfiguration(Table table) {
            table.background(mindustry.gen.Tex.pane);
            table.defaults().pad(3f);
            // hull choice
            table.table(t -> {
                for (ModularUnitType h : hulls) {
                    t.button(b -> b.image(h.hull.uiIcon).size(32f), () -> {
                        configure(encode(h, new Item[h.slots]));
                        // slot count changed: rebuild the panel so it shows the new number of slots
                        if (!Vars.headless) Vars.control.input.config.showConfig(this);
                    })
                        .size(48f).checked(b -> hull == h);
                }
            }).row();
            if (hull == null) return;
            table.add(Core.bundle.get("metrum.assembler.slots")).row();
            // module slots as squares; clicking cycles through modules (empty -> module1 -> ...)
            table.table(t -> {
                for (int i = 0; i < slots.length; i++) {
                    int idx = i;
                    t.button(b -> b.image(() -> idx >= slots.length || slots[idx] == null ? Icon.add.getRegion() : slots[idx].uiIcon).size(32f), () -> {
                        if (idx >= slots.length) return;
                        Item[] next = slots.clone();
                        int cur = next[idx] == null ? -1 : modules.indexOf(next[idx]);
                        next[idx] = cur + 1 >= modules.size ? null : modules.get(cur + 1);
                        configure(encode(hull, next));
                    }).size(52f);
                }
            });
        }

        @Override
        public Object config() {
            return encode(hull, slots);
        }

        @Override
        public void draw() {
            super.draw();
            if (hull == null) return;
            Draw.z(Layer.blockOver);
            Draw.alpha(0.3f + 0.7f * progress);
            Draw.rect(hull.fullIcon, x, y, size * 5f, size * 5f, 90f);
            Draw.reset();
            // slot squares along the bottom edge
            float s = 3f, start = x - (slots.length - 1) * (s + 1.5f) / 2f;
            for (int i = 0; i < slots.length; i++) {
                float sx = start + i * (s + 1.5f), sy = y - size * 4f + 3f;
                Draw.color(slots[i] == null ? Color.gray : (items.get(slots[i]) > 0 ? Pal.accent : Color.lightGray));
                if (slots[i] == null) {
                    Lines.stroke(0.5f);
                    Lines.square(sx, sy, s / 2f);
                } else Fill.square(sx, sy, s / 2f);
            }
            Draw.reset();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.str(encode(hull, slots));
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            parse(read.str());
            progress = read.f();
        }
    }
}
