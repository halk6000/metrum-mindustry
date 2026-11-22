package metrum.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.MinerAI;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.WaveEffect;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.unit.ErekirUnitType;

/** MonoMet mining drones ported from the original Metrum JSON mod. */
public class MetrumUnits {
    public static UnitType monoMetT1, monoMetT1b, monoMetT2, monoMetT2b, monoMetT3, monoMetT3b, monoMetT1c, monoMetT2c, monoMetT3c, monoMetT1d, monoMetT2d, monoMetT3d;

    private static final String P = "metrum-";

    public static void load() {
        monoMetT1 = miner("monomet-t1", 8f, 3.8f, 0.12f, 120f, 1.8f, 90, 60f, 0.5f, 3, 6.2f, true, Items.beryllium, MetrumItems.iron);

        monoMetT1b = miner("monomet-t1b", 8f, 7.8f, 0.32f, 140f, 1.8f, 120, 120f, 2.1f, 4, 6.2f, false, Items.beryllium, MetrumItems.iron);
        LightningBulletType heal = new LightningBulletType();
        heal.lightningColor = heal.hitColor = Color.valueOf("2feb79");
        heal.damage = 25f;
        heal.lightningLength = 16;
        heal.lightningLengthRand = 16;
        heal.shootEffect = Fx.shootHeal;
        heal.healPercent = 2f;
        BulletType arc = new BulletType(0.0001f, 0f);
        arc.lifetime = 16f;
        arc.hitEffect = Fx.hitLancer;
        arc.despawnEffect = Fx.none;
        arc.status = StatusEffects.shocked;
        arc.statusDuration = 10f;
        arc.hittable = false;
        arc.healPercent = 1.6f;
        arc.collidesTeam = true;
        heal.lightningType = arc;

        Weapon shotgun = new Weapon("heal-shotgun-weapon");
        shotgun.top = false; shotgun.x = 5f; shotgun.y = 0.5f; shotgun.shootY = 2.5f; shotgun.shake = 2.2f;
        shotgun.reload = 36f; shotgun.inaccuracy = 35f; shotgun.recoil = 2.5f;
        shotgun.shoot = shots(3, 0.5f);
        shotgun.ejectEffect = Fx.none;
        shotgun.shootSound = Sounds.shootArc;
        shotgun.bullet = heal;
        monoMetT1b.weapons.add(shotgun);

        monoMetT2 = miner("monomet-t2", 12f, 4.2f, 0.3f, 250f, 2.2f, 250, 150f, 0.5f, 4, 12.2f, true, Items.beryllium, Items.thorium, MetrumItems.iron);
        monoMetT2.weapons.add(massWeapon(true, 12f, shrapnel(15f, 15f, 120f, 5f, 3, "91aaed", "5e7ccb", 10f)));

        monoMetT2b = miner("monomet-t2b", 12f, 7.2f, 0.3f, 600f, 2.2f, 150, 175f, 0.5f, 4, 12.2f, false, Items.beryllium, Items.thorium, MetrumItems.iron);
        monoMetT2b.weapons.add(
            massWeapon(true, 12f, shrapnel(15f, 5f, 120f, 5f, 3, "91aaed", "5e7ccb", 10f)),
            massWeapon(false, 3f, shrapnel(6f, 5f, 40f, 12f, 5, "79dee0", "80e184", 15f)));

        monoMetT3 = miner("monomet-t3", 30f, 3.2f, 0.4f, 900f, 8.2f, 300, 190f, 1.6f, 4, 25f, true, Items.thorium);
        monoMetT3.weapons.add(
            massWeapon(true, 12f, shrapnel(15f, 5f, 120f, 5f, 3, "91aaed", "5e7ccb", 10f)),
            missile(true, 60f, 4, 5f, missileBullet(6f, 30f, 50f, 25f, 10f, "ffff00", "00ffff", 100f, 60f)));

        monoMetT3b = miner("monomet-t3b", 40f, 2.2f, 0.4f, 1200f, 8.2f, 500, 220f, 1.2f, 4, 30f, false,
            Items.thorium, Items.beryllium, Items.graphite);
        MissileBulletType wave = missileBullet(6f, 20f, 50f, 50f, 10f, "ffff00", "00ffff", 100f, 60f);
        wave.width = 6f;
        wave.height = 8f;
        WaveEffect ring = new WaveEffect();
        ring.lifetime = 60f;
        ring.sizeFrom = ring.sizeTo = 24f;
        ring.strokeFrom = 4f;
        ring.strokeTo = 0f;
        ring.lightColor = Color.valueOf("ffecd1");
        ring.colorFrom = ring.colorTo = Color.valueOf("e3a76a");
        wave.hitEffect = new MultiEffect(ring);
        monoMetT3b.weapons.add(
            massWeapon(true, 12f, shrapnel(15f, 5f, 120f, 5f, 3, "91aaed", "5e7ccb", 10f)),
            missile(false, 30f, 6, 35f, wave),
            missile(true, 140f, 1, 25f, missileBullet(8f, 300f, 60f, 30f, 60f, "e00600", "e0c706", 160f, 80f)));

        loadBoosters();
    }

