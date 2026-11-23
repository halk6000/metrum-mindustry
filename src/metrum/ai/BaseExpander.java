package metrum.ai;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/**
 * Supplements vanilla buildAi/rtsAi on Metrum planets: the enemy team periodically
 * fortifies its cores, paying from its core storage. Defences grow with the wave number.
 */
public class BaseExpander {
    /** ticks between build attempts per core */
    public static float interval = 60f * 20f;
    public static float minRadius = 6f, maxRadius = 22f;
    public static final Seq<Planet> planets = new Seq<>();

    private static float timer, supplyTimer, boostTimer;
    /** from this wave on, the enemy's own "booster craft" empower a share of its army */
    public static int boostFromWave = 15;
    /** buildings placed by the expander, restocked from the nearest core */
    public static final Seq<Building> placed = new Seq<>();

    /** tiers of defences, unlocked as waves progress */
    static Block[][] tiers;

    public static void init() {
        tiers = new Block[][]{
            {Blocks.breach, Blocks.berylliumWall, Blocks.berylliumWallLarge},
            // only item-fed turrets: the expander can stock them from the core
            {Blocks.diffuse, Blocks.breach, Blocks.tungstenWallLarge},
            {Blocks.titan, Blocks.disperse, Blocks.carbideWallLarge},
            {Blocks.scathe, Blocks.smite, Blocks.titan, Blocks.reinforcedSurgeWallLarge},
        };
        Events.run(Trigger.update, BaseExpander::update);
        Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            placed.clear();
            timer = supplyTimer = boostTimer = 0f;
            if (Vars.net.client() || !active()) return;
            // Rebuild supply tracking from persisted enemy defences; multiblock tiles share one building.
            for (Tile tile : Vars.world.tiles) {
                Building b = tile.build;
                if (b != null && b.tile == tile && b.team == Vars.state.rules.waveTeam
                    && b instanceof mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild) placed.add(b);
            }
        });
    }

    static boolean active() {
        return Vars.state.isCampaign() && Vars.state.rules.sector != null
            && planets.contains(Vars.state.rules.sector.planet)
            && Vars.state.rules.waveTeam.cores().size > 0;
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
        supplyTimer += Time.delta;
        if (supplyTimer >= 60f * 5f) {
            supplyTimer = 0f;
            supply();
        }
        boostTimer += Time.delta;
        if (boostTimer >= 60f * 30f) {
            boostTimer = 0f;
            boostEnemies();
        }
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        Team team = Vars.state.rules.waveTeam;
        int maxTier = Math.min(Vars.state.wave / 10, tiers.length - 1);
        for (CoreBuild core : team.cores()) {
            Block[] pool = tiers[Mathf.random(maxTier)];
            Block block = pool[Mathf.random(pool.length - 1)];
            tryPlace(core, block);
        }
    }

    static void tryPlace(CoreBuild core, Block block) {
        if (!block.environmentBuildable() || !hasItems(core, block)) return;
        for (int attempt = 0; attempt < 12; attempt++) {
            float ang = Mathf.random(360f), dst = Mathf.random(minRadius, maxRadius);
            int tx = core.tileX() + (int) (Mathf.cosDeg(ang) * dst);
            int ty = core.tileY() + (int) (Mathf.sinDeg(ang) * dst);
            Tile tile = Vars.world.tile(tx, ty);
            if (tile == null) continue;
            if (!Build.validPlace(block, core.team, tx, ty, 0, false)) continue;
            for (ItemStack s : block.requirements) core.items.remove(s.item, Math.round(s.amount * Vars.state.rules.buildCostMultiplier));
            tile.setNet(block, core.team, 0);
            if (tile.build != null) placed.add(tile.build);
            return;
        }
    }

    static boolean hasItems(Building core, Block block) {
        for (ItemStack s : block.requirements) {
            if (core.items.get(s.item) < Math.round(s.amount * Vars.state.rules.buildCostMultiplier)) return false;
        }
        return true;
    }

    /** tops up ammo of expander-built item turrets from their team's closest core */
    static void supply() {
        placed.removeAll(b -> !b.isValid());
        for (Building b : placed) {
            if (!(b instanceof mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild)) continue;
            mindustry.world.blocks.defense.turrets.ItemTurret turret = (mindustry.world.blocks.defense.turrets.ItemTurret) b.block;
            mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild t = (mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild) b;
            CoreBuild core = b.closestCore();
            if (core == null) continue;
            Building magazine = b.proximity.find(other -> other.team == b.team
                && other.block == Blocks.reinforcedContainer && other.items != null);
            if (magazine != null) {
                for (mindustry.type.Item ammo : turret.ammoTypes.keys()) {
                    int amount = Math.min(20 - magazine.items.get(ammo), core.items.get(ammo));
                    amount = Math.min(amount, magazine.getMaximumAccepted(ammo) - magazine.items.get(ammo));
                    if (amount > 0) {
                        core.items.remove(ammo, amount);
                        magazine.items.add(ammo, amount);
                    }
                }
            }
            for (int i = 0; i < 10 && t.totalAmmo < turret.maxAmmo / 2; i++) {
                mindustry.type.Item pick = null;
                Building source = core;
                for (mindustry.type.Item it : turret.ammoTypes.keys()) {
                    if (!t.acceptItem(core, it)) continue;
                    if (magazine != null && magazine.items.has(it)) { pick = it; source = magazine; break; }
                    if (core.items.has(it)) { pick = it; break; }
                }
                if (pick == null) break;
                source.items.remove(pick, 1);
                t.handleItem(source, pick);
            }
        }
    }

    /**
     * Late waves: the enemy fields real booster craft (MonoMet T2C/T3C) that follow its army
     * and boost it with material from its core. Shoot them down to stop the boosts.
     */
    static void boostEnemies() {
        int wave = Vars.state.wave;
        if (wave < boostFromWave) return;
        Team team = Vars.state.rules.waveTeam;
        mindustry.type.UnitType type = wave >= 30 ? metrum.content.MetrumUnits.monoMetT3c : metrum.content.MetrumUnits.monoMetT2c;
        int max = 1 + (wave - boostFromWave) / 10;
        int have = team.data().countType(metrum.content.MetrumUnits.monoMetT2c) + team.data().countType(metrum.content.MetrumUnits.monoMetT3c);
        if (have >= max || team.cores().isEmpty() || !type.supportsEnv(Vars.state.rules.env)) return;
        CoreBuild core = team.cores().random();
        mindustry.gen.Unit u = type.spawn(team, core.x + Mathf.range(16f), core.y + Mathf.range(16f));
        u.controller(new BoosterAI());
    }
}
