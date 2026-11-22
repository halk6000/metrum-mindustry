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

    public static void load() {
        for (int i = 1; i < engineBoost.length; i++) {
            StatusEffect e = new StatusEffect("engines-" + i);
            e.speedMultiplier = 1f + 0.15f * i;
            e.show = false;
            engineBoost[i] = e;
        }
    }

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            modules.clear();
            names.clear();
        });
        Events.run(Trigger.update, Loadouts::update);
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
        Groups.unit.each(u -> {
            if (!isModular(u)) return;
            int engines = Math.min(count(u, MetrumItems.partEngine), engineBoost.length - 1);
            if (engines > 0) u.apply(engineBoost[engines], 20f);
            if (!has(u, MetrumItems.weaponMiningLaser)) u.mineTile = null;
        });
    }
}
