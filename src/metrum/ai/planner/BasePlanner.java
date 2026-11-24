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
    static float miningBonusTimer = 60f;
    static float lastPlannerTick = -1000f;
    static int completedSteps;
    static String failureReason;
    static final java.util.Map<CoreBuild, ItemStack> miningDemand = new java.util.HashMap<>();
    static final java.util.Map<CoreBuild, java.util.Deque<ItemStack>> deferredDemand = new java.util.HashMap<>();
    static String plannerAction = "oczekiwanie na pierwszą próbę";

    /** fire map: damage taken by the AI team's buildings, per 30-degree sector around each core */
    static final java.util.Map<CoreBuild, float[]> fireByCore = new java.util.HashMap<>();

    public static void init() {
        EconomyGoals.init();
        Events.run(Trigger.update, BasePlanner::update);
        Events.on(mindustry.game.EventType.ClientLoadEvent.class, e -> {
            arc.Core.scene.table(t -> {
                t.top().right().marginTop(110f).marginRight(8f);
                t.visible(() -> Vars.state.isGame() && Vars.ui.hudfrag.shown);
                t.label(BasePlanner::controlStatus).style(mindustry.ui.Styles.outlineLabel).right();
            });
        });
        Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            fireByCore.clear();
            lastPlannerTick = -1000f;
            completedSteps = 0;
            timer = 0f;
            miningBonusTimer = 60f;
            plannerAction = "oczekiwanie na pierwszą próbę";
            builderTimers.clear();
            miningDemand.clear();
            deferredDemand.clear();

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
            fireByCore.computeIfAbsent(core, ignored -> new float[12])[sector] += e.source.damage;
        });
    }

    static String controlStatus() {
        if (Vars.net.client()) return "[lightgray]AI: status serwera niedostępny[]";
        mindustry.game.Rules.TeamRule rules = Vars.state.rules.teams.get(Vars.state.rules.waveTeam);
        boolean heartbeat = active() && Time.time - lastPlannerTick < 120f;
        String base = heartbeat ? "[green]Metrum[] — " + plannerAction + " (kroki: " + completedSteps + ")"
            : rules.buildAi ? "[orange]Mindustry[]" : "[lightgray]brak aktywnego planera[]";
        return "AI bazy: " + base + (rules.rtsAi ? "\n[orange]Oddziały: RTS Mindustry[]" : "");
    }

    /** setting: run the Metrum AI in custom games / editor playtests too (not only the Ferrox campaign) */
    public static final String everywhereSetting = "metrum-ai-everywhere";

    public static boolean active() {
        if (Vars.state.rules.pvp || Vars.state.rules.waveTeam.cores().size == 0) return false;
        boolean ferroxCampaign = Vars.state.isCampaign() && Vars.state.rules.sector != null && planets.contains(Vars.state.rules.sector.planet);
        boolean custom = !Vars.state.isCampaign() && arc.Core.settings.getBool(everywhereSetting, true);
        return ferroxCampaign || custom;
    }

    /** each enemy core keeps one builder drone (MonoMet T1); respawned 30 s after it dies */
    static float builderRespawn = 60f * 30f;
    static final double builderFlag = -7346281d; // saved by the standard unit serializer
    static final java.util.Map<CoreBuild, Float> builderTimers = new java.util.HashMap<>();

    static double builderHomeFlag(CoreBuild core) {
        return builderFlag - (core.tile.pos() & 0xffffffffL) - 1d;
    }

    static boolean isBuilderFlag(double flag) {
        return flag <= builderFlag && flag >= builderFlag - 4294967296d && flag == Math.rint(flag);
    }

    static boolean hasBuilder(CoreBuild core) {
        return mindustry.gen.Groups.unit.contains(u -> u.team == core.team
            && u.type == metrum.content.MetrumUnits.monoMetT1 && !u.dead()
            && u.controller() instanceof mindustry.ai.types.BuilderAI && u.flag == builderHomeFlag(core));
    }

    static void keepBuilder(Team team) {
        mindustry.gen.Groups.unit.each(u -> {
            if (u.team != team || u.type != metrum.content.MetrumUnits.monoMetT1 || u.dead() || u.isPlayer()) return;
            if (u.flag == builderFlag || (u.flag == 0d && u.controller() instanceof mindustry.ai.types.BuilderAI)) {
                CoreBuild home = u.closestCore();
                if (home != null) u.flag = builderHomeFlag(home);
            }
            if (isBuilderFlag(u.flag) && !(u.controller() instanceof mindustry.ai.types.BuilderAI)) {
                u.controller(new mindustry.ai.types.BuilderAI(true, 600f));
            }
        });
        builderTimers.keySet().removeIf(core -> !core.isValid() || core.team != team);
        for (CoreBuild core : team.cores()) {
            if (hasBuilder(core)) {
                builderTimers.put(core, 0f);
                continue;
            }
            float elapsed = builderTimers.getOrDefault(core, builderRespawn) + Time.delta;
            builderTimers.put(core, elapsed);
            if (elapsed < builderRespawn) continue;
            builderTimers.put(core, 0f);
            mindustry.gen.Unit u = metrum.content.MetrumUnits.monoMetT1.spawn(team, core.x, core.y);
            u.flag = builderHomeFlag(core);
            u.controller(new mindustry.ai.types.BuilderAI(true, 600f));
            mindustry.content.Fx.spawn.at(u);
        }
    }
    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
        lastPlannerTick = Time.time;
        EconomyGoals.restore();
        miningDemand.entrySet().removeIf(e -> !e.getKey().isValid()
            || e.getKey().items.get(e.getValue().item) >= e.getValue().amount);
        deferredDemand.keySet().removeIf(core -> !core.isValid() || core.team != Vars.state.rules.waveTeam);
        for (java.util.Map.Entry<CoreBuild, java.util.Deque<ItemStack>> entry : deferredDemand.entrySet()) {
            CoreBuild core = entry.getKey();
            while (!miningDemand.containsKey(core) && !entry.getValue().isEmpty()) {
                ItemStack previous = entry.getValue().pop();
                if (core.items.get(previous.item) < previous.amount) miningDemand.put(core, previous);
            }
        }
        // Economy compensation is earned by working miners, never by multiplying stored items.
        Vars.state.rules.teams.get(Vars.state.rules.waveTeam).unitMineSpeedMultiplier = 1.5f;
        miningBonusTimer += Time.delta;
        if (miningBonusTimer >= 60f) {
            miningBonusTimer = 0f;
            for (CoreBuild core : Vars.state.rules.waveTeam.cores()) supplyCoreProducers(core);
            mindustry.gen.Groups.build.each(b -> {
                if (b.team == Vars.state.rules.waveTeam
                    && (b.block instanceof mindustry.world.blocks.production.Drill
                        || b.block instanceof mindustry.world.blocks.production.BeamDrill)) {
                    b.applyBoost(1.5f, 120f);
                }
            });
        }
        // Recent attacks matter most: halve remembered damage every minute of simulation time.
        float decay = (float) Math.pow(0.5, Time.delta / (60f * 60f));
        fireByCore.keySet().removeIf(core -> !core.isValid() || core.team != Vars.state.rules.waveTeam);
        for (float[] fire : fireByCore.values()) for (int i = 0; i < fire.length; i++) {
            fire[i] *= decay;
            if (fire[i] < 0.01f) fire[i] = 0f;
        }
        Team aiTeam = Vars.state.rules.waveTeam;
        // the Metrum planner replaces the vanilla base builder (otherwise both would build)
        Vars.state.rules.teams.get(aiTeam).buildAi = false;
        Vars.state.rules.teams.get(aiTeam).infiniteResources = false;
        keepBuilder(aiTeam);
        AiLevel level = AiLevel.current();
        timer += Time.delta;
        if (timer < level.buildInterval * 60f) return;
        timer = 0f;
        failureReason = null;
        int before = completedSteps;
        Team team = Vars.state.rules.waveTeam;
        for (CoreBuild core : team.cores()) {
            if (!hasBuilder(core)) {
                stalled("oczekiwanie na konstruktora przy rdzeniu");
                continue;
            }
            for (int i = 0; i < level.stepsPerTick; i++) {
                if (!step(core, level)) break;
                completedSteps++;
            }
        }
        plannerAction = completedSteps > before ? "ostatnia próba: wykonano pracę" : (failureReason == null ? "ostatnia próba: bez postępu" : failureReason);
    }

    /** picks the next build step in the planned order */
    static boolean step(CoreBuild core, AiLevel level) {
        // Early exits in the construction plan must still allow safe space reclamation.
        return buildStep(core, level) || clearUnusedWaterPipes(core);
    }

    static boolean buildStep(CoreBuild core, AiLevel level) {
        int drills = count(core, b -> b.block instanceof mindustry.world.blocks.production.BeamDrill
            || b.block instanceof mindustry.world.blocks.production.Drill);
        int wantDrills = 2 + level.ordinal();
        // Try mining first, but let its power/water prerequisites resolve a failed attempt.
        if (drills < 2 && buildDrillLine(core, level)) return true;
        if ((!hasGenerator(core) || (hasPowerDeficit(core) && !hasIdleGenerator(core))) && buildPower(core)) return true;
        if (!powerGridDone(core) && extendPowerGrid(core)) return true;
        if (pipeWater(core)) return true;
        if (drills < 2) return false; // do not spend the bootstrap budget on factories or defences
        ItemStack demand = miningDemand.get(core);
        if (demand != null && !minedResources(core).contains(demand.item)
            && buildDrillLine(core, level, true)) return true;
        if (demand != null && buildDemandProducer(core, demand.item)) return true;
        int factories = count(core, b -> b.block == metrum.content.MetrumPhysics.atomizer);
        if (factories < 1 + level.ordinal() / 2 && buildFactory(core)) return true;
        if (drills < wantDrills) return buildDrillLine(core, level);
        int turrets = count(core, b -> b.block instanceof mindustry.world.blocks.defense.turrets.Turret);
        if (turrets < 2 + level.ordinal() * 2 && buildTurret(core, level)) return true;
        if (buildWalls(core, level)) return true;
        return false; // no construction progress; step() may reclaim unused pipe space
    }

    /** Keep startup mining and power affordable before spending shared minerals on ammunition. */
    public static java.util.Map<Item, Integer> economicReserve(CoreBuild core) {
        java.util.Map<Item, Integer> reserve = new java.util.HashMap<>();
        int drills = count(core, b -> b.block instanceof mindustry.world.blocks.production.BeamDrill
            || b.block instanceof mindustry.world.blocks.production.Drill);
        boolean needsGenerator = !hasGenerator(core);
        Block[] blocks = {Blocks.plasmaBore, Blocks.turbineCondenser, Blocks.beamNode};
        int[] quantities = {Math.max(0, 2 - drills), needsGenerator ? 1 : 0,
            needsGenerator ? 4 : (!powerGridDone(core) ? 2 : 0)};
        for (int i = 0; i < blocks.length; i++) for (ItemStack cost : blocks[i].requirements) {
            int amount = quantities[i] * Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
            reserve.put(cost.item, reserve.getOrDefault(cost.item, 0) + amount);
        }
        return reserve;
    }

    public static int availableForAmmo(CoreBuild core, Item item) {
        return Math.max(0, core.items.get(item) - economicReserve(core).getOrDefault(item, 0));
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
        if (best == null) return stalled("geoterma: brak wolnego miejsca na wyziewie");
        if (!pay(core, gen.requirements)) return stalled("geoterma: brak surowców");
        best.setNet(gen, core.team, 0);
        return true;
    }

    /** beam nodes on a lattice (every 8 tiles) between generators and consumers; nodes auto-link in straight lines */
    static boolean powerGridDone(CoreBuild core) {
        int[] unpowered = {0};
        mindustry.Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.enabled && b.shouldConsume() && b.block.consPower != null
                && b.power != null && b.power.status < 0.999f,
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
                if (!pay(core, node.requirements)) return stalled("sieć prądu: brak surowców na węzeł");
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
                if (!pay(core, node.requirements)) return stalled("sieć prądu: brak surowców na węzeł");
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
                if (!pay(core, node.requirements)) return placed > 0 || stalled("sieć prądu: brak surowców na węzeł");
                t.setNet(node, core.team, 0);
                placed++;
            }
        }
        return placed > 0 || stalled("sieć prądu: brak dostępnego miejsca na dalsze węzły");
    }

    // ---------- step 3: water - conduit from a water source (turbine condenser / pump) to a thirsty consumer ----------

    /** Free space only in empty pipe components having no connection to any liquid building. */
    static boolean clearUnusedWaterPipes(CoreBuild core) {
        Seq<Building> pipes = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block == Blocks.reinforcedConduit, pipes::add);
        Seq<Building> unused = unusedWaterPipes(pipes, core.team);
        for (Building pipe : unused) pipe.tile.setNet(Blocks.air, core.team, 0);
        return !unused.isEmpty();
    }

    static Seq<Building> unusedWaterPipes(Seq<Building> pipes, Team team) {
        java.util.Set<Building> seen = new java.util.HashSet<>();
        for (Building seed : pipes) {
            if (seen.contains(seed)) continue;
            Seq<Building> component = new Seq<>();
            java.util.ArrayDeque<Building> queue = new java.util.ArrayDeque<>();
            queue.add(seed);
            boolean needed = false;
            while (!queue.isEmpty()) {
                Building pipe = queue.removeFirst();
                if (!seen.add(pipe)) continue;
                component.add(pipe);
                if (component.size > 512) return new Seq<>(); // leave large networks for a fuller analysis
                if (pipe.liquids == null || pipe.liquids.sum((liquid, amount) -> amount) > 0.001f) needed = true;
                for (Building neighbour : pipe.proximity) {
                    if (neighbour.team == team && neighbour.block == Blocks.reinforcedConduit) {
                        if (!seen.contains(neighbour)) queue.add(neighbour);
                    } else if (neighbour.block.hasLiquids) needed = true;
                }
            }
            if (needed) continue;
            Seq<Building> selected = new Seq<>();
            for (int i = 0; i < Math.min(6, component.size); i++) selected.add(component.get(i));
            return selected;
        }
        return new Seq<>();
    }
    static boolean waterLineConnected(Building from, Building to) {
        if (from.team != to.team) return false;
        Block conduit = Blocks.reinforcedConduit;
        java.util.Set<Building> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<Building> queue = new java.util.ArrayDeque<>();
        if (from.block == conduit) queue.add(from);
        else {
            if (from.proximity.contains(to)) return true;
            for (Building adjacent : from.proximity) {
                if (adjacent.team == from.team && adjacent.block == conduit) {
                    Tile output = adjacent.tile.nearby(adjacent.rotation);
                    if (output != null && output.build != from) queue.add(adjacent);
                }
            }
        }
        while (!queue.isEmpty()) {
            Building pipe = queue.removeFirst();
            if (!visited.add(pipe)) continue;
            Tile output = pipe.tile.nearby(pipe.rotation);
            Building next = output == null ? null : output.build;
            if (next == to) return true;
            if (next == null || next.team != from.team || next.block != conduit) continue;
            Tile nextOutput = next.tile.nearby(next.rotation);
            if (nextOutput != null && nextOutput.build != pipe) queue.add(next);
        }
        return false;
    }
    /** Repair one missing pipe before considering a whole new water line. */
    static boolean repairWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations) {
        Block conduit = Blocks.reinforcedConduit;
        if (destinations.isEmpty()) return false;
        for (Building source : sources) {
            if (source.block != conduit) continue;
            Tile gap = source.tile.nearby(source.rotation);
            if (gap == null || gap.block() != Blocks.air
                || !Build.validPlace(conduit, core.team, gap.x, gap.y, 0, false)) continue;
            for (int direction = 0; direction < 4; direction++) {
                if (direction == (source.rotation + 2) % 4) continue;
                Tile beyond = gap.nearby(direction);
                Building next = beyond == null ? null : beyond.build;
                if (next == null || next.team != core.team) continue;
                if (next.block != conduit && !destinations.contains(next)) continue;
                if (next.block == conduit) {
                    Tile output = next.tile.nearby(next.rotation);
                    if (output == gap) continue; // opposing pipes would not form a working connection
                }
                if (!destinations.contains(to -> next == to || waterLineConnected(next, to))) continue;
                if (!pay(core, conduit.requirements)) return false;
                gap.setNet(conduit, core.team, direction);
                return true;
            }
        }
        return false;
    }
    /** Reuse an existing downstream line across a short straight break instead of laying a parallel route. */
    static boolean repairStraightWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations) {
        if (destinations.isEmpty()) return false;
        Block conduit = Blocks.reinforcedConduit;
        for (Building source : sources) {
            if (source.block != conduit) continue;
            Seq<Tile> gaps = new Seq<>();
            for (int distance = 1; distance <= 7; distance++) {
                Tile t = Vars.world.tile(source.tileX() + arc.math.geom.Geometry.d4x(source.rotation) * distance,
                    source.tileY() + arc.math.geom.Geometry.d4y(source.rotation) * distance);
                if (t == null) break;
                if (t.build == null) {
                    if (distance > 6 || t.block() != Blocks.air
                        || !Build.validPlace(conduit, core.team, t.x, t.y, source.rotation, false)) break;
                    gaps.add(t);
                    continue;
                }
                Building next = t.build;
                if (gaps.size < 2 || next.team != core.team) break;
                if (next.block != conduit && !destinations.contains(next)) break;
                if (next.block == conduit && next.rotation == (source.rotation + 2) % 4) break;
                if (!destinations.contains(to -> next == to || waterLineConnected(next, to))) break;
                if (!payBatch(core, conduit.requirements, gaps.size)) return false;
                for (Tile gap : gaps) {
                    gap.setNet(conduit, core.team, source.rotation);
                }
                return true;
            }
        }
        return false;
    }
    /** Search at most six missing tiles, allowing bends while preserving the old downstream route. */
    static boolean repairBentWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations) {
        if (destinations.isEmpty()) return false;
        Block conduit = Blocks.reinforcedConduit;
        for (Building source : sources) {
            if (source.block != conduit) continue;
            Tile start = source.tile.nearby(source.rotation);
            if (start == null || start.block() != Blocks.air
                || !Build.validPlace(conduit, core.team, start.x, start.y, 0, false)) continue;
            java.util.Set<Tile> seen = new java.util.HashSet<>();
            java.util.ArrayDeque<Seq<Tile>> queue = new java.util.ArrayDeque<>();
            queue.add(Seq.with(start)); seen.add(start);
            while (!queue.isEmpty()) {
                Seq<Tile> path = queue.removeFirst();
                Tile tail = path.peek();
                for (int direction = 0; direction < 4; direction++) {
                    Tile nextTile = tail.nearby(direction);
                    if (nextTile == null) continue;
                    Building next = nextTile.build;
                    if (next != null) {
                        if (next == source || next.team != core.team || path.size < 2) continue;
                        if (next.block != conduit && !destinations.contains(next)) continue;
                        if (next.block == conduit && next.tile.nearby(next.rotation) == tail) continue;
                        if (!destinations.contains(to -> next == to || waterLineConnected(next, to))) continue;
                        if (!payBatch(core, conduit.requirements, path.size)) return false;
                        for (int i = 0; i < path.size; i++) {
                            Tile t = path.get(i);
                            int rotation = i + 1 < path.size ? t.relativeTo(path.get(i + 1)) : direction;
                            t.setNet(conduit, core.team, rotation);
                        }
                        return true;
                    }
                    if (path.size >= 6 || seen.contains(nextTile) || nextTile.block() != Blocks.air
                        || !Build.validPlace(conduit, core.team, nextTile.x, nextTile.y, 0, false)) continue;
                    Seq<Tile> extended = new Seq<>(path); extended.add(nextTile);
                    seen.add(nextTile); queue.addLast(extended);
                }
            }
        }
        return false;
    }
    static boolean pipeWater(CoreBuild core) {
        mindustry.type.Liquid water = mindustry.content.Liquids.water;
        Seq<Building> sources = new Seq<>(), destinations = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.liquids != null && b.block.outputsLiquid && b.liquids.get(water) > 1f, sources::add);
        if (sources.isEmpty()) return false;
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block.consumesLiquid(water) && b.liquids != null && b.liquids.get(water) < 1f,
            destinations::add);
        // Low liquid level is not proof of a missing line: do not build parallel duplicates.
        destinations.removeAll(to -> sources.contains(from -> waterLineConnected(from, to)));
        if (repairWaterGap(core, sources, destinations)) return true;
        if (repairStraightWaterGap(core, sources, destinations)) return true;
        if (repairBentWaterGap(core, sources, destinations)) return true;
        Seq<Building[]> pairs = new Seq<>();
        for (Building from : sources) for (Building to : destinations) {
            if (from != to) pairs.add(new Building[]{from, to});
        }
        pairs.sort((a, b) -> Float.compare(a[0].dst2(a[1]), b[0].dst2(b[1])));
        Block conduit = Blocks.reinforcedConduit;
        Seq<Tile> path = null;
        for (Building[] pair : pairs) {
            Building from = pair[0], to = pair[1];
            Seq<Tile> candidate = Astar.pathfind(from.tile, to.tile, tile -> 1f,
                t -> t.build == from || t.build == to ||
                    (t.block() == Blocks.air && Build.validPlace(conduit, core.team, t.x, t.y, 0, false)));
            if (candidate != null && candidate.size >= 3) {
                path = candidate;
                break;
            }
        }
        if (path == null) return false;
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
        Seq<Building> candidates = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 30f * Vars.tilesize,
            b -> b.block == Blocks.duct && b.dst(core) > 4f * Vars.tilesize, candidates::add);
        for (Building candidate : candidates) {
            Tile duct = candidate.tile;
            int rot = candidate.rotation;
            // Duct routers only accept from behind; do not replace a bend fed from the side.
            Tile input = duct.nearby((rot + 2) % 4);
            if (input == null || input.build == null || input.build.team != core.team
                || input.build.block != Blocks.duct || input.build.rotation != rot) continue;
            // factory goes beside the line (left or right of the flow), touching the router
            for (int side : new int[]{1, 3}) {
                int dir = (rot + side) % 4;
                Tile spot = duct.nearby(dir);
                if (spot == null) continue;
                // Even-sized blocks extend one tile farther along the positive axes.
                int distance = (dir == 0 || dir == 1) ? (factory.size + 1) / 2 : factory.size / 2 + 1;
                Tile origin = Vars.world.tile(duct.x + arc.math.geom.Geometry.d4x(dir) * distance,
                    duct.y + arc.math.geom.Geometry.d4y(dir) * distance);
                if (origin == null || !Build.validPlace(factory, core.team, origin.x, origin.y, 0, false)) continue;
                if (!payExpansion(core, factory.requirements, router.requirements)) return false;
                Item carried = ((mindustry.world.blocks.distribution.Duct.DuctBuild) candidate).current;
                duct.setNet(router, core.team, rot);
                // Replacing a live duct must preserve the item currently travelling through it.
                if (carried != null && duct.build != null) duct.build.handleItem(candidate, carried);
                origin.setNet(factory, core.team, 0);
                return true;
            }
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
            if (!payExpansion(core, turret.requirements)) return false;
            t.setNet(turret, core.team, 0);
            if (t.build == null) return true;
            if (level.turretMagazines) {
                int back = Math.floorMod(Math.round((a + 180f) / 90f), 4);
                for (int turn : new int[]{0, 1, 3, 2}) {
                    int dir = (back + turn) % 4;
                    int turretMin = -(turret.size - 1) / 2, turretMax = turret.size / 2;
                    int magazineMin = -(magazine.size - 1) / 2, magazineMax = magazine.size / 2;
                    int distance = (dir == 0 || dir == 1)
                        ? turretMax - magazineMin + 1 : magazineMax - turretMin + 1;
                    Tile m = Vars.world.tile(t.x + arc.math.geom.Geometry.d4x(dir) * distance,
                        t.y + arc.math.geom.Geometry.d4y(dir) * distance);
                    if (m == null || !Build.validPlace(magazine, core.team, m.x, m.y, 0, false)) continue;
                    if (!payExpansion(core, magazine.requirements)) break;
                    m.setNet(magazine, core.team, 0);
                    break;
                }
            }
            // Every turret needs ammo; the magazine option only controls extra storage.
            metrum.ai.BaseExpander.placed.add(t.build);
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
        float[] fire = fireByCore.computeIfAbsent(core, ignored -> new float[12]);
        for (int i = 0; i < fire.length; i++) if (fire[i] > max) { max = fire[i]; best = i; }
        Building enemy = Vars.state.rules.defaultTeam.core();
        float gapAngle = best >= 0 ? best * 30f + 15f : (enemy != null ? core.angleTo(enemy) : 0f);
        float gapHalf = 12f; // ~3 tiles wide at radius 20
        // A new fire direction can move the passage onto walls built during earlier cycles.
        int cleared = 0;
        for (int dx = -21; dx <= 21; dx++) for (int dy = -21; dy <= 21; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || t.block() != wall || t.team() != core.team) continue;
            if (Math.abs(core.dst(t) - radius) > Vars.tilesize) continue;
            if (arc.math.Angles.angleDist(core.angleTo(t), gapAngle) >= gapHalf) continue;
            t.setNet(Blocks.air, core.team, 0);
            cleared++;
        }
        if (cleared > 0) return true;
        // wall arc facing the threat (+-70 degrees around the gap direction)
        int placed = 0;
        for (float a = gapAngle - 70f; a <= gapAngle + 70f && placed < 6; a += 3f) {
            if (Math.abs(arc.math.Angles.angleDist(a, gapAngle)) < gapHalf) continue; // the passage
            Tile t = Vars.world.tileWorld(core.x + arc.math.Angles.trnsx(a, radius), core.y + arc.math.Angles.trnsy(a, radius));
            if (t == null || t.block() != Blocks.air || !Build.validPlace(wall, core.team, t.x, t.y, 0, false)) continue;
            if (!payExpansion(core, wall.requirements)) return placed > 0;
            t.setNet(wall, core.team, 0);
            placed++;
        }
        return placed > 0;
    }

    // ---------- step 1: drill + path to core ----------

    /** places one drill on the nearest free ore of the scarcest mineable item, then a duct path to the core */
    static boolean buildDrillLine(CoreBuild core, AiLevel level) {
        return buildDrillLine(core, level, false);
    }

    static boolean buildDrillLine(CoreBuild core, AiLevel level, boolean demandOnly) {
        Block chosen = Blocks.plasmaBore, duct = Blocks.duct;
        Tile ore = findOreSpot(core, chosen, 30);
        ItemStack demand = miningDemand.get(core);
        if (ore == null || demand != null) {
            for (Block candidate : Vars.content.blocks()) {
                if (!(candidate instanceof mindustry.world.blocks.production.Drill) || !candidate.environmentBuildable()) continue;
                if (!canSupportDrill(core, candidate)) continue;
                Tile floorOre = findFloorOreSpot(core, (mindustry.world.blocks.production.Drill) candidate, 30);
                if (floorOre == null) continue;
                boolean meetsDemand = demand != null
                    && ((mindustry.world.blocks.production.Drill) candidate).getDrop(floorOre) == demand.item;
                if (ore == null || meetsDemand) { chosen = candidate; ore = floorOre; }
                if (meetsDemand || demand == null) break;
            }
        }
        if (ore == null) return stalled("brak dostępnej rudy dla wiertnicy");
        final Block drill = chosen;
        int rotation = drill instanceof mindustry.world.blocks.production.BeamDrill
            ? faceWall(ore, drill, core, minedResources(core)) : 0;
        if (rotation < 0) return stalled("brak poprawnego kierunku wiercenia");
        Item output = drill instanceof mindustry.world.blocks.production.BeamDrill
            ? wallOreInDirection(ore, (mindustry.world.blocks.production.BeamDrill) drill, rotation)
            : ((mindustry.world.blocks.production.Drill) drill).getDrop(ore);
        if (demandOnly && (demand == null || output != demand.item)) {
            if (demand != null) {
                Block producer = findProducer(core, demand.item);
                if (producer != null) return stalled("brak dostępnej rudy: " + demand.item.localizedName
                    + "; produkcja w: " + producer.localizedName);
            }
            return false;
        }
        if (!pay(core, drill.requirements)) return stalled("brak surowców na wiertnicę");

        // beam drills face the wall they mine: place on the free tile next to the wall, rotated towards it
        Tile site = ore;
        site.setNet(drill, core.team, rotation);
        if (site.build == null) return stalled("nie udało się postawić wiertnicy");

        // Route through the entire drill footprint to reach a free output tile at its edge.
        Seq<Tile> path = Astar.pathfind(site, core.tile, tile -> 1f,
            t -> t.build == core || t.build == site.build ||
                (t.block() == Blocks.air && Build.validPlace(duct, core.team, t.x, t.y, 0, false)));
        int ductCount = 0;
        if (path != null) for (int i = 0; i < path.size - 1; i++) {
            if (path.get(i).build == null) ductCount++;
        }
        float costMultiplier = Vars.state.rules.buildCostMultiplier;
        boolean affordable = path != null && !path.isEmpty(); // adjacent core needs no duct
        for (ItemStack cost : duct.requirements) {
            if (core.items.get(cost.item) < (long) Math.round(cost.amount * costMultiplier) * ductCount) affordable = false;
        }
        if (!affordable) {
            // Roll back the drill too: do not strand paid equipment without a complete delivery route.
            site.setNet(Blocks.air, core.team, 0);
            for (ItemStack cost : drill.requirements) core.items.add(cost.item, Math.round(cost.amount * costMultiplier));
            return stalled(path == null || path.isEmpty() ? "brak trasy do rdzenia" : "brak surowców na całą trasę");
        }
        for (int i = 0; i < path.size - 1; i++) {
            Tile t = path.get(i), next = path.get(i + 1);
            if (t == site || t.build == core || t.build != null) continue;
            if (!pay(core, duct.requirements)) break;
            t.setNet(duct, core.team, t.relativeTo(next));
        }
        return true;
    }

    static boolean stalled(String reason) {
        // Preserve the first blocker in build-order priority for this planning cycle.
        if (failureReason == null) failureReason = reason;
        return false;
    }

    /** Discover actual recipes instead of assuming every missing construction item is an ore. */
    /** Adjacent workshops draw a complete next batch from the core, never partial unpaid inputs. */
    static void supplyCoreProducers(CoreBuild core) {
        ItemStack demand = miningDemand.get(core);
        if (demand == null || core.items.get(demand.item) >= demand.amount) return;
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        workshops: for (Building factory : core.proximity) {
            if (factory.team != core.team || !factory.enabled || factory.items == null
                || !(factory.block instanceof mindustry.world.blocks.production.GenericCrafter)
                || factory.closestCore() != core) continue;
            mindustry.world.blocks.production.GenericCrafter recipe =
                (mindustry.world.blocks.production.GenericCrafter) factory.block;
            boolean wanted = recipe.outputItem != null && recipe.outputItem.item == demand.item;
            if (recipe.outputItems != null) for (ItemStack output : recipe.outputItems) wanted |= output.item == demand.item;
            if (!wanted) continue;
            java.util.Map<Item, Integer> required = new java.util.HashMap<>();
            for (mindustry.world.consumers.Consume consume : factory.block.nonOptionalConsumers) {
                if (!(consume instanceof mindustry.world.consumers.ConsumeItems)) continue;
                for (ItemStack input : ((mindustry.world.consumers.ConsumeItems) consume).items) {
                    required.put(input.item, required.getOrDefault(input.item, 0) + input.amount);
                }
            }
            for (java.util.Map.Entry<Item, Integer> input : required.entrySet()) {
                Item item = input.getKey();
                int missing = Math.max(0, input.getValue() - factory.items.get(item));
                if (input.getValue() > factory.getMaximumAccepted(item)) continue workshops;
                if (core.items.get(item) - reserve.getOrDefault(item, 0) < missing) {
                    java.util.Deque<ItemStack> pending = deferredDemand.computeIfAbsent(core, ignored -> new java.util.ArrayDeque<>());
                    // Keep the parent objective and avoid circular ingredient dependencies.
                    if (item != demand.item && pending.stream().noneMatch(parent -> parent.item == item)) {
                        pending.push(demand);
                        miningDemand.put(core, new ItemStack(item, missing + reserve.getOrDefault(item, 0)));
                        // Other workshops still hold the old demand snapshot; resume them next supply tick.
                        return;
                    }
                    continue workshops;
                }
            }
            for (java.util.Map.Entry<Item, Integer> input : required.entrySet()) {
                int missing = Math.max(0, input.getValue() - factory.items.get(input.getKey()));
                if (missing == 0) continue;
                core.items.remove(input.getKey(), missing);
                factory.items.add(input.getKey(), missing);
            }
        }
    }
    static boolean buildDemandProducer(CoreBuild core, Item item) {
        Block ready = findProducer(core, item);
        final Block producer = ready != null ? ready : findProducer(core, item, true);
        if (producer == null || count(core, b -> b.block == producer) > 0) return false;
        java.util.Map<Item, Integer> inputs = new java.util.HashMap<>();
        for (mindustry.world.consumers.Consume consume : producer.nonOptionalConsumers) {
            if (consume instanceof mindustry.world.consumers.ConsumeItems) {
                for (ItemStack input : ((mindustry.world.consumers.ConsumeItems) consume).items) {
                    inputs.put(input.item, inputs.getOrDefault(input.item, 0) + input.amount);
                }
            }
        }
        ItemStack[] batch = new ItemStack[inputs.size()];
        int index = 0;
        for (java.util.Map.Entry<Item, Integer> input : inputs.entrySet()) {
            if (input.getValue() > producer.itemCapacity) return false;
            batch[index++] = new ItemStack(input.getKey(), input.getValue());
        }
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        for (ItemStack input : batch) {
            int needed = input.amount + reserve.getOrDefault(input.item, 0);
            for (ItemStack cost : producer.requirements) if (cost.item == input.item) {
                needed += Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
            }
            if (core.items.get(input.item) >= needed) continue;
            ItemStack parent = miningDemand.get(core);
            java.util.Deque<ItemStack> pending = deferredDemand.computeIfAbsent(core, ignored -> new java.util.ArrayDeque<>());
            if (parent != null && parent.item != input.item && pending.stream().noneMatch(goal -> goal.item == input.item)) {
                pending.push(parent);
                miningDemand.put(core, new ItemStack(input.item, needed));
            }
            return false;
        }
        for (int dir = 0; dir < 4; dir++) {
            int coreMin = -(core.block.size - 1) / 2, coreMax = core.block.size / 2;
            int factoryMin = -(producer.size - 1) / 2, factoryMax = producer.size / 2;
            int distance = (dir == 0 || dir == 1) ? coreMax - factoryMin + 1 : factoryMax - coreMin + 1;
            Tile site = Vars.world.tile(core.tileX() + arc.math.geom.Geometry.d4x(dir) * distance,
                core.tileY() + arc.math.geom.Geometry.d4y(dir) * distance);
            if (site == null || !Build.validPlace(producer, core.team, site.x, site.y, 0, false)) continue;
            if (!payExpansion(core, producer.requirements, batch)) return false;
            site.setNet(producer, core.team, 0);
            if (site.build != null) for (ItemStack input : batch) site.build.items.add(input.item, input.amount);
            return true;
        }
        return false;
    }
    static Block findProducer(CoreBuild core, Item item) {
        return findProducer(core, item, false);
    }

    static Block findProducer(CoreBuild core, Item item, boolean allowMissingInputs) {
        Block best = null;
        long bestCost = Long.MAX_VALUE;
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        candidates: for (Block block : Vars.content.blocks()) {
            if (!(block instanceof mindustry.world.blocks.production.GenericCrafter) || !block.environmentBuildable()
                || block instanceof mindustry.world.blocks.production.HeatCrafter) continue;
            mindustry.world.blocks.production.GenericCrafter crafter =
                (mindustry.world.blocks.production.GenericCrafter) block;
            boolean produces = crafter.outputItem != null && crafter.outputItem.item == item;
            if (crafter.outputItems != null) for (ItemStack output : crafter.outputItems) produces |= output.item == item;
            if (!produces) continue;
            java.util.Map<Item, Integer> required = new java.util.HashMap<>();
            long totalCost = 0;
            for (ItemStack cost : block.requirements) {
                int amount = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
                required.put(cost.item, required.getOrDefault(cost.item, 0) + amount);
                totalCost += amount;
            }
            for (mindustry.world.consumers.Consume consume : block.nonOptionalConsumers) {
                if (consume instanceof mindustry.world.consumers.ConsumePower) {
                    if (!hasGenerator(core)) continue candidates;
                } else if (consume instanceof mindustry.world.consumers.ConsumeItems) {
                    for (ItemStack input : ((mindustry.world.consumers.ConsumeItems) consume).items) {
                        if (!allowMissingInputs) required.put(input.item, required.getOrDefault(input.item, 0) + input.amount);
                    }
                } else if (consume instanceof mindustry.world.consumers.ConsumeLiquid) {
                    mindustry.type.Liquid liquid = ((mindustry.world.consumers.ConsumeLiquid) consume).liquid;
                    if (liquid != mindustry.content.Liquids.water || count(core,
                        b -> b.block.outputsLiquid && b.liquids != null && b.liquids.get(liquid) > 1f) == 0) continue candidates;
                } else continue candidates;
            }
            for (java.util.Map.Entry<Item, Integer> need : required.entrySet()) {
                if (core.items.get(need.getKey()) - reserve.getOrDefault(need.getKey(), 0) < need.getValue()) continue candidates;
            }
            if (totalCost < bestCost) { best = block; bestCost = totalCost; }
        }
        return best;
    }    static boolean canSupportDrill(CoreBuild core, Block drill) {
        float multiplier = Vars.state.rules.buildCostMultiplier;
        for (ItemStack cost : drill.requirements) {
            if (core.items.get(cost.item) < Math.round(cost.amount * multiplier)) return false;
        }
        for (mindustry.world.consumers.Consume consume : drill.nonOptionalConsumers) {
            if (consume instanceof mindustry.world.consumers.ConsumePower) continue;
            if (!(consume instanceof mindustry.world.consumers.ConsumeLiquid)) return false;
            mindustry.type.Liquid liquid = ((mindustry.world.consumers.ConsumeLiquid) consume).liquid;
            // The current pipe planner supplies water only, so gas-dependent drills must wait.
            if (liquid != mindustry.content.Liquids.water) return false;
            if (count(core, b -> b.block.outputsLiquid && b.liquids != null && b.liquids.get(liquid) > 1f) == 0) return false;
        }
        return true;
    }
    static Tile findFloorOreSpot(CoreBuild core, mindustry.world.blocks.production.Drill drill, int radius) {
        Tile best = null;
        ItemStack demand = miningDemand.get(core);
        boolean bestMeetsDemand = false;
        int least = Integer.MAX_VALUE;
        float nearest = Float.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || t.block() != Blocks.air || !drill.canMine(t)
                || !Build.validPlace(drill, core.team, t.x, t.y, 0, false)) continue;
            Item item = drill.getDrop(t);
            if (item == null) continue;
            int amount = core.items.get(item);
            float distance = core.dst2(t);
            boolean meetsDemand = demand != null && demand.item == item;
            if (best == null || (meetsDemand && !bestMeetsDemand)
                || (meetsDemand == bestMeetsDemand && (amount < least || (amount == least && distance < nearest)))) {
                best = t; least = amount; nearest = distance;
                bestMeetsDemand = meetsDemand;
            }
        }
        return best;
    }
    static java.util.Set<Item> minedResources(CoreBuild core) {
        java.util.Set<Item> mined = new java.util.HashSet<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b instanceof mindustry.world.blocks.production.BeamDrill.BeamDrillBuild,
            b -> {
                mindustry.world.blocks.production.BeamDrill.BeamDrillBuild beam =
                    (mindustry.world.blocks.production.BeamDrill.BeamDrillBuild) b;
                // Disabled drills and drills with no mineable wall do not cover the resource.
                if (beam.enabled && beam.facingAmount > 0 && beam.lastItem != null) mined.add(beam.lastItem);
            });
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b instanceof mindustry.world.blocks.production.Drill.DrillBuild,
            b -> {
                mindustry.world.blocks.production.Drill.DrillBuild floor =
                    (mindustry.world.blocks.production.Drill.DrillBuild) b;
                if (floor.enabled && floor.dominantItems > 0 && floor.dominantItem != null) mined.add(floor.dominantItem);
            });
        return mined;
    }

    /** a free tile next to an ore wall, within radius of the core; prefers the item the core has least of */
    static Tile findOreSpot(CoreBuild core, Block drill, int radius) {
        java.util.Set<Item> mined = minedResources(core);
        Tile best = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestAmount = Integer.MAX_VALUE;
        float bestDst = Float.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
                if (t == null || t.block() != Blocks.air) continue;
                int rotation = faceWall(t, drill, core, mined);
                if (rotation < 0) continue;
                Item item = wallOreInDirection(t, (mindustry.world.blocks.production.BeamDrill) drill, rotation);
                if (item == null) continue;
                if (!Build.validPlace(drill, core.team, t.x, t.y, rotation, false)) continue;
                int amount = core.items.get(item);
                float dst = core.dst(t);
                int priority = orePriority(core, item, mined);
                if (best == null || (priority < bestPriority)
                    || (priority == bestPriority && (amount < bestAmount || (amount == bestAmount && dst < bestDst)))) {
                    best = t;
                    bestPriority = priority;
                    bestAmount = amount;
                    bestDst = dst;
                }
            }
        }
        return best;
    }

    static Item wallOreInDirection(Tile site, mindustry.world.blocks.production.BeamDrill drill, int rotation) {
        Item found = null;
        arc.math.geom.Point2 edge = new arc.math.geom.Point2();
        for (int lane = 0; lane < drill.size; lane++) {
            drill.nearbySide(site.x, site.y, rotation, lane, edge);
            for (int distance = 0; distance < drill.range; distance++) {
                Tile hit = Vars.world.tile(edge.x + arc.math.geom.Geometry.d4x(rotation) * distance,
                    edge.y + arc.math.geom.Geometry.d4y(rotation) * distance);
                if (hit == null || !hit.solid()) continue;
                Item item = hit.wallDrop();
                if (item != null && item.hardness <= drill.tier && item != drill.blockedItem
                    && (drill.blockedItems == null || !drill.blockedItems.contains(item))) {
                    if (found != null && found != item) return null;
                    found = item;
                }
                break; // a beam never passes through the first solid obstacle
            }
        }
        return found;
    }

    static int orePriority(CoreBuild core, Item item, java.util.Set<Item> mined) {
        ItemStack demand = miningDemand.get(core);
        return demand != null && demand.item == item ? 0 : (mined.contains(item) ? 2 : 1);
    }
    static int faceWall(Tile t, Block drill, CoreBuild core, java.util.Set<Item> mined) {
        mindustry.world.blocks.production.BeamDrill beam = (mindustry.world.blocks.production.BeamDrill) drill;
        int best = -1, least = Integer.MAX_VALUE;
        int bestPriority = Integer.MAX_VALUE;
        for (int r = 0; r < 4; r++) {
            if (!Build.validPlace(drill, core.team, t.x, t.y, r, false)) continue;
            Item item = wallOreInDirection(t, beam, r);
            if (item == null) continue;
            int priority = orePriority(core, item, mined);
            if (best < 0 || (priority < bestPriority)
                || (priority == bestPriority && core.items.get(item) < least)) {
                best = r;
                bestPriority = priority;
                least = core.items.get(item);
            }
        }
        return best;
    }
    /** Pay optional expansion as one transaction while preserving the mining/power reserve. */
    static boolean payExpansion(CoreBuild core, ItemStack[]... costs) {
        java.util.Map<Item, Integer> total = new java.util.HashMap<>();
        float mult = Vars.state.rules.buildCostMultiplier;
        for (ItemStack[] group : costs) for (ItemStack cost : group) {
            total.put(cost.item, total.getOrDefault(cost.item, 0) + Math.round(cost.amount * mult));
        }
        for (java.util.Map.Entry<Item, Integer> cost : total.entrySet()) {
            int available = availableForAmmo(core, cost.getKey());
            if (available < cost.getValue()) {
                miningDemand.putIfAbsent(core, new ItemStack(cost.getKey(), cost.getValue()
                    + economicReserve(core).getOrDefault(cost.getKey(), 0)));
                return stalled("brak: " + cost.getKey().localizedName
                + " (wolne " + available + "/" + cost.getValue() + ", po odjęciu rezerwy)");
            }
        }
        for (java.util.Map.Entry<Item, Integer> cost : total.entrySet()) core.items.remove(cost.getKey(), cost.getValue());
        return true;
    }

    static boolean pay(Building core, ItemStack[] cost) {
        return payBatch(core, cost, 1);
    }

    static boolean payBatch(Building core, ItemStack[] cost, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Negative construction quantity");
        java.util.Map<Item, Long> totals = new java.util.HashMap<>();
        for (ItemStack entry : cost) {
            long amount = (long) Math.round(entry.amount * Vars.state.rules.buildCostMultiplier) * quantity;
            totals.put(entry.item, totals.getOrDefault(entry.item, 0L) + amount);
        }
        for (java.util.Map.Entry<Item, Long> entry : totals.entrySet()) {
            int stored = core.items.get(entry.getKey());
            long needed = entry.getValue();
            if (stored < needed) {
                if (core instanceof CoreBuild && needed <= Integer.MAX_VALUE)
                    miningDemand.putIfAbsent((CoreBuild) core, new ItemStack(entry.getKey(), (int) needed));
                return stalled("brak: " + entry.getKey().localizedName + " (" + stored + "/" + needed + ")");
            }
        }
        for (java.util.Map.Entry<Item, Long> entry : totals.entrySet()) core.items.remove(entry.getKey(), entry.getValue().intValue());
        return true;
    }
}