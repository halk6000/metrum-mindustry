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

    private static float timer;

    /** tiers of defences, unlocked as waves progress */
    static Block[][] tiers;

    public static void init() {
        tiers = new Block[][]{
            {Blocks.breach, Blocks.berylliumWall, Blocks.berylliumWallLarge},
            {Blocks.diffuse, Blocks.sublimate, Blocks.tungstenWallLarge, Blocks.radar},
            {Blocks.titan, Blocks.afflict, Blocks.disperse, Blocks.carbideWallLarge},
            {Blocks.lustre, Blocks.scathe, Blocks.smite, Blocks.reinforcedSurgeWallLarge},
        };
        Events.run(Trigger.update, BaseExpander::update);
    }

    static boolean active() {
        return Vars.state.isCampaign() && Vars.state.rules.sector != null
            && planets.contains(Vars.state.rules.sector.planet)
            && Vars.state.rules.waveTeam.cores().size > 0;
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
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
            return;
        }
    }

    static boolean hasItems(Building core, Block block) {
        for (ItemStack s : block.requirements) {
            if (core.items.get(s.item) < Math.round(s.amount * Vars.state.rules.buildCostMultiplier)) return false;
        }
        return true;
    }
}
