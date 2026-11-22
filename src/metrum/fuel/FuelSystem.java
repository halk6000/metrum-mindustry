package metrum.fuel;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

/**
 * Fuel tanks for tier C+ units (tier 3/4/5 -> 100/200/250 fuel).
 * Burns 1 fuel per 0.6 s; an empty tank applies {@link #outOfFuel} (-90% speed).
 * Fuel stations refill tanks via {@link #refuel}.
 */
public class FuelSystem {
    public static final float ticksPerFuel = 36f; // 0.6 s
    public static final int minTier = 3;
    public static final Color barColor = Color.valueOf("ff9a2e");

    /** Only player teams use fuel by default, so enemy waves are not crippled. */
    public static boolean affectWaveTeam = false;

    public static StatusEffect outOfFuel;
    /** speed boost by fuel quality: 1 = slag +10%, 2 = magma +30%, 3 = plasma +50% */
    public static final StatusEffect[] boosts = new StatusEffect[4];
    private static final IntIntMap quality = new IntIntMap();

    /** unit type -> tier (1 = factory output, +1 per reconstructor step) */
    public static final ObjectIntMap<UnitType> tiers = new ObjectIntMap<>();
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
        computeTiers();
        Events.on(WorldLoadEvent.class, e -> {
            fuel.clear();
            quality.clear();
        });
        Events.run(Trigger.update, FuelSystem::update);
        Events.run(Trigger.draw, FuelSystem::draw);
    }

    public static void setTier(UnitType type, int tier) {
        tiers.put(type, tier);
    }

    public static int tier(UnitType type) {
        return tiers.get(type, 1);
    }

    public static float capacity(UnitType type) {
        int t = tier(type);
        if (t < minTier) return 0f;
        return t == 3 ? 100f : t == 4 ? 200f : 250f;
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

    static void computeTiers() {
        for (Block b : Vars.content.blocks()) {
            if (b instanceof UnitFactory) {
                for (UnitFactory.UnitPlan plan : ((UnitFactory) b).plans) {
                    if (!tiers.containsKey(plan.unit)) tiers.put(plan.unit, 1);
                }
            }
        }
        boolean changed = true;
        for (int pass = 0; changed && pass < 10; pass++) {
            changed = false;
            for (Block b : Vars.content.blocks()) {
                if (!(b instanceof Reconstructor)) continue;
                for (UnitType[] up : ((Reconstructor) b).upgrades) {
                    int t = tiers.get(up[0], 1) + 1;
                    if (tiers.get(up[1], 0) < t) {
                        tiers.put(up[1], t);
                        changed = true;
                    }
                }
            }
        }
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