    /**
     * C variants (and every later upgrade) are booster craft: they carry material and spend it
     * to give nearby allies +20% damage and range. Built from the B variant, keeping its weapons.
     */
    private static void loadBoosters() {
        monoMetT1c = booster("monomet-t1c", monoMetT1b, metrum.Balance.f("boost.c.rangeT1", 8f), metrum.Balance.f("upgrade.c.healthT1", 1.5f), metrum.Balance.f("boost.c.seconds", 10f) * 60f);
        monoMetT2c = booster("monomet-t2c", monoMetT2b, metrum.Balance.f("boost.c.rangeT2", 10f), metrum.Balance.f("upgrade.c.healthT2", 1.5f), metrum.Balance.f("boost.c.seconds", 10f) * 60f);
        monoMetT3c = booster("monomet-t3c", monoMetT3b, metrum.Balance.f("boost.c.rangeT3", 12f), metrum.Balance.f("upgrade.c.healthT3", 1.4f), metrum.Balance.f("boost.c.seconds", 10f) * 60f);
        armT3c(monoMetT3c);
        // D: wider field, boosts every 7.5 s instead of 10 s
        monoMetT1d = booster("monomet-t1d", monoMetT1c, metrum.Balance.f("boost.d.rangeT1", 11f), metrum.Balance.f("upgrade.d.healthT1", 1.4f), metrum.Balance.f("boost.d.seconds", 7.5f) * 60f);
        monoMetT2d = booster("monomet-t2d", monoMetT2c, metrum.Balance.f("boost.d.rangeT2", 13f), metrum.Balance.f("upgrade.d.healthT2", 1.4f), metrum.Balance.f("boost.d.seconds", 7.5f) * 60f);
        monoMetT3d = booster("monomet-t3d", monoMetT3c, metrum.Balance.f("boost.d.rangeT3", 15f), metrum.Balance.f("upgrade.d.healthT3", 1.35f), metrum.Balance.f("boost.d.seconds", 7.5f) * 60f);
        // 3D: shield generator - blocks 100% explosions, 60% physical; lasers/flames/slag/poison pass through
        metrum.shields.ResistShieldAbility gen = new metrum.shields.ResistShieldAbility();
        gen.maxHealth = 500f; gen.regenPerSecond = 2f; gen.breakTime = 60f * 60f; gen.restoreHealth = 100f;
        gen.explosiveResist = 1f; gen.physicalResist = 0.6f; gen.radius = monoMetT3d.hitSize * 1.1f;
        monoMetT3d.abilities.add(gen);
    }

