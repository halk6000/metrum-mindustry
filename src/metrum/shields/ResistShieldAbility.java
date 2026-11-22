package metrum.shields;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Bar;

/**
 * Metrum shield library: a personal shield with per-damage-type resistances.
 * Bullets entering the shield are classified:
 *  - EXPLOSIVE (missiles, artillery, anything with splash damage) -> {@link #explosiveResist}
 *  - PHYSICAL (plain kinetic bullets without status effects)      -> {@link #physicalResist}
 *  - OTHER (lasers, flamethrowers, slag, poison/status bullets...) -> pass through untouched
 * The resisted part of the damage is taken by the shield (bullet damage is reduced; fully resisted bullets are absorbed).
 * When broken, the shield is down for {@link #breakTime} and comes back with {@link #restoreHealth}.
 * Optional while-active bonuses: extra armour and burning resistance (armour plating shield).
 */
public class ResistShieldAbility extends Ability {
    public enum Kind { EXPLOSIVE, PHYSICAL, OTHER }

    public float maxHealth = 500f;
    public float regenPerSecond = 2f;
    public float breakTime = 60f * 60f;
    public float restoreHealth = 100f;
    public float radius = 30f;
    public float explosiveResist = 1f;
    public float physicalResist = 0.6f;
    /** while the shield holds: bonus armour, and the fraction of burning that is shrugged off */
    public float bonusArmor = 0f, burnResist = 0f;
    public boolean visible = true;
    /** armour-plating mode: the shield loses HP as the unit takes damage (instead of catching bullets) */
    public boolean drainOnDamage = false;
    private float lastHealth = -1f;
    public Color color = Color.valueOf("7fe3ff");

    public float health = -1f, broken, hitFlash;
    /** bullets already handled (a physical bullet is reduced once, not every tick it stays inside) */
    private final arc.struct.IntSet seen = new arc.struct.IntSet();
    private float baseArmor = -1f;

    public static Kind classify(BulletType t) {
        if (t instanceof LaserBulletType || t instanceof ContinuousBulletType) return Kind.OTHER;
        if (t instanceof MissileBulletType || t instanceof ArtilleryBulletType || t.splashDamage > 0f) return Kind.EXPLOSIVE;
        if (t instanceof BasicBulletType && t.status == mindustry.content.StatusEffects.none && !t.makeFire && t.incendAmount <= 0)
            return Kind.PHYSICAL;
        return Kind.OTHER;
    }

    public boolean active() {
        return health > 0f && broken <= 0f;
    }

    @Override
    public void update(Unit unit) {
        if (health < 0f) health = maxHealth;
        if (baseArmor < 0f) baseArmor = unit.type.armor;
        hitFlash = Math.max(hitFlash - Time.delta / 10f, 0f);

        if (broken > 0f) {
            broken -= Time.delta;
            if (broken <= 0f) health = restoreHealth;
        } else {
            health = Math.min(health + regenPerSecond / 60f * Time.delta, maxHealth);
        }

        if (drainOnDamage) {
            if (lastHealth >= 0f && unit.health < lastHealth && active()) {
                health -= lastHealth - unit.health;
                hitFlash = 1f;
                if (health <= 0f) {
                    health = 0f;
                    broken = breakTime;
                    Fx.shieldBreak.at(unit.x, unit.y, radius, color);
                }
            }
            lastHealth = unit.health;
        }

        boolean on = active();
        unit.armor = baseArmor + (on ? bonusArmor : 0f);
        if (on && burnResist > 0f && unit.hasEffect(mindustry.content.StatusEffects.burning) && Mathf.chance(burnResist * 0.1f * Time.delta)) {
            unit.unapply(mindustry.content.StatusEffects.burning);
        }
        if (!on) return;

        Groups.bullet.intersect(unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, (arc.func.Cons<Bullet>) b -> absorb(unit, b));
    }

    void absorb(Unit unit, Bullet b) {
        if (!active() || b.team == unit.team || !b.within(unit, radius) || !seen.add(b.id)) return;
        if (seen.size > 2048) seen.clear();
        Kind k = classify(b.type);
        float resist = k == Kind.EXPLOSIVE ? explosiveResist : k == Kind.PHYSICAL ? physicalResist : 0f;
        if (resist <= 0f) return;
        float full = b.damage + (k == Kind.EXPLOSIVE ? b.type.splashDamage : 0f);
        float blocked = full * resist;
        health -= blocked;
        hitFlash = 1f;
        if (resist >= 0.999f) {
            b.absorb();
            Fx.absorb.at(b);
        } else {
            b.damage *= (1f - resist);
        }
        if (health <= 0f) {
            health = 0f;
            broken = breakTime;
            Fx.shieldBreak.at(unit.x, unit.y, radius, color);
        }
    }

    @Override
    public void draw(Unit unit) {
        if (!active()) return;
        if (drainOnDamage) {
            // plating: thin glowing outline instead of a bubble
            Draw.z(Layer.flyingUnit + 0.5f);
            Draw.color(color, 0.35f + hitFlash * 0.5f);
            Lines.stroke(1f);
            Lines.poly(unit.x, unit.y, 6, unit.hitSize * 0.7f, unit.rotation);
            Draw.reset();
            return;
        }
        if (!visible) return;
        Draw.z(Layer.shields);
        Draw.color(color, 0.15f + hitFlash * 0.35f);
        Fill.circle(unit.x, unit.y, radius);
        Draw.color(color, 0.6f + hitFlash * 0.4f);
        Lines.stroke(1.2f);
        Lines.circle(unit.x, unit.y, radius);
        Draw.reset();
    }

    @Override
    public void displayBars(Unit unit, Table bars) {
        bars.add(new Bar(
            () -> broken > 0f ? Core.bundle.format("metrum.shield.broken", (int) (broken / 60f)) : Core.bundle.format("metrum.shield.hp", (int) health, (int) maxHealth),
            () -> broken > 0f ? Color.gray : color,
            () -> broken > 0f ? 1f - broken / breakTime : health / maxHealth)).growX();
        bars.row();
    }

    @Override
    public void addStats(Table t) {
        super.addStats(t);
        t.add("[lightgray]" + Core.bundle.format("metrum.shield.stats", (int) maxHealth, (int) (explosiveResist * 100), (int) (physicalResist * 100),
            Mathf.round(regenPerSecond * 10f) / 10f, (int) (breakTime / 60f), (int) restoreHealth)).left().row();
        if (bonusArmor > 0f || burnResist > 0f)
            t.add("[lightgray]" + Core.bundle.format("metrum.shield.plating", (int) bonusArmor, (int) (burnResist * 100))).left().row();
    }
}
