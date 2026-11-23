package metrum.modular;

import arc.Events;
import arc.struct.IntMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import metrum.content.MetrumItems;

/**
 * Per-unit module loadouts for modular drones (built in the Drone Assembler).
 * Weapons only work/draw if their module is mounted; engines add speed; the mining laser enables mining.
 */
public class Loadouts {
    static final IntMap<Seq<Item>> modules = new IntMap<>();
    static final IntMap<String> names = new IntMap<>();
    public static final StatusEffect[] engineBoost = new StatusEffect[5];
    /** wings: agile (+10% speed, faster turning); legs: landing gear / plating (+armour, +health regen) */
    public static StatusEffect wings, legs;

    public static void load() {
        for (int i = 1; i < engineBoost.length; i++) {
            StatusEffect e = new StatusEffect("engines-" + i);
            e.speedMultiplier = 1f + 0.15f * i;
            e.show = false;
            engineBoost[i] = e;
        }
        wings = new StatusEffect("module-wings");
        wings.speedMultiplier = 1.1f;
        wings.show = false;
        legs = new StatusEffect("module-legs");
        legs.healthMultiplier = 1.25f;
        legs.damage = -0.05f; // slow self-repair
        legs.show = false;
    }

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            modules.clear();
            names.clear();
        });
        Events.run(Trigger.update, Loadouts::update);
        mindustry.io.SaveVersion.addCustomChunk("metrum-loadouts", new Chunk());
    }

    static class Saved {
        float x, y;
        int type;
        int team = -1; // legacy records did not store ownership
        String name;
        Seq<Item> mods = new Seq<>();
    }

    static final Seq<Saved> pending = new Seq<>();

    /** Saves modules + ship names; units are re-matched by type and position on load (ids change). */
    static class Chunk implements mindustry.io.SaveFileReader.CustomChunk {
        @Override
        public void write(java.io.DataOutput out) throws java.io.IOException {
            Seq<Unit> list = new Seq<>();
            Groups.unit.each(u -> { if (isModular(u) && modules.containsKey(u.id)) list.add(u); });
            out.writeInt(-1); // version marker; old saves start with the unit count
            out.writeInt(list.size);
            for (Unit u : list) {
                out.writeFloat(u.x);
                out.writeFloat(u.y);
                out.writeShort(u.type.id);
                out.writeByte(u.team.id);
                String n = names.get(u.id);
                out.writeUTF(n == null ? "" : n);
                Seq<Item> m = modules.get(u.id);
                out.writeByte(m.size);
                for (Item it : m) out.writeShort(it.id);
            }
        }

        @Override
        public void read(java.io.DataInput in) throws java.io.IOException {
            pending.clear();
            int n = in.readInt();
            boolean hasTeam = n == -1;
            if (hasTeam) n = in.readInt();
            for (int i = 0; i < n; i++) {
                Saved s = new Saved();
                s.x = in.readFloat();
                s.y = in.readFloat();
                s.type = in.readShort();
                if (hasTeam) s.team = in.readUnsignedByte();
                s.name = in.readUTF();
                int c = in.readByte();
                for (int j = 0; j < c; j++) {
                    Item it = Vars.content.item(in.readShort());
                    if (it != null) s.mods.add(it);
                }
                pending.add(s);
            }
        }
    }

    static void resolvePending() {
        for (Saved s : pending) {
            Unit best = null;
            float bestDst = Vars.tilesize * 2f;
            for (Unit u : Groups.unit) {
                if (u.type.id != s.type || (s.team >= 0 && u.team.id != s.team) || modules.containsKey(u.id)) continue;
                float d = u.dst(s.x, s.y);
                if (d < bestDst) {
                    bestDst = d;
                    best = u;
                }
            }
            if (best != null) set(best, s.mods, s.name);
        }
        pending.clear();
    }

    public static void set(Unit unit, Seq<Item> mods, String name) {
        modules.put(unit.id, new Seq<>(mods));
        if (name != null && !name.isEmpty()) names.put(unit.id, name);
    }

    public static boolean has(Unit unit, Item module) {
        Seq<Item> m = modules.get(unit.id);
        return m != null && m.contains(module);
    }

    public static int count(Unit unit, Item module) {
        Seq<Item> m = modules.get(unit.id);
        return m == null ? 0 : m.count(i -> i == module);
    }

    public static String name(Unit unit) {
        return names.get(unit.id);
    }

    public static boolean isModular(Unit unit) {
        return unit.type instanceof ModularUnitType;
    }

    static void update() {
        if (!Vars.state.isPlaying()) return;
        if (!pending.isEmpty()) resolvePending();
        Groups.unit.each(u -> {
            if (!isModular(u)) return;
            int engines = Math.min(count(u, MetrumItems.partEngine), engineBoost.length - 1);
            if (engines > 0) u.apply(engineBoost[engines], 20f);
            if (has(u, MetrumItems.partWings)) u.apply(wings, 20f);
            if (has(u, MetrumItems.partLegs)) u.apply(legs, 20f);
            if (!has(u, MetrumItems.weaponMiningLaser)) u.mineTile = null;
        });
    }
}