    /**
     * Each upgrade (B -> C -> D) is bigger, slower, tougher and carries one more weapon,
     * keeping the line's role (T1 scout/miner/repair, T2 healer, T3 missile crusher).
     * C and above also carry boost material: mined ore counts, so they keep mining.
     */
    /** T3C/T3D loadout: fire laser + heavy 2x5 missile pods + slow homing torpedoes */
    private static void armT3c(UnitType u) {
        u.weapons.clear();

        // fire laser (Victor-style death ray): 2 s later an aftershock runs along the beam, 30% damage, burning ground
        Weapon laser = new Weapon(P + "fire-laser");
        laser.x = 0f; laser.y = 4f; laser.mirror = false; laser.reload = 150f; laser.shake = 3f;
        laser.shootSound = Sounds.shootLaser;
        laser.bullet = new metrum.entities.AftershockLaserBulletType(140f) {{
            length = 200f; width = 14f;
            colors = new Color[]{Color.valueOf("ff8a3c55"), Color.valueOf("ff8a3c"), Color.white};
            status = StatusEffects.burning; statusDuration = 120f;
        }};

        // heavy pods: 2 x 5 missiles, 30 dmg, range 150 shorter than the old pods (300 -> 150)
        Weapon pods = new Weapon(P + "missile-pod-heavy");
        pods.x = 9f; pods.y = -2f; pods.mirror = true; pods.alternate = true; pods.reload = 80f;
        pods.shoot = shots(5, 4f); pods.inaccuracy = 10f; pods.shake = 1.5f;
        pods.shootSound = Sounds.shootMissile;
        // range 150 + 30 = 180 (lifetime 30 at speed 6); blast ring at 50% of the old T3B ring (24 -> 12)
        MissileBulletType pb = missileBullet(6f, 30f, 30f, 20f, 10f, "ffff00", "dc3c3c", 80f, 50f);
        WaveEffect podRing = new WaveEffect();
        podRing.lifetime = 40f;
        podRing.sizeFrom = podRing.sizeTo = 12f;
        podRing.strokeFrom = 3f;
        podRing.strokeTo = 0f;
        podRing.colorFrom = podRing.colorTo = Color.valueOf("e3a76a");
        pb.hitEffect = new MultiEffect(Fx.blastExplosion, podRing);
        // own art: red-nosed rocket; back sprite = exhaust glow tinted by backColor
        pb.sprite = "metrum-rocket";
        pb.frontColor = Color.white;
        pb.width = 7f; pb.height = 16f;
        pods.bullet = pb;

        // side torpedoes: slow, homing, 300 dmg, range 800, once per 30 s
        Weapon torpedo = new Weapon(P + "torpedo-launcher");
        torpedo.x = 14f; torpedo.y = -6f; torpedo.mirror = true; torpedo.alternate = false; torpedo.reload = 1800f;
        torpedo.shootSound = Sounds.shootMissileLarge;
        // full 300 on a direct hit; area blast at 75% of the old radius (32 -> 24) dealing only 20% (60)
        MissileBulletType tb = missileBullet(2f, 300f, 400f, 24f, 60f, "ffe08a", "e6be28", 200f, 200f);
        WaveEffect torpRing = new WaveEffect();
        torpRing.lifetime = 45f;
        torpRing.sizeFrom = 4f;
        torpRing.sizeTo = 24f;
        torpRing.strokeFrom = 4f;
        torpRing.strokeTo = 0f;
        torpRing.colorFrom = torpRing.colorTo = Color.valueOf("e6be28");
        tb.hitEffect = new MultiEffect(Fx.massiveExplosion, torpRing);
        tb.sprite = "metrum-torpedo";
        tb.frontColor = Color.white;
        tb.width = 10f; tb.height = 26f;
        tb.homingPower = 0.05f; tb.trailLength = 12; tb.trailColor = Color.valueOf("e6be28");
        torpedo.bullet = tb;

        u.weapons.addAll(laser, pods, torpedo);
    }

    private static UnitType booster(String name, UnitType from, float rangeTiles, float healthMul, float reload) {
        ErekirUnitType u = new ErekirUnitType(name);
        // C and above can lift and place hull turrets (2x2 payloads)
        u.constructor = mindustry.gen.PayloadUnit::create;
        u.payloadCapacity = (2f * 8f) * (2f * 8f);
        u.pickupUnits = false;
        // default: escort/assist the army (boosting needs allies nearby). Mining stays available
        // as a manual command; MinerAI would keep dumping the boost material into the core.
        u.defaultCommand = UnitCommand.assistCommand;
        u.flying = true;
        u.drag = from.drag;
        u.hitSize = from.hitSize * metrum.Balance.f("upgrade.sizeMul", 1.25f);
        u.speed = from.speed * metrum.Balance.f("upgrade.speedMul", 0.8f);
        u.accel = from.accel * 0.9f;
        u.health = from.health * healthMul;
        u.armor = from.armor + 2f;
        u.engineSize = from.engineSize * 1.2f;
        u.engineOffset = from.engineOffset * 1.2f;
        u.itemCapacity = (int) (from.itemCapacity * 1.25f);
        u.range = u.maxRange = from.range * 1.1f;
        u.buildSpeed = from.buildSpeed * 1.2f;
        u.mineTier = from.mineTier;
        u.mineSpeed = from.mineSpeed * 1.2f;
        u.mineWalls = from.mineWalls;
        u.mineFloor = from.mineFloor;
        u.mineItems = from.mineItems;
        u.drawCell = false;
        u.researchCostMultiplier = 0f;
        for (Weapon w : from.weapons) u.weapons.add(w.copy());
        // one extra weapon: a further-out copy of the line's main gun
        if (from.weapons.any()) {
            Weapon extra = from.weapons.first().copy();
            extra.x = Math.abs(extra.x) + u.hitSize * 0.3f;
            extra.y -= u.hitSize * 0.15f;
            extra.reload *= metrum.Balance.f("upgrade.extraWeaponReloadMul", 1.15f);
            u.weapons.add(extra);
        }
        metrum.boost.BoostAbility boost = new metrum.boost.BoostAbility();
        boost.range = rangeTiles * mindustry.Vars.tilesize;
        boost.reload = reload;
        boost.duration = reload;
        boost.tankCapacity = Math.max(40, u.itemCapacity / 2);
        u.abilities.add(boost);
        return u;
    }

