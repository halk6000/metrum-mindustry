package metrum.entities;

import arc.math.Angles;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Fires;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.world.Tile;

/**
 * Laser with a "death ray" aftershock: after {@link #aftershockDelay} a chain of fire explosions
 * runs along the beam's path (from the emitter to the tip) over {@link #aftershockTravel} ticks.
 * Each explosion deals {@link #aftershockFraction} of the laser damage and sets the ground on fire.
 */
public class AftershockLaserBulletType extends LaserBulletType {
    public float aftershockDelay = 120f;     // 2 s
    public float aftershockTravel = 42f;     // ~0.7 s from emitter to tip
    public float aftershockFraction = 0.3f;
    public float aftershockSpacing = 18f;
    public float aftershockRadius = 20f;

    public AftershockLaserBulletType(float damage) {
        super(damage);
    }

    @Override
    public void init(Bullet b) {
        super.init(b);
        float len = Damage.findLaserLength(b, length);
        float sx = b.x, sy = b.y, rot = b.rotation();
        Team team = b.team;
        float dmg = damage * aftershockFraction;
        int steps = Math.max(1, (int) (len / aftershockSpacing));
        for (int i = 1; i <= steps; i++) {
            float d = len * i / steps;
            float px = sx + Angles.trnsx(rot, d), py = sy + Angles.trnsy(rot, d);
            Time.run(aftershockDelay + aftershockTravel * i / steps, () -> {
                Damage.damage(team, px, py, aftershockRadius, dmg);
                Damage.status(team, px, py, aftershockRadius, StatusEffects.burning, 240f, true, true);
                Fx.blastExplosion.at(px, py);
                mindustry.gen.Sounds.explosionPlasmaSmall.at(px, py, arc.math.Mathf.random(0.9f, 1.1f), 0.45f);
                Tile tile = mindustry.Vars.world.tileWorld(px, py);
                if (tile != null) metrum.entities.MetrumFires.create(tile);
            });
        }
    }
}
