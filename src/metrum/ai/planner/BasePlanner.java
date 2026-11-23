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

    /** fire map: damage taken by the AI team's buildings, per 30-degree sector around each core */
    static final float[] fire = new float[12];

    public static void init() {
        Events.run(Trigger.update, BasePlanner::update);
        Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            java.util.Arrays.fill(fire, 0f);
            firstBuilder = true;
            builderTimer = 0f;
            // the enemy fields Metrum drones: swap the wave units for MonoMets of a similar strength
            if (active()) {
                for (mindustry.game.SpawnGroup g : Vars.state.rules.spawns) g.type = metrumEquivalent(g.type);
            }
            // hand-made maps may lack geothermal spots: guarantee steam vents near every core on Metrum planets
            if (active()) {
                for (CoreBuild c : Vars.state.rules.waveTeam.cores()) ensureVents(c, 2);
                for (CoreBuild c : Vars.state.rules.defaultTeam.cores()) ensureVents(c, 2);
            }
        });
        Events.on(mindustry.game.EventType.BuildDamageEvent.class, e -> {
            if (e.build == null || e.source == null || e.build.team != Vars.state.rules.waveTeam) return;
            CoreBuild core = e.build.closestCore();
            if (core == null) return;
            int sector = (int) (arc.math.Angles.angle(core.x, core.y, e.source.x, e.source.y) / 30f) % 12;
            fire[sector] += e.source.damage;
        });
    }

    /** setting: run the Metrum AI in custom games / editor playtests too (not only the Ferrox campaign) */
    public static final String everywhereSetting = "metrum-ai-everywhere";

    static boolean active() {
        if (Vars.state.rules.pvp || Vars.state.rules.waveTeam.cores().size == 0) return false;
        boolean ferroxCampaign = Vars.state.isCampaign() && Vars.state.rules.sector != null && planets.contains(Vars.state.rules.sector.planet);
        boolean custom = !Vars.state.isCampaign() && arc.Core.settings.getBool(everywhereSetting, true);
        return ferroxCampaign || custom;
    }

    /** each enemy core keeps one builder drone (MonoMet T1); respawned 30 s after it dies */
    static float builderRespawn = 60f * 30f, builderTimer;

    static boolean hasBuilder(Team team) {
        return team.data().countType(metrum.content.MetrumUnits.monoMetT1) > 0;
    }

    static void keepBuilder(Team team) {
        if (hasBuilder(team)) {
            builderTimer = 0f;
            return;
        }
        builderTimer += Time.delta;
        if (builderTimer < builderRespawn && !firstBuilder) return;
        firstBuilder = false;
        builderTimer = 0f;
        CoreBuild core = team.core();
        if (core == null) return;
        mindustry.gen.Unit u = metrum.content.MetrumUnits.monoMetT1.spawn(team, core.x, core.y);
        u.controller(new mindustry.ai.types.BuilderAI(true, 600f));
        mindustry.content.Fx.spawn.at(u);
    }

    static boolean firstBuilder = true;

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
        Team aiTeam = Vars.state.rules.waveTeam;
        // the Metrum planner replaces the vanilla base builder (otherwise both would build)
        Vars.state.rules.teams.get(aiTeam).buildAi = false;
        keepBuilder(aiTeam);
        // no builder drone -> no construction
        if (!hasBuilder(aiTeam)) return;
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
        if ((!hasGenerator(core) || (hasPowerDeficit(core) && !hasIdleGenerator(core))) && buildPower(core)) return true;
        if (!powerGridDone(core) && extendPowerGrid(core)) return true;
        if (pipeWater(core)) return true;
        int factories = count(core, b -> b.block == metrum.content.MetrumPhysics.atomizer);
        if (factories < 1 + level.ordinal() / 2 && buildFactory(core)) return true;
        if (drills < wantDrills) return buildDrillLine(core, level);
        int turrets = count(core, b -> b.block instanceof mindustry.world.blocks.defense.turrets.Turret);
        if (turrets < 2 + level.ordinal() * 2 && buildTurret(core, level)) return true;
        if (buildWalls(core, level)) return true;
        return false; // next steps (water, factories, turrets, walls) come later
    }

    static int count(CoreBuild core, arc.func.Boolf<Building> pred) {
        int[] n = {0};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize, pred, b -> n[0]++);
        return n[0];
    }

    // ---------- step 2: power - geothermal (turbine condenser on steam vents) + beam-node grid ----------

    static mindustry.type.UnitType metrumEquivalent(mindustry.type.UnitType t) {
        if (t == null || t.name.startsWith("metrum-")) return t;
        float hp = t.health;
        if (hp < 300f) return metrum.content.MetrumUnits.monoMetT1;
        if (hp < 600f) return metrum.content.MetrumUnits.monoMetT1b;
        if (hp < 1000f) return metrum.content.MetrumUnits.monoMetT2;
        if (hp < 2000f) return metrum.content.MetrumUnits.monoMetT2b;
        if (hp < 5000f) return metrum.content.MetrumUnits.monoMetT3;
        if (hp < 9000f) return metrum.content.MetrumUnits.monoMetT3b;
        return metrum.content.MetrumUnits.monoMetT3c;
    }

    /** adds steam vents (3x3 rhyolite vent pattern) near a core if there are fewer than {@code wanted} within 25 tiles */
    static void ensureVents(CoreBuild core, int wanted) {
        int have = 0;
        for (int dx = -25; dx <= 25; dx++) for (int dy = -25; dy <= 25; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t != null && t.floor() instanceof mindustry.world.blocks.environment.SteamVent
                && ((mindustry.world.blocks.environment.SteamVent) t.floor()).isCenterVent(t)) have++;
        }
        arc.math.Rand r = new arc.math.Rand(core.pos());
        for (int attempt = 0; attempt < 400 && have < wanted; attempt++) {
            float a = r.random(360f), d = r.random(7f, 18f);
            int x = core.tileX() + (int) arc.math.Angles.trnsx(a, d), y = core.tileY() + (int) arc.math.Angles.trnsy(a, d);
            boolean ok = true;
            for (arc.math.geom.Point2 p : mindustry.world.blocks.environment.SteamVent.offsets) {
                Tile t = Vars.world.tile(x + p.x, y + p.y);
                if (t == null || t.block() != Blocks.air || t.floor().isLiquid || t.overlay() == Blocks.spawn) { ok = false; break; }
            }
            if (!ok) continue;
            for (arc.math.geom.Point2 p : mindustry.world.blocks.environment.SteamVent.offsets)
                Vars.world.tile(x + p.x, y + p.y).setFloorNet(Blocks.rhyoliteVent, Blocks.air);
            have++;
        }
    }

    static boolean hasGenerator(CoreBuild core) {
        return count(core, b -> b.block instanceof mindustry.world.blocks.power.PowerGenerator) > 0;
    }

    /** Connect available generation before paying for another isolated generator. */
    static boolean hasIdleGenerator(CoreBuild core) {
        return count(core, b -> b.block instanceof mindustry.world.blocks.power.PowerGenerator
            && b.power != null && b.power.graph.getLastPowerNeeded() <= 0.001f) > 0;
    }

    /** A working grid with exhausted storage needs more generation, not just more nodes. */
    static boolean hasPowerDeficit(CoreBuild core) {
        return count(core, b -> {
            if (b.power == null || b.block.consPower == null) return false;
            mindustry.world.blocks.power.PowerGraph graph = b.power.graph;
            return graph.getLastPowerProduced() > 0f
                && graph.getLastPowerNeeded() > graph.getLastPowerProduced() + 0.001f
                && graph.getLastPowerStored() <= 0.001f;
        }) > 0;
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
            b -> b.block.consPower != null && b.power != null && b.power.status < 0.999f,
            b -> unpowered[0]++);
        return unpowered[0] == 0;
    }

    static boolean extendPowerGrid(CoreBuild core) {
        Block node = Blocks.beamNode;
        // Give off-lattice buildings an adjacent connection point before filling the regular grid.
        Seq<Building> endpoints = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.power != null && (b.block.consPower != null
                || b.block instanceof mindustry.world.blocks.power.PowerGenerator)
                && !b.proximity.contains(other -> other.team == core.team && other.block == node),
            endpoints::add);
        for (Building endpoint : endpoints) {
            int min = -(endpoint.block.size - 1) / 2, max = endpoint.block.size / 2;
            for (int dx = min - 1; dx <= max + 1; dx++) for (int dy = min - 1; dy <= max + 1; dy++) {
                // Edge neighbours only; corner nodes do not touch the building.
                if (!((dx >= min && dx <= max && (dy == min - 1 || dy == max + 1))
                    || (dy >= min && dy <= max && (dx == min - 1 || dx == max + 1)))) continue;
                Tile t = Vars.world.tile(endpoint.tileX() + dx, endpoint.tileY() + dy);
                if (t == null || t.block() != Blocks.air || !Build.validPlace(node, core.team, t.x, t.y, 0, false)) continue;
                if (!pay(core, node.requirements)) return false;
                t.setNet(node, core.team, 0);
                return true;
            }
        }
        // Grow a continuous connection from a producing graph to an isolated node.
        Seq<Building> nodes = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block == node && b.power != null, nodes::add);
        Seq<Building[]> pairs = new Seq<>();
        for (Building a : nodes) for (Building b : nodes) {
            if (a.power.graph.getLastPowerProduced() <= 0f || b.power.graph.getLastPowerProduced() > 0f
                || a.power.graph == b.power.graph) continue;
            pairs.add(new Building[]{a, b});
        }
        pairs.sort((a, b) -> Float.compare(a[0].dst2(a[1]), b[0].dst2(b[1])));
        for (Building[] pair : pairs) {
            Seq<Tile> route = Astar.pathfind(pair[0].tile, pair[1].tile, t -> 1f,
                t -> (t.build != null && t.build.team == core.team && t.build.block == node)
                    || (t.block() == Blocks.air && Build.validPlace(node, core.team, t.x, t.y, 0, false)));
            // An unreachable pair must not prevent another disconnected branch from getting power.
            if (route == null || route.isEmpty()) continue;
            for (Tile t : route) {
                if (t.build != null) continue;
                if (!pay(core, node.requirements)) return false;
                t.setNet(node, core.team, 0);
                return true;
            }
        }
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

    // ---------- step 3: water - conduit from a water source (turbine condenser / pump) to a thirsty consumer ----------

    static boolean pipeWater(CoreBuild core) {
        mindustry.type.Liquid water = mindustry.content.Liquids.water;
        Building[] src = {null}, dst = {null};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.liquids != null && b.block.outputsLiquid && b.liquids.get(water) > 1f, b -> { if (src[0] == null) src[0] = b; });
        if (src[0] == null) return false;
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b != src[0] && b.block.consumesLiquid(water) && b.liquids != null && b.liquids.get(water) < 1f,
            b -> { if (dst[0] == null || b.dst(src[0]) < dst[0].dst(src[0])) dst[0] = b; });
        if (dst[0] == null) return false;

        Block conduit = Blocks.reinforcedConduit;
        Building from = src[0], to = dst[0];
        Seq<Tile> path = Astar.pathfind(from.tile, to.tile, tile -> 1f,
            t -> t.build == from || t.build == to ||
                (t.block() == Blocks.air && Build.validPlace(conduit, core.team, t.x, t.y, 0, false)));
        if (path == null || path.size < 3) return false;
        int pipeCount = 0;
        for (int i = 0; i < path.size - 1; i++) if (path.get(i).build == null) pipeCount++;
        // Check the complete line before spending anything; partial pipes obstruct later retries.
        float costMultiplier = Vars.state.rules.buildCostMultiplier;
        for (ItemStack cost : conduit.requirements) {
            if (core.items.get(cost.item) < (long) Math.round(cost.amount * costMultiplier) * pipeCount) return false;
        }
        int placed = 0;
        for (int i = 0; i < path.size - 1; i++) {
            Tile t = path.get(i), next = path.get(i + 1);
            if (t.build != null) continue; // inside source/target
            if (!pay(core, conduit.requirements)) break;
            t.setNet(conduit, core.team, t.relativeTo(next));
            placed++;
        }
        return placed > 0;
    }

    // ---------- step 4: splitter on a resource line + a factory fed from it ----------

    /** swaps one duct of a mining line for a router and puts a factory (atomizer: any mineral in) next to it */
    static boolean buildFactory(CoreBuild core) {
        Block router = Blocks.ductRouter, factory = metrum.content.MetrumPhysics.atomizer;
        Building[] pick = {null};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 30f * Vars.tilesize,
            b -> b.block == Blocks.duct && b.dst(core) > 4f * Vars.tilesize, b -> { if (pick[0] == null) pick[0] = b; });
        if (pick[0] == null) return false;
        Tile duct = pick[0].tile;
        int rot = pick[0].rotation;
        // factory goes beside the line (left or right of the flow), touching the router
        for (int side : new int[]{1, 3}) {
            int dir = (rot + side) % 4;
            Tile spot = duct.nearby(dir);
            if (spot == null) continue;
            int off = factory.sizeOffset;
            Tile origin = Vars.world.tile(spot.x + arc.math.geom.Geometry.d4x(dir) * (factory.size / 2) - off * 0,
                spot.y + arc.math.geom.Geometry.d4y(dir) * (factory.size / 2));
            if (origin == null || !Build.validPlace(factory, core.team, origin.x, origin.y, 0, false)) continue;
            if (!pay(core, factory.requirements) || !pay(core, router.requirements)) return false;
            duct.setNet(router, core.team, rot);
            origin.setNet(factory, core.team, 0);
            return true;
        }
        return false;
    }

    // ---------- step 5: turrets facing the attacker; HARD and up add an ammo magazine ----------

    static boolean buildTurret(CoreBuild core, AiLevel level) {
        Block turret = Blocks.breach;
        Block magazine = Blocks.reinforcedContainer;
        // aim the defensive ring at the player's nearest core (or towards the map centre)
        Building enemy = Vars.state.rules.defaultTeam.core();
        float ang = enemy != null ? core.angleTo(enemy) : core.angleTo(Vars.world.unitWidth() / 2f, Vars.world.unitHeight() / 2f);
        for (int attempt = 0; attempt < 16; attempt++) {
            float a = ang + arc.math.Mathf.range(55f), d = arc.math.Mathf.random(9f, 16f) * Vars.tilesize;
            Tile t = Vars.world.tileWorld(core.x + arc.math.Angles.trnsx(a, d), core.y + arc.math.Angles.trnsy(a, d));
            if (t == null || !Build.validPlace(turret, core.team, t.x, t.y, 0, false)) continue;
            if (!pay(core, turret.requirements)) return false;
            t.setNet(turret, core.team, 0);
            if (t.build == null) return true;
            if (level.turretMagazines) {
                // magazine on the side away from the enemy; magazines get restocked from the core
                Tile m = Vars.world.tileWorld(t.worldx() - arc.math.Angles.trnsx(a, 3f * Vars.tilesize), t.worldy() - arc.math.Angles.trnsy(a, 3f * Vars.tilesize));
                if (m != null && Build.validPlace(magazine, core.team, m.x, m.y, 0, false) && pay(core, magazine.requirements)) {
                    m.setNet(magazine, core.team, 0);
                }
                metrum.ai.BaseExpander.placed.add(t.build);
            }
            return true;
        }
        return false;
    }

    // ---------- step 6: wall ring; leaves a gap where the fire is heaviest (all AI levels) ----------

    static boolean buildWalls(CoreBuild core, AiLevel level) {
        Block wall = Blocks.tungstenWall;
        float radius = 20f * Vars.tilesize;
        // where to leave the gap: sector with the most recorded damage, else towards the player's core
        int best = -1;
        float max = 0f;
        for (int i = 0; i < fire.length; i++) if (fire[i] > max) { max = fire[i]; best = i; }
        Building enemy = Vars.state.rules.defaultTeam.core();
        float gapAngle = best >= 0 ? best * 30f + 15f : (enemy != null ? core.angleTo(enemy) : 0f);
        float gapHalf = 12f; // ~3 tiles wide at radius 20
        // wall arc facing the threat (+-70 degrees around the gap direction)
        int placed = 0;
        for (float a = gapAngle - 70f; a <= gapAngle + 70f && placed < 6; a += 3f) {
            if (Math.abs(arc.math.Angles.angleDist(a, gapAngle)) < gapHalf) continue; // the passage
            Tile t = Vars.world.tileWorld(core.x + arc.math.Angles.trnsx(a, radius), core.y + arc.math.Angles.trnsy(a, radius));
            if (t == null || t.block() != Blocks.air || !Build.validPlace(wall, core.team, t.x, t.y, 0, false)) continue;
            if (!pay(core, wall.requirements)) return placed > 0;
            t.setNet(wall, core.team, 0);
            placed++;
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

        // Route through the entire drill footprint to reach a free output tile at its edge.
        Seq<Tile> path = Astar.pathfind(site, core.tile, tile -> 1f,
            t -> t.build == core || t.build == site.build ||
                (t.block() == Blocks.air && Build.validPlace(duct, core.team, t.x, t.y, 0, false)));
        int ductCount = 0;
        if (path != null) for (int i = 0; i < path.size - 1; i++) {
            if (path.get(i).build == null) ductCount++;
        }
        float costMultiplier = Vars.state.rules.buildCostMultiplier;
        boolean affordable = path != null && path.size >= 2;
        for (ItemStack cost : duct.requirements) {
            if (core.items.get(cost.item) < (long) Math.round(cost.amount * costMultiplier) * ductCount) affordable = false;
        }
        if (!affordable) {
            // Roll back the drill too: do not strand paid equipment without a complete delivery route.
            site.setNet(Blocks.air, core.team, 0);
            for (ItemStack cost : drill.requirements) core.items.add(cost.item, Math.round(cost.amount * costMultiplier));
            return false;
        }
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
