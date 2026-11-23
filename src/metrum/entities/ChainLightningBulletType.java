package metrum.entities;

import arc.graphics.Color;
import arc.math.geom.Position;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Healthc;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/**
 * Metrum chain lightning: an instant arc that hits the first target near the aim point and then
 * jumps from target to target - across enemy units AND buildings/walls - up to {@link #jumps} times.
 * Every jump deals a bit less damage ({@link #falloff}). Friendly buildings on the path are repaired.
 */
public class ChainLightningBulletType extends BulletType {
    public int jumps = 4;
    public float jumpRange = 60f;
    public float falloff = 0.85f;
    public float healPerJump = 0f;
    public Color arcColor = Color.valueOf("2feb79");
    public StatusEffect arcStatus = StatusEffects.shocked;

    public ChainLightningBulletType(float damage) {
        super(0f, damage);
        lifetime = 1f;
        collides = false;
        hittable = false;
        absorbable = false;
        keepVelocity = false;
        despawnEffect = hitEffect = Fx.none;
        shootEffect = Fx.hitLancer;
    }

    /** speed is 0 (instant arc), so the vanilla range formula gives 0 and units never fire: use the arc reach */
    @Override
    protected float calculateRange() {
        return rangeOverride > 0f ? rangeOverride : Math.max(range, 110f);
    }

    @Override
    public float estimateDPS() {
        return damage * jumps;
    }

    @Override
    public void init(Bullet b) {
        super.init(b);
        Seq<Healthc> hit = new Seq<>();
        Position from = b;
        float x = b.x + arc.math.Angles.trnsx(b.rotation(), range * 0.5f), y = b.y + arc.math.Angles.trnsy(b.rotation(), range * 0.5f);
        float dmg = damage;
        for (int i = 0; i <= jumps; i++) {
            Healthc target = next(b, i == 0 ? x : from.getX(), i == 0 ? y : from.getY(), i == 0 ? range : jumpRange, hit);
            if (target == null) break;
            hit.add(target);
            Fx.chainLightning.at(from.getX(), from.getY(), 0f, arcColor, target);
            mindustry.gen.Sounds.shockBullet.at(target.getX(), target.getY(), 1f + i * 0.08f, 0.4f);
            if (target instanceof Building && ((Building) target).team == b.team) {
                if (healPerJump > 0f) ((Building) target).heal(healPerJump);
            } else {
                target.damage(dmg);
                if (target instanceof Unit) ((Unit) target).apply(arcStatus, 60f);
                Fx.hitLancer.at(target.getX(), target.getY(), arcColor);
            }
            from = target;
            dmg *= falloff;
        }
    }

    /** nearest enemy unit, enemy building/wall, or (to arc through) friendly building not yet hit */
    Healthc next(Bullet b, float x, float y, float r, Seq<Healthc> hit) {
        Unit u = Units.closestEnemy(b.team, x, y, r, e -> !hit.contains(e));
        Building enemyB = Units.findEnemyTile(b.team, x, y, r, e -> !hit.contains(e));
        Healthc best = u;
        if (enemyB != null && (best == null || enemyB.dst(x, y) < ((Unit) best).dst(x, y))) best = enemyB;
        if (best == null && healPerJump > 0f) {
            Building ally = mindustry.Vars.indexer.findTile(b.team, x, y, r, e -> !hit.contains(e) && e.damaged());
            best = ally;
        }
        return best;
    }
}
