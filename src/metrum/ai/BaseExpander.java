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
    private static final Seq<Building> placed = new Seq<>();

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
        Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> placed.clear());
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
        if (boostTimer >= 600f) {
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
            for (int i = 0; i < 10 && t.totalAmmo < turret.maxAmmo / 2; i++) {
                mindustry.type.Item pick = null;
                for (mindustry.type.Item it : turret.ammoTypes.keys()) {
                    if (core.items.has(it) && t.acceptItem(core, it)) { pick = it; break; }
                }
                if (pick == null) break;
                core.items.remove(pick, 1);
                t.handleItem(core, pick);
            }
        }
    }

    /** late waves: 25% of enemy units (40% after wave 30) get Metrum's boost for 10 s */
    static void boostEnemies() {
        int wave = Vars.state.wave;
        if (wave < boostFromWave || metrum.boost.BoostSystem.boosted == null) return;
        float share = wave >= 30 ? 0.4f : 0.25f;
        Vars.state.rules.waveTeam.data().units.each(u -> {
            if (Mathf.chance(share)) u.apply(metrum.boost.BoostSystem.boosted, 600f);
        });
    }
}
