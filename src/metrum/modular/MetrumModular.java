package metrum.modular;

import arc.graphics.Color;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import metrum.content.MetrumItems;
import metrum.modular.ModularUnitType.ModuleWeapon;

/** Modular drone hulls. Every hull carries all module weapons; only mounted ones are active. */
public class MetrumModular {
    public static ModularUnitType droneLight, droneMedium, droneHeavy;
    public static DroneAssembler assembler;

    public static void load() {
        droneLight = hull("modular-light", MetrumItems.hullLight, 2, 8f, 3.4f, 260f, 0f);
        droneMedium = hull("modular-medium", MetrumItems.hullMedium, 3, 12f, 2.6f, 620f, 3f);
        droneHeavy = hull("modular-heavy", MetrumItems.hullHeavy, 4, 18f, 1.8f, 1400f, 7f);

        assembler = new DroneAssembler("drone-assembler");
        assembler.requirements(mindustry.type.Category.units, mindustry.type.ItemStack.with(MetrumItems.steel, 150, Items.silicon, 150, Items.tungsten, 60, MetrumItems.berylliumBronze, 30));
        assembler.size = 4;
        assembler.consumePower(3f);
        assembler.buildTime = metrum.Balance.f("assembler.buildSeconds", 60f) * 60f;
        assembler.hulls.addAll(droneLight, droneMedium, droneHeavy);
        assembler.modules.addAll(MetrumItems.weaponMiningLaser, MetrumItems.weaponMachineGun, MetrumItems.weaponMissiles,
            MetrumItems.weaponExplosives, MetrumItems.weaponFuelTank, MetrumItems.partEngine, MetrumItems.partWings, MetrumItems.partLegs);
    }

    static ModularUnitType hull(String name, mindustry.type.Item hullItem, int slots, float size, float speed, float health, float armor) {
        ModularUnitType u = new ModularUnitType(name, hullItem, slots);
        u.constructor = UnitEntity::create;
        u.flying = true;
        u.hitSize = size;
        u.speed = speed;
        u.accel = 0.2f;
        u.drag = 0.06f;
        u.health = health;
        u.armor = armor;
        u.engineSize = size / 5f;
        u.engineOffset = size / 2f;
        u.itemCapacity = 40 + slots * 20;
        u.mineTier = 3;
        u.mineSpeed = 4f + slots;
        u.mineItems = arc.struct.Seq.with(Items.beryllium, Items.graphite, Items.tungsten, Items.thorium, MetrumItems.iron);
        u.defaultCommand = UnitCommand.moveCommand;
        u.drawCell = false;
        u.alwaysUnlocked = true;
        float off = size * 0.35f;

        ModuleWeapon gun = new ModuleWeapon("metrum-c1-mass-weapon", MetrumItems.weaponMachineGun);
        gun.x = off; gun.y = 1f; gun.reload = 8f; gun.mirror = true; gun.alternate = true;
        gun.shootSound = Sounds.shoot;
        gun.bullet = new BasicBulletType(4.5f, 14f) {{ lifetime = 35f; width = 6f; height = 9f; }};

        ModuleWeapon missiles = new ModuleWeapon("metrum-missile-put", MetrumItems.weaponMissiles);
        missiles.x = off * 0.6f; missiles.y = -2f; missiles.reload = 70f; missiles.mirror = true; missiles.alternate = true;
        missiles.shootSound = Sounds.shootMissile;
        missiles.bullet = new MissileBulletType(4f, 30f) {{
            lifetime = 55f; splashDamage = 25f; splashDamageRadius = 24f; homingPower = 0.08f;
            frontColor = Color.white; backColor = Color.valueOf("dc3c3c");
        }};

        ModuleWeapon bombs = new ModuleWeapon("", MetrumItems.weaponExplosives);
        bombs.x = 0f; bombs.mirror = false; bombs.reload = 90f; bombs.shootCone = 180f; bombs.ignoreRotation = true;
        bombs.shootSound = Sounds.none;
        bombs.bullet = new BombBulletType(80f, 30f) {{ hitEffect = Fx.massiveExplosion; }};

        u.weapons.addAll(gun, missiles, bombs);
        return u;
    }
}
