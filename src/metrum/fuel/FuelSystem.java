package metrum.fuel;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntSeq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;

/**
 * Fuel tanks for upgrade level C and above (Metrum tiers: T1-T3 base, then B / C / D upgrades).
 * Tank size follows the base tier: T1 = 100, T2 = 200, T3 = 250.
 * Burns 1 fuel per 0.6 s; an empty tank applies {@link #outOfFuel} (-90% speed).
 * Fuel stations refill tanks via {@link #refuel}.
 */
public class FuelSystem {
    public static final float ticksPerFuel = 36f; // 0.6 s
    public static final Color barColor = Color.valueOf("ff9a2e");

    /** Only player teams use fuel by default, so enemy waves are not crippled. */
    public static boolean affectWaveTeam = false;

    public static StatusEffect outOfFuel;
    /** speed boost by fuel quality: 1 = slag +10%, 2 = magma +30%, 3 = plasma +50% */
    public static final StatusEffect[] boosts = new StatusEffect[4];
    private static final IntIntMap quality = new IntIntMap();

    /** unit type -> tank capacity; only registered types use fuel */
    public static final arc.struct.ObjectFloatMap<UnitType> capacities = new arc.struct.ObjectFloatMap<>();
    /** tank size per base tier (index = T) */
    public static final float[] tankByTier = {0f, 100f, 200f, 250f};
    /** unit id -> current fuel */
    private static final IntFloatMap fuel = new IntFloatMap();
    private static final IntSeq dead = new IntSeq();
    private static final arc.math.geom.Rect viewRect = new arc.math.geom.Rect();

    public static void load() {
        outOfFuel = new StatusEffect("out-of-fuel");
        outOfFuel.speedMultiplier = 0.1f;
        outOfFuel.color = barColor;
        outOfFuel.permanent = false;

        float[] mult = {0f, 1.1f, 1.3f, 1.5f};
        String[] names = {null, "fuel-slag", "fuel-magma", "fuel-plasma"};
        String[] colors = {null, "ffaa5f", "ff6a2e", "c896ff"};
        for (int q = 1; q <= 3; q++) {
            StatusEffect e = new StatusEffect(names[q]);
            e.speedMultiplier = mult[q];
            e.color = Color.valueOf(colors[q]);
            boosts[q] = e;
        }
    }

    /** Call after all content is loaded. */
    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            fuel.clear();
            quality.clear();
        });
        Events.run(Trigger.update, FuelSystem::update);
        Events.run(Trigger.draw, FuelSystem::draw);
    }

    /**
     * Registers a unit variant. {@code level}: 0 = base, 1 = B, 2 = C, 3 = D.
     * Only C and D variants get a tank.
     */
    public static void register(UnitType type, int baseTier, int level) {
        if (level >= 2) capacities.put(type, tankByTier[Math.min(Math.max(baseTier, 1), 3)]);
        else capacities.remove(type, 0f);
    }

    public static float capacity(UnitType type) {
        return capacities.get(type, 0f);
    }

    public static boolean usesFuel(Unit unit) {
        if (capacity(unit.type) <= 0f) return false;
        return affectWaveTeam || Vars.state.rules.waveTeam != unit.team;
    }

    public static float fuel(Unit unit) {
        return fuel.get(unit.id, capacity(unit.type));
    }

    public static int quality(Unit unit) {
        return quality.get(unit.id, 0);
    }

    /** Adds fuel of the given quality (1-3); returns the amount actually accepted. */
    public static float refuel(Unit unit, float amount, int fuelQuality) {
        float cap = capacity(unit.type), cur = fuel(unit);
        float added = Math.min(amount, cap - cur);
        if (added > 0f) {
            fuel.put(unit.id, cur + added);
            // the tank takes on the quality of the fuel last pumped in
            quality.put(unit.id, fuelQuality);
        }
        return Math.max(added, 0f);
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client()) return;
        float burn = Time.delta / ticksPerFuel;
        Groups.unit.each(u -> {
            if (!usesFuel(u)) return;
            float f = Math.max(fuel(u) - burn, 0f);
            fuel.put(u.id, f);
            if (f <= 0f) {
                u.apply(outOfFuel, 20f);
            } else {
                int q = quality.get(u.id, 0);
                if (q > 0) u.apply(boosts[q], 20f);
            }
        });
        // forget dead units
        dead.clear();
        IntFloatMap.Keys keys = fuel.keys();
        while (keys.hasNext) {
            int id = keys.next();
            if (Groups.unit.getByID(id) == null) dead.add(id);
        }
        for (int i = 0; i < dead.size; i++) {
            fuel.remove(dead.get(i), 0f);
            quality.remove(dead.get(i), 0);
        }
    }

    static void draw() {
        if (!Vars.state.isGame()) return;
        Draw.z(Layer.flyingUnit + 1f);
        Groups.unit.each(u -> {
            if (!usesFuel(u) || !inView(u)) return;
            float frac = fuel(u) / capacity(u.type);
            float w = u.hitSize * 1.1f, h = 1.6f;
            float x = u.x - w / 2f, y = u.y - u.hitSize * 0.75f - 2f;
            Draw.color(Color.black, 0.6f);
            Fill.rect(x + w / 2f, y, w + 1f, h + 1f);
            Draw.color(barColor);
            Fill.rect(x + w * frac / 2f, y, w * frac, h);
        });
        Draw.reset();
    }

    static boolean inView(Unit u) {
        return arc.Core.camera.bounds(viewRect).overlaps(
            u.x - u.hitSize, u.y - u.hitSize, u.hitSize * 2f, u.hitSize * 2f);
    }
}
