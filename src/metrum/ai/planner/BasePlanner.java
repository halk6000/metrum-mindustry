package metrum.ai.planner;

import arc.Events;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.Astar;
import mindustry.content.Blocks;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/**
 * Metrum AI library - base planner. Builds the enemy base step by step, in this order:
 *  1. drills on ore near the core + a conveyor/duct path to the core   (this file, step DRILLS)
 *  2. power (geothermal/generators) wired to consumers                (TODO)
 *  3. water to whatever needs it                                       (TODO)
 *  4. a splitter on a resource line feeding a factory                   (TODO)
 *  5. turrets (+ ammo magazines from HARD up)                           (TODO)
 *  6. walls, leaving a gap where the fire is heaviest                   (TODO)
 * Pays from the core like a player would; difficulty = {@link AiLevel}.
 */
public class BasePlanner {
    public static final Seq<mindustry.type.Planet> planets = new Seq<>();
    static float timer;

    public static void init() {
        Events.run(Trigger.update, BasePlanner::update);
    }

    static boolean active() {
        return Vars.state.isCampaign() && Vars.state.rules.sector != null
            && planets.contains(Vars.state.rules.sector.planet) && Vars.state.rules.waveTeam.cores().size > 0;
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
        AiLevel level = AiLevel.current();
        timer += Time.delta;
        if (timer < level.buildInterval * 60f) return;
        timer = 0f;
        Team team = Vars.state.rules.waveTeam;
        for (CoreBuild core : team.cores()) {
            for (int i = 0; i < level.stepsPerTick; i++) {
                if (!step(core, level)) break;
            }
        }
    }

    /** picks the next build step in the planned order */
    static boolean step(CoreBuild core, AiLevel level) {
        int drills = count(core, b -> b.block instanceof mindustry.world.blocks.production.BeamDrill
            || b.block instanceof mindustry.world.blocks.production.Drill);
        int wantDrills = 2 + level.ordinal();
        if (drills < 2) return buildDrillLine(core, level);
        if (!hasGenerator(core) && buildPower(core)) return true;
        if (!powerGridDone(core) && extendPowerGrid(core)) return true;
        if (drills < wantDrills) return buildDrillLine(core, level);
        return false; // next steps (water, factories, turrets, walls) come later
    }

    static int count(CoreBuild core, arc.func.Boolf<Building> pred) {
        int[] n = {0};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize, pred, b -> n[0]++);
        return n[0];
    }

    // ---------- step 2: power - geothermal (turbine condenser on steam vents) + beam-node grid ----------

    static boolean hasGenerator(CoreBuild core) {
        return count(core, b -> b.block instanceof mindustry.world.blocks.power.PowerGenerator) > 0;
    }

    static boolean buildPower(CoreBuild core) {
        Block gen = Blocks.turbineCondenser;
        Tile best = null;
        float bestDst = Float.MAX_VALUE;
        for (int dx = -35; dx <= 35; dx++) for (int dy = -35; dy <= 35; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || !(t.floor() instanceof mindustry.world.blocks.environment.SteamVent)) continue;
            if (!((mindustry.world.blocks.environment.SteamVent) t.floor()).isCenterVent(t)) continue;
            if (!Build.validPlace(gen, core.team, t.x, t.y, 0, false)) continue;
            float d = core.dst(t);
            if (d < bestDst) { bestDst = d; best = t; }
        }
        if (best == null || !pay(core, gen.requirements)) return false;
        best.setNet(gen, core.team, 0);
        return true;
    }

    /** beam nodes on a lattice (every 8 tiles) between generators and consumers; nodes auto-link in straight lines */
    static boolean powerGridDone(CoreBuild core) {
        int[] unpowered = {0};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block.consPower != null && b.power != null && b.power.graph.getPowerBalance() <= 0f && b.power.graph.getLastPowerProduced() <= 0f,
            b -> unpowered[0]++);
        return unpowered[0] == 0;
    }

    static boolean extendPowerGrid(CoreBuild core) {
        Block node = Blocks.beamNode;
        int step = 8, placed = 0;
        for (int r = step; r <= 32 && placed < 3; r += step) {
            for (int dx = -r; dx <= r && placed < 3; dx += step) for (int dy = -r; dy <= r && placed < 3; dy += step) {
                if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
                if (t == null || t.block() != Blocks.air || !Build.validPlace(node, core.team, t.x, t.y, 0, false)) continue;
                if (!pay(core, node.requirements)) return placed > 0;
                t.setNet(node, core.team, 0);
                placed++;
            }
        }
        return placed > 0;
    }

    // ---------- step 1: drill + path to core ----------

    /** places one drill on the nearest free ore of the scarcest mineable item, then a duct path to the core */
    static boolean buildDrillLine(CoreBuild core, AiLevel level) {
        Block drill = Blocks.plasmaBore, duct = Blocks.duct;
        if (!drill.unlockedNow() && !drill.environmentBuildable()) return false;
        Tile ore = findOreSpot(core, drill, 30);
        if (ore == null) return false;
        if (!pay(core, drill.requirements)) return false;

        // beam drills face the wall they mine: place on the free tile next to the wall, rotated towards it
        Tile site = ore;
        site.setNet(drill, core.team, faceWall(site, drill));
        if (site.build == null) return false;

        // path from the drill to the core, made of ducts (dumb AI: shortest path, ignores existing lines)
        Seq<Tile> path = Astar.pathfind(site, core.tile, tile -> 1f,
            t -> t.block() == Blocks.air || t.build == core || t == site || (t.build != null && t.build.block == duct));
        if (path == null || path.size < 2) return true;
        for (int i = 0; i < path.size - 1; i++) {
            Tile t = path.get(i), next = path.get(i + 1);
            if (t == site || t.build == core || t.build != null) continue;
            if (!pay(core, duct.requirements)) break;
            t.setNet(duct, core.team, t.relativeTo(next));
        }
        return true;
    }

    /** a free tile next to an ore wall, within radius of the core; prefers the item the core has least of */
    static Tile findOreSpot(CoreBuild core, Block drill, int radius) {
        Tile best = null;
        int bestAmount = Integer.MAX_VALUE;
        float bestDst = Float.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
                if (t == null || t.block() != Blocks.air) continue;
                Item item = adjacentWallOre(t);
                if (item == null || item.hardness > 3) continue;
                if (!Build.validPlace(drill, core.team, t.x, t.y, faceWall(t, drill), false)) continue;
                int amount = core.items.get(item);
                float dst = core.dst(t);
                if (amount < bestAmount || (amount == bestAmount && dst < bestDst)) {
                    best = t;
                    bestAmount = amount;
                    bestDst = dst;
                }
            }
        }
        return best;
    }

    static Item adjacentWallOre(Tile t) {
        for (int r = 0; r < 4; r++) {
            Tile n = t.nearby(r);
            if (n != null && n.wallDrop() != null) return n.wallDrop();
        }
        return null;
    }

    static int faceWall(Tile t, Block drill) {
        for (int r = 0; r < 4; r++) {
            Tile n = t.nearby(r);
            if (n != null && n.wallDrop() != null) return r;
        }
        return 0;
    }

    static boolean pay(Building core, ItemStack[] cost) {
        float mult = Vars.state.rules.buildCostMultiplier;
        for (ItemStack s : cost) if (core.items.get(s.item) < Math.round(s.amount * mult)) return false;
        for (ItemStack s : cost) core.items.remove(s.item, Math.round(s.amount * mult));
        return true;
    }
}
