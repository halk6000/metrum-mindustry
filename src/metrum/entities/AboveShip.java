package metrum.entities;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

/** Draws the attacks of Metrum's flying units above their hulls. Bombs still fall below the ship. */
public class AboveShip {
    public static final float layer = Layer.flyingUnit + 1f;

    private static final ObjectMap<Effect, Effect> lifted = new ObjectMap<>();
    private static final ObjectSet<BulletType> done = new ObjectSet<>();
    private static final ObjectSet<Color> arcColors = new ObjectSet<>();

    /** call once, after all units and before content init */
    public static void load() {
        for (UnitType unit : Vars.content.units()) {
            if (!unit.flying || !unit.name.startsWith("metrum-")) continue;
            for (Weapon weapon : unit.weapons) lift(weapon.bullet);
        }
        // vanilla draws every lightning bolt with one shared effect: lift only the colours of our weapons
        Cons<Effect.EffectContainer> vanilla = Fx.lightning.renderer;
        Fx.lightning.renderer = e -> {
            if (arcColors.contains(e.color)) Draw.z(layer);
            vanilla.get(e);
        };
    }

    /** raises a bullet, its muzzle effects and every bullet it spawns */
    static void lift(BulletType type) {
        if (type == null || type instanceof BombBulletType || !done.add(type)) return;
        type.layer = Math.max(type.layer, layer);
        type.shootEffect = lift(type.shootEffect);
        type.smokeEffect = lift(type.smokeEffect);
        type.chargeEffect = lift(type.chargeEffect);
        if (type instanceof LightningBulletType) arcColors.add(((LightningBulletType) type).lightningColor.cpy());
        lift(type.fragBullet);
        lift(type.intervalBullet);
        lift(type.lightningType);
        for (BulletType spawned : type.spawnBullets) lift(spawned);
    }

    /** raised copy of a plain effect; the shared vanilla effect stays untouched */
    static Effect lift(Effect effect) {
        if (effect == null || effect == Fx.none || effect.getClass() != Effect.class || effect.layer >= layer) return effect;
        Effect copy = lifted.get(effect);
        if (copy == null) {
            copy = new Effect(effect.lifetime, effect.clip, effect.renderer);
            copy.startDelay = effect.startDelay;
            copy.baseRotation = effect.baseRotation;
            copy.followParent = effect.followParent;
            copy.rotWithParent = effect.rotWithParent;
            copy.layer = layer;
            copy.layerDuration = effect.layerDuration;
            lifted.put(effect, copy);
        }
        return copy;
    }
}
