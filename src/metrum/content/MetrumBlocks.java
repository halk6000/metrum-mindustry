package metrum.content;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.meta.Attribute;

import static mindustry.type.ItemStack.with;

/** Blocks ported from the original Metrum JSON mod. */
public class MetrumBlocks {
    public static Block oreIron, oreGraphite;
    public static Block colorBridge;
    public static Block quarkCannon, carborundumBore, hullGunTurret, hullMissileTurret;
    public static Block carborundumWall, carborundumWallLarge, tungstenCarbideWall, tungstenCarbideWallLarge;
    public static Block streamDrillT1, streamDrillT1Adv, streamDrillT2, streamDrillT2Adv, rockScraper;

    public static void load() {
        // iron ore: generated on Ferrox, mineable by drills and MonoMet drones
        oreIron = new mindustry.world.blocks.environment.OreBlock("ore-iron", MetrumItems.iron);
        // the base game only has graphite in walls; open-pit deposits need a floor ore
        oreGraphite = new mindustry.world.blocks.environment.OreBlock("ore-graphite", Items.graphite);

        // circuit-coloured duct bridge: links only to bridges of the same colour
        colorBridge = new metrum.world.ColorDuctBridge("color-bridge") {{
            requirements(Category.distribution, with(Items.beryllium, 25, Items.graphite, 5));
            health = 90;
            speed = 4f;
            range = 4;
            buildCostMultiplier = 2f;
        }};

        // carborundum-tipped beam drill: mines everything Erekir has, carborundum bits triple its speed
        carborundumBore = new metrum.world.BoostedBeamDrill("carborundum-bore") {{
            boostItem = MetrumItems.carborundum;
            requirements(Category.production, with(MetrumItems.ferrotungsten, 60, MetrumItems.steel, 80, Items.silicon, 80, Items.tungsten, 60));
            size = 3;
            tier = 6;
            drillTime = 90f;
            range = 8;
            optionalBoostIntensity = 3f;
            itemCapacity = 30;
            consumePower(2f);
            consumeItem(MetrumItems.carborundum).boost();
        }};

        // hull turrets: a hull + weapon module made into a 2x2 turret; C+ drones can lift and re-place them.
        // Ammo: 1 ammo crate per shot.
        hullGunTurret = new mindustry.world.blocks.defense.turrets.ItemTurret("hull-gun-turret") {{
            requirements(Category.turret, with(MetrumItems.hullMedium, 1, MetrumItems.weaponMachineGun, 1, MetrumItems.steel, 20));
            size = 2;
            health = 1100;
            range = 190f;
            reload = 10f;
            rotateSpeed = 8f;
            shootSound = mindustry.gen.Sounds.shoot;
            ammo(MetrumItems.ammoCrate, new mindustry.entities.bullet.BasicBulletType(5f, 22f) {{
                lifetime = 38f; width = 7f; height = 10f; ammoMultiplier = 1f;
            }});
        }};
        hullMissileTurret = new mindustry.world.blocks.defense.turrets.ItemTurret("hull-missile-turret") {{
            requirements(Category.turret, with(MetrumItems.hullMedium, 1, MetrumItems.weaponMissiles, 1, MetrumItems.steel, 20));
            size = 2;
            health = 1000;
            range = 260f;
            reload = 55f;
            rotateSpeed = 5f;
            shoot.shots = 2;
            shoot.shotDelay = 6f;
            shootSound = mindustry.gen.Sounds.shootMissile;
            ammo(MetrumItems.ammoCrate, new mindustry.entities.bullet.MissileBulletType(4.2f, 35f) {{
                lifetime = 62f; splashDamage = 40f; splashDamageRadius = 28f; homingPower = 0.08f; ammoMultiplier = 1f;
                backColor = arc.graphics.Color.valueOf("dc3c3c");
            }});
        }};

        // endgame turret fed with collider products
        quarkCannon = new mindustry.world.blocks.defense.turrets.ItemTurret("quark-cannon") {{
            requirements(Category.turret, with(MetrumItems.tungstenCarbide, 150, MetrumItems.ferrotungsten, 120, Items.surgeAlloy, 100, Items.silicon, 200));
            size = 3;
            health = 3200;
            range = 330f;
            reload = 70f;
            recoil = 3f;
            shake = 3f;
            rotateSpeed = 2.5f;
            shootY = 10f;
            outlineColor = mindustry.graphics.Pal.darkOutline;
            drawer = new mindustry.world.draw.DrawTurret("reinforced-");
            consumePower(4f);
            coolant = consumeCoolant(0.3f);
            shootSound = mindustry.gen.Sounds.shootQuad;
            ammo(
                // beryllium bronze: fast, piercing slugs
                MetrumItems.berylliumBronze, new mindustry.entities.bullet.BasicBulletType(9f, 140f) {{
                    width = 10f; height = 18f; lifetime = 37f; pierceCap = 3; pierce = true;
                    frontColor = arc.graphics.Color.valueOf("ffe2a0"); backColor = arc.graphics.Color.valueOf("c8a050");
                    reloadMultiplier = 1.6f; ammoMultiplier = 2f;
                    hitEffect = mindustry.content.Fx.hitBulletColor;
                }},
                // quark matter: slow orb, huge splash
                MetrumItems.quarkMatter, new mindustry.entities.bullet.BasicBulletType(5f, 120f) {{
                    width = height = 16f; lifetime = 66f; sprite = "large-orb";
                    frontColor = arc.graphics.Color.white; backColor = arc.graphics.Color.valueOf("ff50c8");
                    splashDamage = 650f; splashDamageRadius = 60f;
                    status = mindustry.content.StatusEffects.melting; statusDuration = 240f;
                    hitEffect = mindustry.content.Fx.massiveExplosion; despawnEffect = mindustry.content.Fx.massiveExplosion;
                    hitShake = 6f; ammoMultiplier = 1f;
                }}
            );
        }};

        // collider-product walls: carborundum = light + deflects bullets, tungsten carbide = heaviest armour
        carborundumWall = wall("carborundum-wall", 1, MetrumItems.carborundum, 6, 1400, 0.25f);
        carborundumWallLarge = wall("carborundum-wall-large", 2, MetrumItems.carborundum, 24, 1400 * 4, 0.25f);
        tungstenCarbideWall = wall("tungsten-carbide-wall", 1, MetrumItems.tungstenCarbide, 6, 2600, 0f);
        tungstenCarbideWallLarge = wall("tungsten-carbide-wall-large", 2, MetrumItems.tungstenCarbide, 24, 2600 * 4, 0f);

        streamDrillT1 = streamDrill("stream-drill-t1", 3, 150f, 0.09f, 0.001f, 1.25f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 65));
        streamDrillT1Adv = streamDrill("stream-drill-t1-adv", 4, 100f, 0.12f, 0.001f, 1.15f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 60, MetrumItems.iron, 60));
        streamDrillT2 = streamDrill("stream-drill-t2", 3, 150f, 0.08f, 0.006f, 1.75f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 80));
        streamDrillT2Adv = streamDrill("stream-drill-t2-adv", 4, 120f, 0.06f, 0.025f, 3f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 80, MetrumItems.iron, 90));
        metrum.world.Upgrades.family(streamDrillT1, streamDrillT1Adv, streamDrillT2, streamDrillT2Adv);

        rockScraper = new WallCrafter("rock-scraper") {{
            requirements(Category.production, with(Items.silicon, 100, Items.beryllium, 200, Items.graphite, 200));
            consumePower(0.6f);
            output = MetrumItems.pieceRock;
            attribute = Attribute.sand;
            drillTime = 40f;
            size = 2;
        }};
    }

    private static mindustry.world.blocks.defense.Wall wall(String name, int size, mindustry.type.Item item, int amount, int health, float deflect) {
        mindustry.world.blocks.defense.Wall w = new mindustry.world.blocks.defense.Wall(name);
        w.requirements(Category.defense, with(item, amount));
        w.size = size;
        w.health = health;
        w.armor = deflect > 0f ? 8f : 14f;
        if (deflect > 0f) {
            w.chanceDeflect = deflect * 40f;
            w.flashHit = true;
        }
        w.buildCostMultiplier = 5f;
        return w;
    }

    // NOTE: no anonymous {{ }} block here - inside it, parameters named like fields (tier, drillTime)
    // are shadowed by the inherited fields, so "this.tier = tier" assigned the field to itself (tier stayed 1).
    private static BeamDrill streamDrill(String name, int drillTier, float time, float power,
                                         float steamUse, float boost, ItemStack[] req) {
        BeamDrill d = new BeamDrill(name) {
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };
        d.requirements(Category.production, req);
        d.tier = drillTier;
        d.drillTime = time;
        d.size = 2;
        d.range = 6;
        d.optionalBoostIntensity = boost;
        d.itemCapacity = 20;
        d.consumePower(power);
        d.consumeLiquid(MetrumLiquids.steam, steamUse).boost();
        return d;
    }
}
