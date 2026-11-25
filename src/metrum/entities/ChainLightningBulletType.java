package metrum.entities;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Healthc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * Metrum chain lightning: an instant arc that hits the first target near the aim point and then
 * jumps from target to target - across enemy units AND buildings/walls - up to {@link #jumps} times.
 * Every jump deals a bit less damage ({@link #falloff}). Friendly buildings on the path are repaired.
 */
public class ChainLightningBulletType extends BulletType {
    /** thick jagged bolt with side forks, drawn from the effect position to the position in its data */
    public static final Effect bolt = new Effect(22f, 500f, e -> {
        if (!(e.data instanceof Position)) return;
        Position to = (Position) e.data;
        float dst = Mathf.dst(e.x, e.y, to.getX(), to.getY()), angle = Angles.angle(e.x, e.y, to.getX(), to.getY());
        int segments = Math.max(2, Mathf.ceil(dst / 9f));
        // colour glow below, white core on top; the same seed gives both the same shape
        for (int pass = 0; pass < 2; pass++) {
            Rand rand = new Rand(e.id);
            float width = (pass == 0 ? 4.5f : 1.6f) * e.fout();
            Draw.color(pass == 0 ? e.color : Color.white, pass == 0 ? 0.75f : 1f);
            float px = e.x, py = e.y;
            for (int i = 1; i <= segments; i++) {
                float f = i / (float) segments, side = i == segments ? 0f : rand.range(6f);
                float nx = e.x + Angles.trnsx(angle, dst * f, side), ny = e.y + Angles.trnsy(angle, dst * f, side);
                Lines.stroke(width);
                Lines.line(px, py, nx, ny);
                Fill.circle(nx, ny, width / 2f);
                if (i < segments && rand.chance(0.35f)) {
                    float fork = angle + rand.range(55f), len = rand.random(8f, 20f);
                    Lines.stroke(width * 0.5f);
                    Lines.line(nx, ny, nx + Angles.trnsx(fork, len), ny + Angles.trnsy(fork, len));
                }
                px = nx;
                py = ny;
            }
        }
        Drawf.light(e.x, e.y, to.getX(), to.getY(), 24f * e.fout(), e.color, 0.6f);
        Draw.reset();
    }).layer(Layer.bullet + 1f);

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
        // aim point, limited to the reach of the weapon
        float reach = range;
        if (b.aimX >= 0f && b.aimY >= 0f) reach = Math.min(reach, b.dst(b.aimX, b.aimY));
        float ax = b.x + Angles.trnsx(b.rotation(), reach), ay = b.y + Angles.trnsy(b.rotation(), reach);
        float dmg = damage;
        for (int i = 0; i <= jumps; i++) {
            Healthc target;
            if (i == 0) {
                target = next(b, ax, ay, jumpRange, hit);
                // nothing at the aim point: anything along the way
                if (target == null) target = next(b, b.x + Angles.trnsx(b.rotation(), range / 2f), b.y + Angles.trnsy(b.rotation(), range / 2f), range / 2f, hit);
            } else {
                target = next(b, from.getX(), from.getY(), jumpRange, hit);
            }
            if (target == null) {
                // a miss still discharges visibly
                if (i == 0) bolt.at(b.x, b.y, 0f, arcColor, new Vec2(b.x + Angles.trnsx(b.rotation(), range), b.y + Angles.trnsy(b.rotation(), range)));
                break;
            }
            hit.add(target);
            bolt.at(from.getX(), from.getY(), 0f, arcColor, new Vec2(target.getX(), target.getY()));
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
