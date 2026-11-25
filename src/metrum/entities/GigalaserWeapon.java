package metrum.entities;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.entities.Damage;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;

/**
 * Gigalaser that charges up first: red points grow on every gun of the ship, then a beam leaves each of them.
 * The beams are the look of one single laser, so the number of guns does not change its damage.
 */
public class GigalaserWeapon extends Weapon {
    public static final Color red = Color.valueOf("ff3c3c");

    public GigalaserWeapon(String name, float chargeTime) {
        super(name);
        shoot.firstShotDelay = chargeTime;
        chargeSound = Sounds.chargeCorvus;
        mountType = ChargeMount::new;
    }

    public static class ChargeMount extends WeaponMount {
        public float chargeTime;

        public ChargeMount(Weapon weapon) {
            super(weapon);
        }
    }

    @FunctionalInterface
    public interface MuzzleCons {
        void get(float x, float y);
    }

    /** visits the muzzle of every other weapon; a ship without guns uses the gigalaser itself */
    public static void muzzles(Unit unit, MuzzleCons cons) {
        boolean any = false;
        for (WeaponMount m : unit.mounts) {
            Weapon w = m.weapon;
            if (w instanceof GigalaserWeapon) continue;
            any = true;
            visit(unit, m, w, cons);
        }
        if (any) return;
        for (WeaponMount m : unit.mounts) if (m.weapon instanceof GigalaserWeapon) visit(unit, m, m.weapon, cons);
    }

    private static void visit(Unit unit, WeaponMount m, Weapon w, MuzzleCons cons) {
        float base = unit.rotation - 90f, turned = base + (w.rotate ? m.rotation : w.baseRotation);
        cons.get(unit.x + Angles.trnsx(base, w.x, w.y) + Angles.trnsx(turned, w.shootX, w.shootY),
            unit.y + Angles.trnsy(base, w.x, w.y) + Angles.trnsy(turned, w.shootX, w.shootY));
    }

    @Override
    public void update(Unit unit, WeaponMount mount) {
        super.update(unit, mount);
        ChargeMount charge = (ChargeMount) mount;
        charge.chargeTime = mount.charging ? charge.chargeTime + Time.delta : 0f;
    }

    @Override
    public void draw(Unit unit, WeaponMount mount) {
        super.draw(unit, mount);
        if (!mount.charging) return;
        float time = ((ChargeMount) mount).chargeTime;
        float f = Mathf.clamp(time / Math.max(1f, shoot.firstShotDelay));
        float z = Draw.z();
        Draw.z(Layer.effect);
        muzzles(unit, (x, y) -> {
            float radius = 0.6f + 2.6f * f + Mathf.absin(Time.time, 2f, 0.5f * f);
            Draw.color(red, 0.35f + 0.25f * f);
            Fill.circle(x, y, radius * 1.8f);
            Draw.color(red);
            Fill.circle(x, y, radius);
            Draw.color(Color.white, f);
            Fill.circle(x, y, radius * 0.45f);
            // sparks pulled into the point
            Draw.color(red, f);
            Lines.stroke(0.8f);
            Rand rand = new Rand(unit.id * 31L + (long) (x - unit.x));
            for (int i = 0; i < 5; i++) {
                float pull = 1f - (time / 18f + rand.random(1f)) % 1f, angle = rand.random(360f);
                Lines.lineAngle(x + Angles.trnsx(angle, 11f * pull), y + Angles.trnsy(angle, 11f * pull), angle, 2.5f * pull);
            }
            Drawf.light(x, y, 22f * f, red, 0.7f);
        });
        Draw.z(z);
        Draw.color();
    }

    /** one laser for damage, drawn as beams from every gun that meet at its end */
    public static class MultiBeamBulletType extends ContinuousLaserBulletType {
        public MultiBeamBulletType(float damage) {
            super(damage);
        }

        @Override
        public void draw(Bullet b) {
            if (!(b.owner instanceof Unit) || !((Unit) b.owner).isAdded()) {
                super.draw(b);
                return;
            }
            float fade = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (b.lifetime - fadeTime)) / fadeTime : 1f);
            float real = Damage.findLength(b, length * fade, laserAbsorb, pierceCap);
            Vec2 tip = new Vec2().trns(b.rotation(), real).add(b.x, b.y);
            muzzles((Unit) b.owner, (x, y) -> {
                for (int i = 0; i < colors.length; i++) {
                    float layer = i / (float) Math.max(1, colors.length - 1);
                    float stroke = (width + Mathf.absin(Time.time, oscScl, oscMag)) * fade * Mathf.lerp(strokeFrom, strokeTo, layer) * 0.6f;
                    Draw.color(colors[i]);
                    Lines.stroke(stroke);
                    Lines.line(x, y, tip.x, tip.y, false);
                    Fill.circle(x, y, stroke * 0.7f);
                    Fill.circle(tip.x, tip.y, stroke * 0.9f);
                }
                Drawf.light(x, y, tip.x, tip.y, lightStroke * fade, lightColor, 0.4f);
            });
            Draw.reset();
        }
    }
}