    private static UnitType miner(String name, float hitSize, float speed, float accel, float health, float engineSize,
                                  int itemCapacity, float range, float buildSpeed, int mineTier, float mineSpeed,
                                  boolean hasCell, Item... mineItems) {
        ErekirUnitType u = new ErekirUnitType(name);
        u.constructor = UnitEntity::create;
        // no fixed controller: player units get CommandAI (shift+click RTS commands work),
        // starting with the mine command; enemy/AI teams fall back to the type's default AI
        u.defaultCommand = UnitCommand.mineCommand;
        u.flying = true;
        u.mineWalls = true;
        u.mineFloor = true;
        u.drag = 0.06f;
        u.hitSize = hitSize;
        u.speed = speed;
        u.accel = accel;
        u.health = health;
        u.engineSize = engineSize;
        u.engineOffset = 5.7f;
        u.itemCapacity = itemCapacity;
        u.range = u.maxRange = range;
        u.buildSpeed = buildSpeed;
        u.mineTier = mineTier;
        u.mineSpeed = mineSpeed;
        u.mineItems = Seq.with(mineItems);
        u.drawCell = hasCell;
        u.researchCostMultiplier = 0f;
        return u;
    }

    private static ShootPattern shots(int shots, float delay) {
        ShootPattern p = new ShootPattern();
        p.shots = shots;
        p.shotDelay = delay;
        return p;
    }

    private static ShrapnelBulletType shrapnel(float lifetime, float damage, float length, float width, int pierceCap,
                                               String from, String to, float heal) {
        ShrapnelBulletType b = new ShrapnelBulletType();
        b.lifetime = lifetime;
        b.damage = damage;
        b.length = length;
        b.width = width;
        b.pierceCap = pierceCap;
        b.serrations = 1;
        b.fromColor = Color.valueOf(from);
        b.toColor = Color.valueOf(to);
        b.hitEffect = b.shootEffect = Fx.blastExplosion;
        b.healAmount = heal;
        b.healColor = Color.valueOf(to);
        return b;
    }

    private static Weapon massWeapon(boolean top, float reload, BulletType bullet) {
        Weapon w = new Weapon(P + "c1-mass-weapon");
        w.top = top; w.x = -4.2f; w.y = 2.7f; w.reload = reload;
        w.mirror = true; w.alternate = true;
        w.shootSound = Sounds.shootSap;
        w.bullet = bullet;
        return w;
    }

    private static MissileBulletType missileBullet(float speed, float damage, float lifetime, float splashRadius,
                                                   float splashDamage, String front, String back,
                                                   float homingPower, float homingRange) {
        MissileBulletType b = new MissileBulletType(speed, damage);
        b.lifetime = lifetime;
        b.splashDamageRadius = splashRadius;
        b.splashDamage = splashDamage;
        b.frontColor = Color.valueOf(front);
        b.backColor = Color.valueOf(back);
        b.homingPower = homingPower;
        b.homingRange = homingRange;
        return b;
    }

    private static Weapon missile(boolean top, float reload, int shots, float inaccuracy, BulletType bullet) {
        Weapon w = new Weapon(P + "missile-put");
        w.top = top; w.x = 5f; w.y = -3.5f; w.reload = reload; w.shake = 1f;
        w.shoot = shots(shots, 0f);
        w.inaccuracy = inaccuracy;
        w.alternate = true;
        w.bullet = bullet;
        return w;
    }
}
