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
import mindustry.entities.units.BuildPlan;
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
    static final java.util.Map<CoreBuild, Seq<BuildPlan>> abandonedPlans = new java.util.HashMap<>();
    static final java.util.Map<CoreBuild, Integer> bentRepairCursor = new java.util.HashMap<>();
    static float lastPlannerTick = -1000f;
    static int completedSteps;
    static String failureReason;
    static final java.util.Map<CoreBuild, ItemStack> miningDemand = new java.util.HashMap<>();
    private static final java.util.Set<CoreBuild> blockedSiliconSites = new java.util.HashSet<>();
    private static final java.util.Map<CoreBuild, Float> lastSandOnInput = new java.util.HashMap<>();
    private static final java.util.Map<CoreBuild, Float> lastSandForFurnace = new java.util.HashMap<>();
    private static final java.util.Map<Building, Integer> observedFurnaceSand = new java.util.HashMap<>();
    static final java.util.Map<CoreBuild, java.util.Deque<ItemStack>> deferredDemand = new java.util.HashMap<>();
    static String plannerAction = "oczekiwanie na pierwszą próbę";

    /** fire map: damage taken by the AI team's buildings, per 30-degree sector around each core */
    static final java.util.Map<CoreBuild, float[]> fireByCore = new java.util.HashMap<>();

    public static void init() {
        EconomyGoals.init();
        AbandonedBuilds.init();
        Events.run(Trigger.update, BasePlanner::update);
        Events.on(mindustry.game.EventType.UnitDestroyEvent.class, e -> {
            if (Vars.net.client() || !isBuilderFlag(e.unit.flag) || e.unit.plans.isEmpty()) return;
            CoreBuild home = e.unit.team.cores().find(core -> builderHomeFlag(core) == e.unit.flag);
            if (home != null) abandonedPlans.put(home, copyBuildQueue(e.unit));
        });
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
            abandonedPlans.clear();
            bentRepairCursor.clear();
            miningDemand.clear();
            blockedSiliconSites.clear();
            lastSandOnInput.clear();
            lastSandForFurnace.clear();
            observedFurnaceSand.clear();
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
        return "AI bazy: " + base + (heartbeat ? resourceStatus() : "")
            + (rules.rtsAi ? "\n[orange]Oddziały: RTS Mindustry[]" : "");
    }

    private static String resourceStatus() {
        CoreBuild core = Vars.state.rules.waveTeam.cores().find(c -> miningDemand.containsKey(c));
        if (core == null) return "";
        ItemStack demand = miningDemand.get(core);
        java.util.Deque<ItemStack> pending = deferredDemand.get(core);
        ItemStack original = pending == null ? null : pending.peekLast();
        String status = arc.Core.bundle.format("metrum.ai.resource-status", demand.item.localizedName,
            core.items.get(demand.item), demand.amount);
        if (original != null) status += "\n" + arc.Core.bundle.format("metrum.ai.deferred-status", original.item.localizedName);
        if (demand.item == mindustry.content.Items.sand
            && Time.time - lastSandOnInput.getOrDefault(core, Float.NEGATIVE_INFINITY) < 120f) {
            boolean burnsSand = ((mindustry.world.blocks.storage.CoreBlock) core.block).incinerateNonBuildable
                && !demand.item.buildable;
            status += "\n" + arc.Core.bundle.get(burnsSand ? "metrum.ai.sand-core-incinerates" : "metrum.ai.sand-in-transit");
        }
        if (demand.item == mindustry.content.Items.silicon && blockedSiliconSites.contains(core)) {
            status += "\n" + arc.Core.bundle.get("metrum.ai.silicon-site-blocked");
        }
        if (needsArcFurnaceSand(core, demand)) {
            Building furnace = findSandArcFurnace(core);
            if (furnace != null) {
                Item sand = mindustry.content.Items.sand, graphite = mindustry.content.Items.graphite;
                int sandStock = furnace.items.get(sand), graphiteStock = furnace.items.get(graphite);
                int sandNeeded = requiredPerBatch(furnace, sand), graphiteNeeded = requiredPerBatch(furnace, graphite);
                status += "\n" + arc.Core.bundle.format("metrum.ai.arc-furnace-inputs",
                    sandStock, sandNeeded, graphiteStock, graphiteNeeded);
                if (sandStock >= sandNeeded && graphiteStock >= graphiteNeeded
                    && furnace.block.consPower != null && furnace.power != null
                    && furnace.shouldConsume() && furnace.power.status < 0.999f) {
                    status += "\n" + arc.Core.bundle.format("metrum.ai.arc-furnace-low-power",
                        (int) (furnace.power.status * 100f));
                }
            }
        }
        mindustry.gen.Unit drone = assignedMiningDrone(core);
        String droneStatus = drone == null ? arc.Core.bundle.get("metrum.ai.mining-drone-unassigned")
            : arc.Core.bundle.format("metrum.ai.mining-drone-assigned", drone.type.localizedName);
        return "\n" + status + "\n" + droneStatus;
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

    static mindustry.gen.Unit assignedBuilder(CoreBuild core) {
        return mindustry.gen.Groups.unit.find(u -> u.team == core.team && !u.dead()
            && u.type == metrum.content.MetrumUnits.monoMetT1 && u.flag == builderHomeFlag(core)
            && u.controller() instanceof MetrumBuilderAI);
    }
    static boolean hasBuilder(CoreBuild core) {
        return mindustry.gen.Groups.unit.contains(u -> u.team == core.team
            && u.type == metrum.content.MetrumUnits.monoMetT1 && !u.dead()
            && u.controller() instanceof mindustry.ai.types.BuilderAI && u.flag == builderHomeFlag(core));
    }

    static Seq<BuildPlan> copyBuildQueue(mindustry.gen.Unit builder) {
        Seq<BuildPlan> copy = new Seq<>();
        for (BuildPlan plan : builder.plans) copy.add(plan.copy());
        return copy;
    }

    static boolean restoreBuildQueue(mindustry.gen.Unit builder, Seq<BuildPlan> plans) {
        if (builder == null || !builder.plans.isEmpty() || plans == null || plans.isEmpty()) return false;
        for (BuildPlan plan : plans) builder.addBuild(plan.copy());
        return true;
    }

    static void restoreAbandonedPlans(CoreBuild core, mindustry.gen.Unit builder) {
        if (restoreBuildQueue(builder, abandonedPlans.get(core))) abandonedPlans.remove(core);
    }
    static void keepBuilder(Team team) {
        mindustry.gen.Groups.unit.each(u -> {
            if (u.team != team || u.type != metrum.content.MetrumUnits.monoMetT1 || u.dead() || u.isPlayer()) return;
            if (u.flag == builderFlag || (u.flag == 0d && u.controller() instanceof mindustry.ai.types.BuilderAI)) {
                CoreBuild home = u.closestCore();
                if (home != null) u.flag = builderHomeFlag(home);
            }
            if (isBuilderFlag(u.flag) && !(u.controller() instanceof MetrumBuilderAI)) {
                u.controller(new MetrumBuilderAI());
            }
        });
        builderTimers.keySet().removeIf(core -> !core.isValid() || core.team != team);
        bentRepairCursor.keySet().removeIf(core -> !core.isValid() || core.team != team);
        abandonedPlans.keySet().removeIf(core -> !core.isValid() || core.team != team);
        for (CoreBuild core : team.cores()) {
            if (hasBuilder(core)) {
                restoreAbandonedPlans(core, assignedBuilder(core));
                builderTimers.put(core, 0f);
                continue;
            }
            float elapsed = builderTimers.getOrDefault(core, builderRespawn) + Time.delta;
            builderTimers.put(core, elapsed);
            if (elapsed < builderRespawn) continue;
            builderTimers.put(core, 0f);
            mindustry.gen.Unit u = metrum.content.MetrumUnits.monoMetT1.spawn(team, core.x, core.y);
            u.flag = builderHomeFlag(core);
            u.controller(new MetrumBuilderAI());
            restoreAbandonedPlans(core, u);
            mindustry.content.Fx.spawn.at(u);
        }
    }
    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !active()) return;
        lastPlannerTick = Time.time;
        EconomyGoals.restore();
        AbandonedBuilds.restore();
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
        observeSandInputs();
        // Economy compensation is earned by working miners, never by multiplying stored items.
        Vars.state.rules.teams.get(Vars.state.rules.waveTeam).unitMineSpeedMultiplier = 1.5f;
        miningBonusTimer += Time.delta;
        if (miningBonusTimer >= 60f) {
            miningBonusTimer = 0f;
            for (CoreBuild core : Vars.state.rules.waveTeam.cores()) {
                supplyCoreProducers(core);
                supplyMiningDock(core);
            }
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
        blockedSiliconSites.clear();
        int before = completedSteps;
        Team team = Vars.state.rules.waveTeam;
        for (CoreBuild core : team.cores()) {
            if (!hasBuilder(core)) {
                stalled("oczekiwanie na konstruktora przy rdzeniu");
                continue;
            }
            assignMiningDrone(core);
            for (int i = 0; i < level.stepsPerTick; i++) {
                if (!step(core, level)) break;
                completedSteps++;
            }
        }
        plannerAction = completedSteps > before ? "ostatnia próba: wykonano pracę" : (failureReason == null ? "ostatnia próba: bez postępu" : failureReason);
    }

    private static void observeSandInputs() {
        lastSandOnInput.keySet().removeIf(core -> !core.isValid() || core.team != Vars.state.rules.waveTeam);
        lastSandForFurnace.keySet().removeIf(core -> !core.isValid() || core.team != Vars.state.rules.waveTeam);
        observedFurnaceSand.keySet().removeIf(furnace -> !furnace.isValid() || furnace.team != Vars.state.rules.waveTeam);
        Item sand = mindustry.content.Items.sand;
        for (CoreBuild core : Vars.state.rules.waveTeam.cores()) {
            ItemStack demand = miningDemand.get(core);
            if (demand == null || (demand.item != sand && !needsArcFurnaceSand(core, demand))) continue;
            mindustry.world.blocks.distribution.Duct.DuctBuild input = sandInput(core);
            if (input == null) for (Building storage : core.proximity) {
                if (!isLinkedStorage(core, storage)) continue;
                input = sandInput(storage);
                if (input != null) break;
            }
            if (input != null) lastSandOnInput.put(core, Time.time);

            if (coreBurnsSand(core) && needsArcFurnaceSand(core, demand)) {
                if (feedArcFurnaceSand(core, input) && demand.item == sand) miningDemand.remove(core);
            }
        }
    }

    private static boolean coreBurnsSand(CoreBuild core) {
        return ((mindustry.world.blocks.storage.CoreBlock) core.block).incinerateNonBuildable
            && !mindustry.content.Items.sand.buildable;
    }

    private static boolean isSandForSilicon(CoreBuild core, ItemStack demand) {
        return demand.item == mindustry.content.Items.sand && needsArcFurnaceSand(core, demand);
    }

    private static boolean needsArcFurnaceSand(CoreBuild core, ItemStack demand) {
        if (demand.item == mindustry.content.Items.silicon) return true;
        if (demand.item != mindustry.content.Items.sand && demand.item != mindustry.content.Items.graphite) return false;
        java.util.Deque<ItemStack> pending = deferredDemand.get(core);
        ItemStack parent = pending == null ? null : pending.peekFirst();
        return parent != null && parent.item == mindustry.content.Items.silicon;
    }

    private static mindustry.world.blocks.distribution.Duct.DuctBuild sandInput(Building destination) {
        return (mindustry.world.blocks.distribution.Duct.DuctBuild) destination.proximity.find(b -> b.team == destination.team
            && b instanceof mindustry.world.blocks.distribution.Duct.DuctBuild
            && ((mindustry.world.blocks.distribution.Duct.DuctBuild) b).next == destination
            && ((mindustry.world.blocks.distribution.Duct.DuctBuild) b).current == mindustry.content.Items.sand);
    }

    private static boolean feedArcFurnaceSand(CoreBuild core, mindustry.world.blocks.distribution.Duct.DuctBuild input) {
        Building furnace = findSandArcFurnace(core);
        if (furnace == null) return false;
        Item sand = mindustry.content.Items.sand;
        int batch = requiredPerBatch(furnace, sand);
        int stocked = furnace.items.get(sand);
        Integer previous = observedFurnaceSand.put(furnace, stocked);
        if (previous != null && stocked > previous) lastSandForFurnace.put(core, Time.time);
        if (stocked < batch && input != null) {
            int transferred = input.removeStack(sand, 1);
            if (transferred > 0) {
                furnace.items.add(sand, transferred);
                observedFurnaceSand.put(furnace, stocked + transferred);
                lastSandForFurnace.put(core, Time.time);
            }
        }
        return furnace.items.get(sand) >= batch;
    }

    private static Building findSandArcFurnace(CoreBuild core) {
        for (Building furnace : coreWorkshops(core, mindustry.content.Items.silicon)) {
            if (furnace.team != core.team || !furnace.enabled || furnace.items == null || furnace.closestCore() != core
                || !isSiliconArcFurnace(furnace.block)) continue;
            int batch = requiredPerBatch(furnace, mindustry.content.Items.sand);
            if (batch > 0 && batch <= furnace.getMaximumAccepted(mindustry.content.Items.sand)) return furnace;
        }
        return null;
    }

    private static int requiredPerBatch(Building furnace, Item item) {
        int batch = 0;
        for (mindustry.world.consumers.Consume consumer : furnace.block.nonOptionalConsumers) {
            if (!(consumer instanceof mindustry.world.consumers.ConsumeItems)) continue;
            for (ItemStack ingredient : ((mindustry.world.consumers.ConsumeItems) consumer).items) {
                if (ingredient.item == item) batch += ingredient.amount;
            }
        }
        return batch;
    }

    static void assignMiningDrone(CoreBuild core) {
        ItemStack demand = miningDemand.get(core);
        if (demand == null || !metrum.ai.SmartMining.canStoreMined(core, demand.item)) return;
        mindustry.gen.Unit current = assignedMiningDrone(core);
        if (current != null && (current.stack.amount > 0 || canGather(current, demand.item))) return;
        mindustry.gen.Unit drone = mindustry.gen.Groups.unit.find(u -> u.team == core.team && !u.dead() && !u.isPlayer()
            && u.flag == 0d && u.plans.isEmpty() && u.closestCore() == core
            && metrum.ai.SmartMining.command != null && u.type.commands.contains(metrum.ai.SmartMining.command)
            && u.controller() instanceof mindustry.entities.units.AIController
            && !(u.controller() instanceof ResourceDroneAI) && canGather(u, demand.item));
        if (drone == null) return;
        if (current != null) {
            current.mineTile = null;
            current.controller(((ResourceDroneAI) current.controller()).previousController);
        }
        drone.controller(new ResourceDroneAI(core, drone.controller()));
    }

    private static mindustry.gen.Unit assignedMiningDrone(CoreBuild core) {
        return mindustry.gen.Groups.unit.find(u -> !u.dead() && u.team == core.team
            && u.controller() instanceof ResourceDroneAI && ((ResourceDroneAI) u.controller()).home == core);
    }

    private static boolean canGather(mindustry.gen.Unit drone, Item item) {
        return drone.type.mineItems.contains(item) && drone.canMine(item)
            && metrum.ai.SmartMining.findMineableOre(drone, item) != null;
    }

    private static class ResourceDroneAI extends metrum.ai.SmartMining.SmartMinerAI {
        private static final float tungstenWait = 20f * 60f;
        final CoreBuild home;
        final mindustry.entities.units.UnitController previousController;
        private ItemStack tungstenGoal;
        private int lastTungstenCargo;
        private float lastTungstenProgress;

        ResourceDroneAI(CoreBuild home, mindustry.entities.units.UnitController previousController) {
            this.home = home;
            this.previousController = previousController;
        }

        @Override
        public void updateMovement() {
            if (!home.isValid() || home.team != unit.team) {
                unit.mineTile = null;
                if (unit.stack.amount == 0) {
                    unit.controller(previousController);
                } else {
                    CoreBuild fallback = unit.closestCore();
                    if (fallback != null) deliverCargo(fallback);
                }
                return;
            }
            ItemStack demand = miningDemand.get(home);
            ItemStack goal = demand != null && demand.item == mindustry.content.Items.tungsten ? demand : null;
            int tungstenCargo = unit.stack.item == mindustry.content.Items.tungsten ? unit.stack.amount : 0;
            if (goal != tungstenGoal || tungstenCargo > lastTungstenCargo) lastTungstenProgress = Time.time;
            tungstenGoal = goal;
            lastTungstenCargo = tungstenCargo;
            if (unit.stack.amount > 0) {
                Item cargo = unit.stack.item;
                int accepted = home.acceptStack(cargo, unit.stack.amount, unit);
                CoreBuild nearest = unit.closestCore();
                boolean vanillaWouldDiscardCargo = targetItem != null && nearest != null
                    && nearest.acceptStack(targetItem, 1, unit) == 0;
                boolean hasRequestedAmount = demand != null && cargo == demand.item
                    && unit.stack.amount >= demand.amount - home.items.get(cargo);
                if (!mining || hasRequestedAmount || unit.stack.amount >= unit.type.itemCapacity
                    || accepted < unit.stack.amount || vanillaWouldDiscardCargo
                    || (demand != null && cargo != demand.item)
                    || (!unit.validMine(unit.mineTile) && !canGather(unit, cargo))) {
                    deliverCargo(home);
                    return;
                }
            }
            super.updateMovement();
        }

        private void deliverCargo(CoreBuild core) {
            mining = false;
            unit.mineTile = null;
            Item cargo = unit.stack.item;
            int accepted = core.acceptStack(cargo, unit.stack.amount, unit);
            if (accepted > 0 && unit.within(core, unit.type.range)) {
                mindustry.gen.Call.transferItemTo(unit, cargo, accepted, unit.x, unit.y, core);
                if (cargo == mindustry.content.Items.tungsten) lastTungstenProgress = Time.time;
            }
            moveTo(core, unit.type.range * 0.8f);
        }

        private boolean isMakingTungstenProgress() {
            return tungstenGoal == null || Time.time - lastTungstenProgress < tungstenWait;
        }

        @Override
        protected Item desired() {
            ItemStack demand = miningDemand.get(home);
            if (demand != null && metrum.ai.SmartMining.canStoreMined(home, demand.item)
                && canGather(unit, demand.item)) return demand.item;
            return super.desired();
        }
    }

    /** picks the next build step in the planned order */
    static boolean step(CoreBuild core, AiLevel level) {
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder != null && !builder.plans.isEmpty()) return stalled("konstruktor realizuje plan budowy");
        if (builder != null && resumeConstruction(core, builder)) return true;
        // Early exits in the construction plan must still allow safe space reclamation.
        return buildStep(core, level) || clearUnusedWaterPipes(core);
    }

    static boolean canResumeConstruction(mindustry.world.blocks.ConstructBlock.ConstructBuild site, mindustry.gen.Unit builder) {
        return site.team == builder.team && site.current != null && site.wasConstructing && !site.activeDeconstruct
            && (site.lastBuilder == null || site.lastBuilder == builder || site.lastBuilder.dead());
    }

    static boolean resumeConstruction(CoreBuild core, mindustry.gen.Unit builder) {
        Building[] nearest = {null};
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b instanceof mindustry.world.blocks.ConstructBlock.ConstructBuild && b.closestCore() == core
                && canResumeConstruction((mindustry.world.blocks.ConstructBlock.ConstructBuild) b, builder),
            b -> { if (nearest[0] == null || builder.dst2(b) < builder.dst2(nearest[0])) nearest[0] = b; });
        if (nearest[0] == null) return false;
        mindustry.world.blocks.ConstructBlock.ConstructBuild site =
            (mindustry.world.blocks.ConstructBlock.ConstructBuild) nearest[0];
        builder.addBuild(new BuildPlan(site.tileX(), site.tileY(), site.rotation, site.current, site.lastConfig));
        return true;
    }
    static boolean buildStep(CoreBuild core, AiLevel level) {
        ItemStack currentDemand = miningDemand.get(core);
        if (currentDemand != null && coreBurnsSand(core) && needsArcFurnaceSand(core, currentDemand)
            && redirectSandLine(core)) return true;
        boolean waitingForSand = currentDemand != null && isSandForSilicon(core, currentDemand)
            && Time.time - lastSandForFurnace.getOrDefault(core, Float.NEGATIVE_INFINITY) < 6f * 60f;
        int drills = count(core, b -> b.block instanceof mindustry.world.blocks.production.BeamDrill
            || b.block instanceof mindustry.world.blocks.production.Drill);
        int wantDrills = 2 + level.ordinal();
        // Try mining first, but let its power/water prerequisites resolve a failed attempt.
        if (drills < 2 && !waitingForSand && buildDrillLine(core, level)) return true;
        boolean hasPowerSource = hasGenerator(core);
        if (hasPowerSource && prioritizeArcFurnacePower(core)) return true;
        if ((!hasPowerSource || (hasPowerDeficit(core) && !hasIdleGenerator(core))) && buildPower(core)) return true;
        if (!powerGridDone(core) && extendPowerGrid(core)) return true;
        if (hasWaterDeficit(core) && !hasIdleGenerator(core) && buildPower(core)) return true;
        if (pipeWater(core)) return true;
        ItemStack demand = miningDemand.get(core);
        if (demand != null && hasRecoveryGoal(core, demand.item) && needsRecoveryWater(core)
            && hasWaterDeficit(core) && buildPower(core, true)) return true;
        if (pipeSteam(core)) return true;
        demand = miningDemand.get(core);
        if (demand != null && coreBurnsSand(core) && isSandForSilicon(core, demand)
            && buildDemandProducer(core, mindustry.content.Items.silicon)) return true;
        if (demand != null && !waitingForSand && buildMiningDock(core, demand.item)) return true;
        boolean waitingForTungsten = demand != null && demand.item == mindustry.content.Items.tungsten
            && hasLocalTungstenDrone(core, demand.item);
        if (demand != null && !waitingForTungsten && !waitingForSand) {
            if (buildSteamForRecovery(core, demand.item)) return true;
            if (demand.item == metrum.content.MetrumItems.pieceRock
                && buildWallExtractor(core, metrum.content.MetrumBlocks.rockScraper)) return true;
            if (drills >= 2 || hasRecoveryGoal(core, demand.item)) {
                if (!minedResources(core).contains(demand.item) && buildDrillLine(core, level, true)) return true;
                if (demand.item == mindustry.content.Items.sand && hasRecoveryGoal(core, demand.item)
                    && buildWallExtractor(core, Blocks.cliffCrusher)) return true;
                if (buildDemandProducer(core, demand.item)) return true;
                boolean explore = demand.item == mindustry.content.Items.tungsten
                    || ((demand.item == mindustry.content.Items.coal || demand.item == mindustry.content.Items.sand)
                        && hasRecoveryGoal(core, demand.item));
                if (explore && miningDemand.get(core) == demand
                    && !minedResources(core).contains(demand.item) && buildDrillLine(core, level, true, 60)) return true;
                if (miningDemand.get(core) == demand && demand.item == mindustry.content.Items.coal
                    && hasRecoveryGoal(core, demand.item) && stoneCrusherNeedsRock(core)
                    && buildWallExtractor(core, metrum.content.MetrumBlocks.rockScraper, false)) return true;
            }
        }
        if (drills < 2) return false; // Keep routine expansion behind mining and recovery prerequisites.
        int factories = count(core, b -> b.block == metrum.content.MetrumPhysics.atomizer);
        if (factories < 1 + level.ordinal() / 2 && buildFactory(core)) return true;
        if (drills < wantDrills && !waitingForSand) {
            if (buildDrillLine(core, level)) return true;
            if (!waitingForTungsten) return false;
        }
        int turrets = count(core, b -> b.block instanceof mindustry.world.blocks.defense.turrets.Turret);
        if (turrets < 2 + level.ordinal() * 2 && buildTurret(core, level)) return true;
        if (buildWalls(core, level)) return true;
        return false; // no construction progress; step() may reclaim unused pipe space
    }

    private static boolean stoneCrusherNeedsRock(CoreBuild core) {
        Item rock = metrum.content.MetrumItems.pieceRock;
        return count(core, b -> b.enabled && b.block == metrum.content.MetrumProduction.stoneCrusher
            && b.items != null && b.closestCore() == core && b.items.get(rock) + core.items.get(rock) < requiredPerBatch(b, rock)) > 0;
    }

    private static boolean redirectSandLine(CoreBuild core) {
        Building furnace = findSandArcFurnace(core);
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (furnace == null || builder == null || !builder.plans.isEmpty()) return false;
        Seq<Building> inputs = new Seq<>();
        inputs.addAll(core.proximity);
        for (Building storage : core.proximity) {
            if (!isLinkedStorage(core, storage)) continue;
            for (Building input : storage.proximity) if (!inputs.contains(input)) inputs.add(input);
        }
        for (Building input : inputs) {
            if (input.team != core.team || !input.block.quickRotate
                || !(input instanceof mindustry.world.blocks.distribution.Duct.DuctBuild)) continue;
            mindustry.world.blocks.distribution.Duct.DuctBuild duct = (mindustry.world.blocks.distribution.Duct.DuctBuild) input;
            if (duct.next != core && (duct.next == null || !isLinkedStorage(core, duct.next))) continue;
            for (int direction = 0; direction < 4; direction++) {
                Tile output = duct.tile.nearby(direction);
                if (output == null || output.build != furnace || !isDedicatedSandLine(duct)
                    || !Build.validPlace(duct.block, core.team, duct.tileX(), duct.tileY(), direction, false)) continue;
                builder.addBuild(new BuildPlan(duct.tileX(), duct.tileY(), direction, duct.block));
                return true;
            }
        }
        for (Building input : inputs) {
            if (input.team != core.team || !input.block.quickRotate
                || !(input instanceof mindustry.world.blocks.distribution.Duct.DuctBuild)) continue;
            mindustry.world.blocks.distribution.Duct.DuctBuild duct = (mindustry.world.blocks.distribution.Duct.DuctBuild) input;
            if (duct.next != core && (duct.next == null || !isLinkedStorage(core, duct.next))) continue;
            if (!isDedicatedSandLine(duct)) continue;
            Seq<Tile> route = Astar.pathfind(duct.tile, furnace.tile, tile -> 1f,
                tile -> tile.build == furnace || (tile.block() == Blocks.air
                    && duct.within(tile, 6f * Vars.tilesize)
                    && Build.validPlace(duct.block, core.team, tile.x, tile.y, 0, false)));
            if (route == null || route.size < 2 || route.size > 7) continue;
            int direction = duct.tile.relativeTo(route.first());
            if (!Build.validPlace(duct.block, core.team, duct.tileX(), duct.tileY(), direction, false)) continue;
            Seq<BuildPlan> connector = new Seq<>();
            for (int i = 0; i < route.size - 1; i++) {
                Tile tile = route.get(i);
                if (tile.build == furnace) break;
                connector.add(new BuildPlan(tile.x, tile.y, tile.relativeTo(route.get(i + 1)), duct.block));
            }
            if (connector.isEmpty() || !queueConstructionBatch(core, connector)) continue;
            builder.addBuild(new BuildPlan(duct.tileX(), duct.tileY(), direction, duct.block));
            return true;
        }
        return false;
    }

    private static boolean isDedicatedSandLine(mindustry.world.blocks.distribution.Duct.DuctBuild duct) {
        java.util.Set<Building> visited = new java.util.HashSet<>();
        Item sand = mindustry.content.Items.sand;
        while (visited.add(duct)) {
            if (duct.current != null && duct.current != sand) return false;
            Building source = null;
            for (Building adjacent : duct.proximity) {
                if (adjacent.team != duct.team || adjacent == duct.next || !adjacent.block.outputsItems()) continue;
                if (adjacent instanceof mindustry.world.blocks.distribution.Duct.DuctBuild
                    && ((mindustry.world.blocks.distribution.Duct.DuctBuild) adjacent).next != duct) continue;
                if (source != null) return false;
                source = adjacent;
            }
            if (source == null) return false;
            if (source.block instanceof mindustry.world.blocks.production.WallCrafter) {
                return ((mindustry.world.blocks.production.WallCrafter) source.block).output == sand;
            }
            if (source instanceof mindustry.world.blocks.production.Drill.DrillBuild) {
                return ((mindustry.world.blocks.production.Drill.DrillBuild) source).dominantItem == sand;
            }
            if (!(source instanceof mindustry.world.blocks.distribution.Duct.DuctBuild)) return false;
            duct = (mindustry.world.blocks.distribution.Duct.DuctBuild) source;
        }
        return false;
    }

    private static boolean buildMiningDock(CoreBuild core, Item ore) {
        Block dock = metrum.content.MetrumUnitBlocks.airMinerDock;
        if (dock == null || !dock.environmentBuildable() || !canTrainMiningDrone(core, ore)) return false;
        Building existing = core.proximity.find(b -> b.block == dock && b.team == core.team
            && b.enabled && b.closestCore() == core);
        if (existing != null) return redirectMiningDock(core, existing);
        if (count(core, b -> b.block == dock) > 0) return false;
        int coreMin = -(core.block.size - 1) / 2, coreMax = core.block.size / 2;
        int dockMin = -(dock.size - 1) / 2, dockMax = dock.size / 2;
        for (int direction = 0; direction < 4; direction++) {
            int dx = arc.math.geom.Geometry.d4x(direction), dy = arc.math.geom.Geometry.d4y(direction);
            int distance = direction < 2 ? coreMax - dockMin + 1 : dockMax - coreMin + 1;
            Tile site = Vars.world.tile(core.tileX() + dx * distance, core.tileY() + dy * distance);
            if (site == null || !Build.validPlace(dock, core.team, site.x, site.y, direction, false)) continue;
            Tile exit = miningDockExit(site, dock, direction);
            if (exit == null || exit.block() != Blocks.air) continue;
            return queueConstructionBatch(core, Seq.with(new BuildPlan(site.x, site.y, direction, dock)), true);
        }
        return false;
    }

    private static boolean redirectMiningDock(CoreBuild core, Building dock) {
        Tile currentExit = miningDockExit(dock.tile, dock.block, dock.rotation);
        if (currentExit != null && (currentExit.block() == Blocks.air
            || (currentExit.team() == core.team && currentExit.block().acceptsPayload))) return false;
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder == null || !builder.plans.isEmpty()) return false;
        for (int direction = 0; direction < 4; direction++) {
            Tile exit = miningDockExit(dock.tile, dock.block, direction);
            if (exit == null || exit.block() != Blocks.air
                || !Build.validPlace(dock.block, core.team, dock.tileX(), dock.tileY(), direction, false)) continue;
            builder.addBuild(new BuildPlan(dock.tileX(), dock.tileY(), direction, dock.block));
            return true;
        }
        return false;
    }

    private static Tile miningDockExit(Tile site, Block dock, int direction) {
        int distance = direction < 2 ? dock.size / 2 + 1 : (dock.size - 1) / 2 + 1;
        return Vars.world.tile(site.x + arc.math.geom.Geometry.d4x(direction) * distance,
            site.y + arc.math.geom.Geometry.d4y(direction) * distance);
    }

    private static boolean canTrainMiningDrone(CoreBuild core, Item ore) {
        mindustry.type.UnitType drone = metrum.content.MetrumUnits.monoMetT1;
        return drone.mineItems.contains(ore) && ore.hardness <= drone.mineTier && assignedMiningDrone(core) == null
            && ((drone.mineFloor && Vars.indexer.findClosestOre(core.x, core.y, ore) != null)
                || (drone.mineWalls && Vars.indexer.findClosestWallOre(core.x, core.y, ore) != null));
    }

    private static void supplyMiningDock(CoreBuild core) {
        Building dock = core.proximity.find(b -> b.block == metrum.content.MetrumUnitBlocks.airMinerDock
            && b.team == core.team && b.enabled && b.closestCore() == core);
        if (!(dock instanceof mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild)) return;
        mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild factory =
            (mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild) dock;
        mindustry.world.blocks.units.UnitFactory block = (mindustry.world.blocks.units.UnitFactory) dock.block;
        int planIndex = block.plans.indexOf(plan -> plan.unit == metrum.content.MetrumUnits.monoMetT1);
        if (planIndex < 0) return;

        assignMiningDrone(core);
        ItemStack demand = miningDemand.get(core);
        if (demand == null || !canTrainMiningDrone(core, demand.item)) {
            if (factory.currentPlan == planIndex) mindustry.gen.Call.tileConfig(null, factory, -1);
            return;
        }
        if (factory.currentPlan != planIndex) mindustry.gen.Call.tileConfig(null, factory, planIndex);
        ItemStack[] requirements = block.plans.get(planIndex).requirements;
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        for (ItemStack cost : requirements) {
            int needed = cost.amount - factory.items.get(cost.item);
            if (needed > 0 && core.items.get(cost.item) - reserve.getOrDefault(cost.item, 0) < needed) return;
        }
        for (ItemStack cost : requirements) {
            int needed = cost.amount - factory.items.get(cost.item);
            if (needed <= 0) continue;
            core.items.remove(cost.item, needed);
            factory.items.add(cost.item, needed);
        }
    }

    private static boolean hasLocalTungstenDrone(CoreBuild core, Item ore) {
        return mindustry.gen.Groups.unit.contains(drone -> {
            if (drone.dead() || drone.team != core.team || !(drone.controller() instanceof ResourceDroneAI)
                || ((ResourceDroneAI) drone.controller()).home != core) return false;
            if (!((ResourceDroneAI) drone.controller()).isMakingTungstenProgress()) return false;
            if (drone.stack.amount > 0 && drone.stack.item == ore) return true;
            if (drone.stack.amount > 0 || !drone.canMine(ore) || !drone.type.mineItems.contains(ore)) return false;
            Tile deposit = metrum.ai.SmartMining.findMineableOre(drone, ore);
            return deposit != null && core.within(deposit, 30f * Vars.tilesize);
        });
    }

    private static boolean hasRecoveryGoal(CoreBuild core, Item item) {
        if (item == metrum.content.MetrumItems.steel || item == mindustry.content.Items.tungsten) return true;
        java.util.Deque<ItemStack> pending = deferredDemand.get(core);
        return pending != null && pending.stream().anyMatch(goal ->
            goal.item == metrum.content.MetrumItems.steel || goal.item == mindustry.content.Items.tungsten);
    }

    static java.util.Map<Item, Integer> queuedBuildReserve(mindustry.gen.Unit builder) {
        return planMaterials(builder == null ? null : builder.plans);
    }

    static java.util.Map<Item, Integer> planMaterials(Iterable<BuildPlan> plans) {
        java.util.Map<Item, Integer> reserve = new java.util.HashMap<>();
        if (plans == null) return reserve;
        for (BuildPlan plan : plans) {
            if (plan.breaking || plan.block == null) continue;
            for (ItemStack cost : plan.block.requirements) {
                int amount = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
                reserve.put(cost.item, reserve.getOrDefault(cost.item, 0) + amount);
            }
        }
        return reserve;
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
        for (java.util.Map.Entry<Item, Integer> cost : queuedBuildReserve(assignedBuilder(core)).entrySet()) {
            reserve.put(cost.getKey(), reserve.getOrDefault(cost.getKey(), 0) + cost.getValue());
        }
        // Keep the dead constructor's remaining jobs funded until they transfer to its replacement.
        for (java.util.Map.Entry<Item, Integer> cost : planMaterials(abandonedPlans.get(core)).entrySet()) {
            reserve.put(cost.getKey(), reserve.getOrDefault(cost.getKey(), 0) + cost.getValue());
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

    /** Queue real construction; the engine consumes resources as the drone builds, not here. */
    static boolean queueConstruction(CoreBuild core, Block block, Tile site, int rotation) {
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder == null || !builder.plans.isEmpty()) return stalled("konstruktor zajęty lub niedostępny");
        if (site == null || !Build.validPlace(block, core.team, site.x, site.y, rotation, false)) return false;
        for (ItemStack cost : block.requirements) {
            int needed = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
            if (core.items.get(cost.item) < needed) {
                miningDemand.putIfAbsent(core, new ItemStack(cost.item, needed));
                return stalled("brak: " + cost.item.localizedName + " (" + core.items.get(cost.item) + "/" + needed + ")");
            }
        }
        builder.addBuild(new mindustry.entities.units.BuildPlan(site.x, site.y, rotation, block));
        return true;
    }
    static boolean queueConstructionBatch(CoreBuild core, Seq<BuildPlan> plans) {
        return queueConstructionBatch(core, plans, false);
    }

    static boolean queueConstructionBatch(CoreBuild core, Seq<BuildPlan> plans, boolean protectReserve) {
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder == null || !builder.plans.isEmpty() || plans.isEmpty()) return false;
        java.util.Map<Item, Long> total = new java.util.HashMap<>();
        for (BuildPlan plan : plans) {
            if (!Build.validPlace(plan.block, core.team, plan.x, plan.y, plan.rotation, false)) return false;
            for (ItemStack cost : plan.block.requirements) {
                long amount = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
                total.put(cost.item, total.getOrDefault(cost.item, 0L) + amount);
            }
        }
        java.util.Map<Item, Integer> reserve = protectReserve ? economicReserve(core) : java.util.Collections.emptyMap();
        for (java.util.Map.Entry<Item, Long> cost : total.entrySet()) {
            if ((long) core.items.get(cost.getKey()) - reserve.getOrDefault(cost.getKey(), 0) < cost.getValue()) {
                long needed = cost.getValue() + reserve.getOrDefault(cost.getKey(), 0);
                if (needed <= Integer.MAX_VALUE) miningDemand.putIfAbsent(core, new ItemStack(cost.getKey(), (int) needed));
                return stalled("brak materiałów na cały plan: " + cost.getKey().localizedName);
            }
        }
        for (BuildPlan plan : plans) builder.addBuild(plan);
        return true;
    }
    static boolean buildPower(CoreBuild core) {
        return buildPower(core, false);
    }

    private static boolean buildPower(CoreBuild core, boolean allowWaterPump) {
        return buildPower(core, allowWaterPump, core);
    }

    private static boolean buildPower(CoreBuild core, boolean allowWaterPump, Building nearestTo) {
        Block gen = Blocks.turbineCondenser;
        Tile best = null;
        float bestDst = Float.MAX_VALUE;
        for (int dx = -35; dx <= 35; dx++) for (int dy = -35; dy <= 35; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || !(t.floor() instanceof mindustry.world.blocks.environment.SteamVent)) continue;
            if (!((mindustry.world.blocks.environment.SteamVent) t.floor()).isCenterVent(t)) continue;
            if (!Build.validPlace(gen, core.team, t.x, t.y, 0, false)) continue;
            float d = nearestTo.dst2(t);
            if (d < bestDst) { bestDst = d; best = t; }
        }
        if (best == null) {
            if (allowWaterPump && buildWaterPump(core)) return true;
            return stalled("geoterma: brak wolnego miejsca na wyziewie");
        }
        return queueConstruction(core, gen, best, 0);
    }

    private static boolean buildWaterPump(CoreBuild core) {
        mindustry.type.Liquid water = mindustry.content.Liquids.water;
        if (count(core, b -> b.enabled && b instanceof mindustry.world.blocks.production.Pump.PumpBuild
            && ((mindustry.world.blocks.production.Pump.PumpBuild) b).liquidDrop == water) > 0) return false;
        Block pump = Blocks.reinforcedPump;
        if (!pump.environmentBuildable()) return false;
        Tile best = null;
        float distance = Float.MAX_VALUE;
        for (int dx = -35; dx <= 35; dx++) for (int dy = -35; dy <= 35; dy++) {
            Tile tile = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (tile == null || tile.floor().liquidDrop != water || core.dst2(tile) >= distance
                || !Build.validPlace(pump, core.team, tile.x, tile.y, 0, false)) continue;
            best = tile;
            distance = core.dst2(tile);
        }
        if (best == null) return false;
        if (prioritizeWorkshopMaterials(core, pump)) return stalled("brak materiałów na pompę wody");
        return queueConstructionBatch(core, Seq.with(new BuildPlan(best.x, best.y, 0, pump)), true);
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

    private static boolean prioritizeArcFurnacePower(CoreBuild core) {
        ItemStack demand = miningDemand.get(core);
        if (demand != null && needsArcFurnaceSand(core, demand)) {
            Building furnace = findSandArcFurnace(core);
            if (furnace != null && furnace.power != null && furnace.power.status < 0.999f && furnace.shouldConsume()
                && furnace.items.get(mindustry.content.Items.sand) >= requiredPerBatch(furnace, mindustry.content.Items.sand)
                && furnace.items.get(mindustry.content.Items.graphite) >= requiredPerBatch(furnace, mindustry.content.Items.graphite)) {
                if (extendPowerGrid(core, furnace)) return true;
                mindustry.world.blocks.power.PowerGraph graph = furnace.power.graph;
                if (miningDemand.get(core) == demand && graph.getLastPowerProduced() > 0f
                    && graph.getLastPowerNeeded() > graph.getLastPowerProduced() + 0.001f
                    && graph.getLastPowerStored() <= 0.001f) return buildPower(core, false, furnace);
            }
        }
        return false;
    }

    static boolean extendPowerGrid(CoreBuild core) {
        return extendPowerGrid(core, null);
    }

    private static boolean extendPowerGrid(CoreBuild core, Building priority) {
        Block node = Blocks.beamNode;
        // Give off-lattice buildings an adjacent connection point before filling the regular grid.
        Seq<Building> endpoints = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> (priority == null || b == priority) && b.power != null && (b.block.consPower != null
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
                return queuePowerNode(core, node, t, priority != null);
            }
        }
        // Grow a continuous connection from a producing graph to an isolated node.
        Seq<Building> nodes = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block == node && b.power != null, nodes::add);
        Seq<Building[]> pairs = new Seq<>();
        for (Building a : nodes) for (Building b : nodes) {
            if (a.power.graph == b.power.graph || a.power.graph.getLastPowerProduced() <= 0f) continue;
            if (priority == null) {
                if (b.power.graph.getLastPowerProduced() > 0f) continue;
            } else {
                if (b.power.graph != priority.power.graph
                    || a.power.graph.getLastPowerProduced() <= a.power.graph.getLastPowerNeeded() + 0.001f) continue;
            }
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
                return queuePowerNode(core, node, t, priority != null);
            }
        }
        if (priority != null) return false;
        int step = 8, placed = 0;
        for (int r = step; r <= 32 && placed < 3; r += step) {
            for (int dx = -r; dx <= r && placed < 3; dx += step) for (int dy = -r; dy <= r && placed < 3; dy += step) {
                if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
                if (t == null || t.block() != Blocks.air || !Build.validPlace(node, core.team, t.x, t.y, 0, false)) continue;
                return queueConstruction(core, node, t, 0);
            }
        }
        return placed > 0 || stalled("sieć prądu: brak dostępnego miejsca na dalsze węzły");
    }

    private static boolean queuePowerNode(CoreBuild core, Block node, Tile site, boolean priority) {
        if (priority) {
            for (ItemStack cost : node.requirements) {
                int needed = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
                if (core.items.get(cost.item) < needed && prioritizeProducerDependency(core, cost.item, needed)) {
                    return stalled("brak materiału na zasilanie pieca łukowego: " + cost.item.localizedName);
                }
            }
        }
        return queueConstruction(core, node, site, 0);
    }

    // ---------- step 3: water - conduit from a water source (turbine condenser / pump) to a thirsty consumer ----------

    /** Free space only in empty pipe components having no connection to any liquid building. */
    static boolean clearUnusedWaterPipes(CoreBuild core) {
        Seq<Building> pipes = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block == Blocks.reinforcedConduit, pipes::add);
        Seq<Building> unused = unusedWaterPipes(pipes, core.team);
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder == null || !builder.plans.isEmpty()) return false;
        for (Building pipe : unused) {
            if (!Build.validBreak(core.team, pipe.tileX(), pipe.tileY())) return false;
        }
        for (Building pipe : unused) builder.addBuild(new BuildPlan(pipe.tileX(), pipe.tileY()));
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
    static boolean waterSiteSafe(Tile site, Team team) {
        return liquidSiteSafe(site, team, mindustry.content.Liquids.water);
    }
    static boolean liquidSiteSafe(Tile site, Team team, mindustry.type.Liquid liquid) {
        return liquidSiteSafe(site, team, liquid, null, null);
    }
    static boolean liquidSiteSafe(Tile site, Team team, mindustry.type.Liquid liquid, Building source, Building destination) {
        for (int direction = 0; direction < 4; direction++) {
            Tile neighbour = site.nearby(direction);
            Building building = neighbour == null ? null : neighbour.build;
            if (building != null && building != source && building != destination && building.team == team && building.block.hasLiquids
                && !liquidCompatible(building, liquid)) return false;
        }
        return true;
    }
    static boolean waterCompatible(Building pipe) {
        return liquidCompatible(pipe, mindustry.content.Liquids.water);
    }
    static boolean liquidCompatible(Building pipe, mindustry.type.Liquid carried) {
        return pipe.liquids == null || pipe.liquids.sum((liquid, amount) ->
            liquid == carried ? 0f : amount) <= 0.001f;
    }
    static boolean waterLineConnected(Building from, Building to) {
        return liquidLineConnected(from, to, mindustry.content.Liquids.water);
    }
    static boolean liquidLineConnected(Building from, Building to, mindustry.type.Liquid liquid) {
        if (from.team != to.team) return false;
        if (from.block == Blocks.reinforcedConduit && !liquidCompatible(from, liquid)) return false;
        Block conduit = Blocks.reinforcedConduit;
        java.util.Set<Building> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<Building> queue = new java.util.ArrayDeque<>();
        if (from.block == conduit) queue.add(from);
        else {
            if (from.proximity.contains(to)) return true;
            for (Building adjacent : from.proximity) {
                if (adjacent.team == from.team && adjacent.block == conduit && liquidCompatible(adjacent, liquid)) {
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
            if (next == null || next.team != from.team || next.block != conduit || !liquidCompatible(next, liquid)) continue;
            Tile nextOutput = next.tile.nearby(next.rotation);
            if (nextOutput != null && nextOutput.build != pipe) queue.add(next);
        }
        return false;
    }
    /** Repair one missing pipe before considering a whole new water line. */
    /** Valid only during one planning call, which ends immediately after any world mutation. */
    static class WaterConnections {
        private final java.util.Map<Building, java.util.Map<Building, Boolean>> results = new java.util.HashMap<>();
        boolean connected(Building from, Building to) {
            return results.computeIfAbsent(from, ignored -> new java.util.HashMap<>())
                .computeIfAbsent(to, ignored -> waterLineConnected(from, to));
        }
    }
    static boolean repairWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations, WaterConnections connections) {
        Block conduit = Blocks.reinforcedConduit;
        if (destinations.isEmpty()) return false;
        for (Building source : sources) {
            if (source.block != conduit || !waterCompatible(source)) continue;
            Tile gap = source.tile.nearby(source.rotation);
            if (gap == null || gap.block() != Blocks.air
                || !(waterSiteSafe(gap, core.team) && Build.validPlace(conduit, core.team, gap.x, gap.y, 0, false))) continue;
            for (int direction = 0; direction < 4; direction++) {
                if (direction == (source.rotation + 2) % 4) continue;
                Tile beyond = gap.nearby(direction);
                Building next = beyond == null ? null : beyond.build;
                if (next == null || next.team != core.team) continue;
                if (next.block != conduit && !destinations.contains(next)) continue;
                if (next.block == conduit && !waterCompatible(next)) continue;
                if (next.block == conduit) {
                    Tile output = next.tile.nearby(next.rotation);
                    if (output == gap) continue; // opposing pipes would not form a working connection
                }
                if (!destinations.contains(to -> next == to || connections.connected(next, to))) continue;
                return queueConstruction(core, conduit, gap, direction);
            }
        }
        return false;
    }
    /** Reuse an existing downstream line across a short straight break instead of laying a parallel route. */
    static boolean repairStraightWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations, WaterConnections connections) {
        if (destinations.isEmpty()) return false;
        Block conduit = Blocks.reinforcedConduit;
        for (Building source : sources) {
            if (source.block != conduit || !waterCompatible(source)) continue;
            Seq<Tile> gaps = new Seq<>();
            for (int distance = 1; distance <= 7; distance++) {
                Tile t = Vars.world.tile(source.tileX() + arc.math.geom.Geometry.d4x(source.rotation) * distance,
                    source.tileY() + arc.math.geom.Geometry.d4y(source.rotation) * distance);
                if (t == null) break;
                if (t.build == null) {
                    if (distance > 6 || t.block() != Blocks.air
                        || !(waterSiteSafe(t, core.team) && Build.validPlace(conduit, core.team, t.x, t.y, source.rotation, false))) break;
                    gaps.add(t);
                    continue;
                }
                Building next = t.build;
                if (gaps.size < 2 || next.team != core.team) break;
                if (next.block != conduit && !destinations.contains(next)) break;
                if (next.block == conduit && !waterCompatible(next)) break;
                if (next.block == conduit && next.rotation == (source.rotation + 2) % 4) break;
                if (!destinations.contains(to -> next == to || connections.connected(next, to))) break;
                Seq<BuildPlan> plans = new Seq<>();
                for (Tile gap : gaps) plans.add(new BuildPlan(gap.x, gap.y, source.rotation, conduit));
                return queueConstructionBatch(core, plans);
            }
        }
        return false;
    }
    /** Search at most six missing tiles, allowing bends while preserving the old downstream route. */
    static boolean repairBentWaterGap(CoreBuild core, Seq<Building> sources, Seq<Building> destinations, WaterConnections connections) {
        if (destinations.isEmpty() || sources.isEmpty()) return false;
        Block conduit = Blocks.reinforcedConduit;
        int first = Math.floorMod(bentRepairCursor.getOrDefault(core, 0), sources.size);
        // Bound expensive searches, rotating through candidates so later pipe ends are not starved.
        for (int attempt = 0; attempt < Math.min(8, sources.size); attempt++) {
            int index = (first + attempt) % sources.size;
            bentRepairCursor.put(core, (index + 1) % sources.size);
            Building source = sources.get(index);
            if (source.block != conduit || !waterCompatible(source)) continue;
            Tile start = source.tile.nearby(source.rotation);
            if (start == null || start.block() != Blocks.air
                || !(waterSiteSafe(start, core.team) && Build.validPlace(conduit, core.team, start.x, start.y, 0, false))) continue;
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
                        if (next.block == conduit && !waterCompatible(next)) continue;
                        if (next.block == conduit && next.tile.nearby(next.rotation) == tail) continue;
                        if (!destinations.contains(to -> next == to || connections.connected(next, to))) continue;
                        Seq<BuildPlan> plans = new Seq<>();
                        for (int i = 0; i < path.size; i++) {
                            Tile t = path.get(i);
                            int rotation = i + 1 < path.size ? t.relativeTo(path.get(i + 1)) : direction;
                            plans.add(new BuildPlan(t.x, t.y, rotation, conduit));
                        }
                        return queueConstructionBatch(core, plans);
                    }
                    if (path.size >= 6 || seen.contains(nextTile) || nextTile.block() != Blocks.air
                        || !(waterSiteSafe(nextTile, core.team) && Build.validPlace(conduit, core.team, nextTile.x, nextTile.y, 0, false))) continue;
                    Seq<Tile> extended = new Seq<>(path); extended.add(nextTile);
                    seen.add(nextTile); queue.addLast(extended);
                }
            }
        }
        return false;
    }
    static boolean pipeWater(CoreBuild core) {
        WaterConnections connections = new WaterConnections();
        mindustry.type.Liquid water = mindustry.content.Liquids.water;
        Seq<Building> sources = new Seq<>(), destinations = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> canSupplyWater(b), sources::add);
        if (sources.isEmpty()) return false;
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.block.consumesLiquid(water) && b.liquids != null && b.liquids.get(water) < 1f,
            destinations::add);
        // Low liquid level is not proof of a missing line: do not build parallel duplicates.
        destinations.removeAll(to -> sources.contains(from -> connections.connected(from, to)));
        ItemStack demand = miningDemand.get(core);
        if (demand != null && hasRecoveryGoal(core, demand.item)) {
            Seq<Building> boilers = new Seq<>();
            for (Building destination : destinations) {
                if (destination.enabled && destination.block == metrum.content.MetrumProduction.steamFurnace) {
                    boilers.add(destination);
                }
            }
            if (!boilers.isEmpty()) {
                if (queueWaterLine(core, sources, boilers, connections)) return true;
                destinations.removeAll(boilers::contains);
            }
        }
        return queueWaterLine(core, sources, destinations, connections);
    }

    private static boolean queueWaterLine(CoreBuild core, Seq<Building> sources, Seq<Building> destinations,
                                          WaterConnections connections) {
        if (destinations.isEmpty()) return false;
        if (repairWaterGap(core, sources, destinations, connections)) return true;
        if (repairStraightWaterGap(core, sources, destinations, connections)) return true;
        if (repairBentWaterGap(core, sources, destinations, connections)) return true;
        return queueLiquidLine(core, sources, destinations, mindustry.content.Liquids.water);
    }

    static boolean pipeSteam(CoreBuild core) {
        mindustry.type.Liquid steam = metrum.content.MetrumLiquids.steam;
        if (steam == null) return false;
        Seq<Building> sources = new Seq<>(), destinations = new Seq<>();
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            BasePlanner::isSteamSource, sources::add);
        if (sources.isEmpty()) return false;
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> (b.block == metrum.content.MetrumProduction.infusionSmelter
                || b.block == metrum.content.MetrumProduction.salvagerAdvanced) && b.enabled
                && !sources.contains(from -> liquidLineConnected(from, b, steam)), destinations::add);
        return queueLiquidLine(core, sources, destinations, steam);
    }

    private static boolean isSteamSource(Building building) {
        if (!building.enabled || !(building.block instanceof mindustry.world.blocks.production.GenericCrafter)) return false;
        mindustry.type.LiquidStack output = ((mindustry.world.blocks.production.GenericCrafter) building.block).outputLiquid;
        return output != null && output.liquid == metrum.content.MetrumLiquids.steam;
    }

    static boolean queueLiquidLine(CoreBuild core, Seq<Building> sources, Seq<Building> destinations, mindustry.type.Liquid liquid) {
        Seq<Building[]> pairs = new Seq<>();
        for (Building from : sources) for (Building to : destinations) {
            if (from != to) pairs.add(new Building[]{from, to});
        }
        pairs.sort((a, b) -> Float.compare(a[0].dst2(a[1]), b[0].dst2(b[1])));
        Block conduit = Blocks.reinforcedConduit;
        boolean water = liquid == mindustry.content.Liquids.water;
        ItemStack demand = miningDemand.get(core);
        boolean recoveryWater = water && demand != null && hasRecoveryGoal(core, demand.item);
        int shortestDemandLine = Integer.MAX_VALUE;
        int pipeBudget = Integer.MAX_VALUE;
        for (ItemStack cost : conduit.requirements) {
            int perPipe = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
            if (perPipe > 0) pipeBudget = Math.min(pipeBudget, core.items.get(cost.item) / perPipe);
        }
        if (pipeBudget == 0 && (demand == null || (water && !recoveryWater))) return false;
        for (Building[] pair : pairs) {
            Building from = pair[0], to = pair[1];
            Seq<Tile> path = Astar.pathfind(from.tile, to.tile, tile -> 1f,
                t -> t.build == from || t.build == to ||
                    (t.block() == Blocks.air && Build.validPlace(conduit, core.team, t.x, t.y, 0, false)
                        && (water ? waterSiteSafe(t, core.team)
                            : liquidSiteSafe(t, core.team, liquid, from, to))));
            if (path == null || path.size < 3) continue;
            Seq<BuildPlan> plans = new Seq<>();
            for (int i = 0; i < path.size - 1; i++) {
                Tile t = path.get(i), next = path.get(i + 1);
                if (t.build != null) continue;
                plans.add(new BuildPlan(t.x, t.y, t.relativeTo(next), conduit));
            }
            if (plans.size <= pipeBudget) return queueConstructionBatch(core, plans);
            boolean neededForDemand = water ? recoveryWater && to.block == metrum.content.MetrumProduction.steamFurnace
                : demand != null && producesItem(to.block, demand.item);
            if (neededForDemand) {
                shortestDemandLine = Math.min(shortestDemandLine, plans.size);
            }
        }
        if (shortestDemandLine != Integer.MAX_VALUE) {
            for (ItemStack cost : conduit.requirements) {
                long needed = (long) Math.round(cost.amount * Vars.state.rules.buildCostMultiplier) * shortestDemandLine;
                if (needed <= Integer.MAX_VALUE && core.items.get(cost.item) < needed
                    && prioritizeProducerDependency(core, cost.item, (int) needed)) {
                    return stalled((water ? "brak materiału na wodę do kotła: " : "brak materiału na rurociąg parowy: ")
                        + cost.item.localizedName);
                }
            }
        }
        return false;
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
                if (!Build.validPlace(router, core.team, duct.x, duct.y, rot, false)) continue;
                // Complete the consumer first, then replace the duct using the game's normal construction path.
                Seq<BuildPlan> plans = Seq.with(new BuildPlan(origin.x, origin.y, 0, factory),
                    new BuildPlan(duct.x, duct.y, rot, router));
                return queueConstructionBatch(core, plans, true);
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
            Seq<BuildPlan> plans = Seq.with(new BuildPlan(t.x, t.y, 0, turret));
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
                    plans.add(new BuildPlan(m.x, m.y, 0, magazine));
                    break;
                }
            }
            if (queueConstructionBatch(core, plans, true)) return true;
            if (plans.size > 1) {
                plans.pop(); // optional magazine must not prevent an affordable turret
                return queueConstructionBatch(core, plans, true);
            }
            return false;
        }
        return false;
    }

    // ---------- step 6: wall ring; leaves a gap where the fire is heaviest (all AI levels) ----------

    static float wallGapAngle(CoreBuild core) {
        int best = -1;
        float max = 0f;
        float[] fire = fireByCore.computeIfAbsent(core, ignored -> new float[12]);
        for (int i = 0; i < fire.length; i++) if (fire[i] > max) { max = fire[i]; best = i; }
        Building enemy = Vars.state.rules.defaultTeam.core();
        return best >= 0 ? best * 30f + 15f : (enemy != null ? core.angleTo(enemy) : 0f);
    }

    static boolean wallNeedsOpening(CoreBuild core, Tile tile) {
        return tile != null && isRingWall(tile.block()) && tile.team() == core.team
            && Math.abs(core.dst(tile) - 20f * Vars.tilesize) <= Vars.tilesize
            && arc.math.Angles.angleDist(core.angleTo(tile), wallGapAngle(core)) < 12f;
    }
    static boolean isRingWall(Block block) {
        return block == Blocks.tungstenWall || block == Blocks.berylliumWall;
    }

    static boolean buildWalls(CoreBuild core, AiLevel level) {
        mindustry.gen.Unit builder = assignedBuilder(core);
        if (builder == null || !builder.plans.isEmpty()) return false;
        float radius = 20f * Vars.tilesize;
        // where to leave the gap: sector with the most recorded damage, else towards the player's core
        float gapAngle = wallGapAngle(core);
        float gapHalf = 12f; // ~3 tiles wide at radius 20
        // A new fire direction can move the passage onto walls built during earlier cycles.
        int cleared = 0;
        for (int dx = -21; dx <= 21 && cleared < 6; dx++) for (int dy = -21; dy <= 21 && cleared < 6; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || !isRingWall(t.block()) || t.team() != core.team) continue;
            if (Math.abs(core.dst(t) - radius) > Vars.tilesize) continue;
            if (arc.math.Angles.angleDist(core.angleTo(t), gapAngle) >= gapHalf) continue;
            if (!Build.validBreak(core.team, t.x, t.y)) continue;
            builder.addBuild(new BuildPlan(t.x, t.y));
            cleared++;
        }
        if (cleared > 0) return true;
        Block wall = canAffordRingWall(core, Blocks.tungstenWall) ? Blocks.tungstenWall : Blocks.berylliumWall;
        if (!canAffordRingWall(core, wall)) return stalled("brak surowców na plan muru");
        // wall arc facing the threat (+-70 degrees around the gap direction)
        int placed = 0;
        for (float a = gapAngle - 70f; a <= gapAngle + 70f && placed < 6; a += 3f) {
            if (Math.abs(arc.math.Angles.angleDist(a, gapAngle)) < gapHalf) continue; // the passage
            Tile t = Vars.world.tileWorld(core.x + arc.math.Angles.trnsx(a, radius), core.y + arc.math.Angles.trnsy(a, radius));
            if (t == null || t.block() != Blocks.air || !Build.validPlace(wall, core.team, t.x, t.y, 0, false)) continue;
            builder.addBuild(new mindustry.entities.units.BuildPlan(t.x, t.y, 0, wall));
            return true; // the unit constructs it and the engine charges actual construction materials

        }
        return placed > 0;
    }

    private static boolean canAffordRingWall(CoreBuild core, Block wall) {
        if (!wall.environmentBuildable()) return false;
        for (ItemStack cost : wall.requirements) {
            if (availableForAmmo(core, cost.item) < Math.round(cost.amount * Vars.state.rules.buildCostMultiplier)) return false;
        }
        return true;
    }

    // ---------- step 1: drill + path to core ----------

    /** places one drill on the nearest free ore of the scarcest mineable item, then a duct path to the core */
    static boolean buildDrillLine(CoreBuild core, AiLevel level) {
        return buildDrillLine(core, level, false);
    }

    static boolean buildDrillLine(CoreBuild core, AiLevel level, boolean demandOnly) {
        return buildDrillLine(core, level, demandOnly, 30);
    }

    private static boolean buildDrillLine(CoreBuild core, AiLevel level, boolean demandOnly, int searchRadius) {
        Seq<BuildPlan> fallback = new Seq<>();
        if (buildDrillLine(core, demandOnly, searchRadius, new arc.struct.IntSet(), fallback)) return true;
        if (fallback.isEmpty()) return false;
        ItemStack demand = miningDemand.get(core);
        if (demandOnly && demand != null) {
            boolean recoveryAlternative = demand.item == mindustry.content.Items.tungsten
                || ((demand.item == mindustry.content.Items.coal || demand.item == mindustry.content.Items.sand)
                    && hasRecoveryGoal(core, demand.item));
            // Give local production a chance before committing to a more expensive extraction route.
            if (!recoveryAlternative || searchRadius > 30) {
                for (java.util.Map.Entry<Item, Integer> cost : planMaterials(fallback).entrySet()) {
                    Item material = cost.getKey();
                    if (core.items.get(material) < cost.getValue()
                        && prioritizeProducerDependency(core, material, cost.getValue())) {
                        return stalled("brak materiału na wiertnię i transport: " + material.localizedName);
                    }
                }
            }
        }
        return queueConstructionBatch(core, fallback);
    }

    private static boolean buildDrillLine(CoreBuild core, boolean demandOnly, int searchRadius,
                                          arc.struct.IntSet rejected, Seq<BuildPlan> fallback) {
        Block chosen = Blocks.plasmaBore;
        Tile ore = findOreSpot(core, chosen, searchRadius, rejected);
        ItemStack demand = miningDemand.get(core);
        // Consider modded wall drills as well as the vanilla plasma bore.
        for (Block candidate : Vars.content.blocks()) {
            if (!(candidate instanceof mindustry.world.blocks.production.BeamDrill) || candidate == chosen
                || !candidate.environmentBuildable() || !canSupportDrill(core, candidate)) continue;
            Tile alternative = findOreSpot(core, candidate, searchRadius, rejected);
            if (alternative == null) continue;
            int direction = faceWall(alternative, candidate, core, minedResources(core));
            Item candidateItem = wallOreInDirection(alternative,
                (mindustry.world.blocks.production.BeamDrill) candidate, direction);
            boolean wanted = demand != null && candidateItem == demand.item;
            boolean currentWanted = ore != null && demand != null && wallOreInDirection(ore,
                (mindustry.world.blocks.production.BeamDrill) chosen,
                faceWall(ore, chosen, core, minedResources(core))) == demand.item;
            if (ore == null || !canSupportDrill(core, chosen) || (wanted && !currentWanted)) {
                chosen = candidate;
                ore = alternative;
            }
        }
        if (ore == null || demand != null) {
            for (Block candidate : Vars.content.blocks()) {
                if (!(candidate instanceof mindustry.world.blocks.production.Drill) || !candidate.environmentBuildable()) continue;
                if (!canSupportDrill(core, candidate)) continue;
                Tile floorOre = findFloorOreSpot(core, (mindustry.world.blocks.production.Drill) candidate, searchRadius, rejected);
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
        Seq<BuildPlan> plans = extractorPlans(core, drill, ore, rotation, output);
        if (plans != null) {
            long missing = missingPlanMaterials(core, plans);
            if (missing == 0) return queueConstructionBatch(core, plans);
            if (fallback.isEmpty() || missing < missingPlanMaterials(core, fallback)) {
                fallback.clear();
                fallback.addAll(plans);
            }
        }
        rejected.add(ore.pos());
        if (rejected.size < 4) return buildDrillLine(core, demandOnly, searchRadius, rejected, fallback);
        return fallback.isEmpty() && stalled("brak trasy z wiertni do rdzenia");
    }

    private static long missingPlanMaterials(CoreBuild core, Seq<BuildPlan> plans) {
        long missing = 0;
        for (java.util.Map.Entry<Item, Integer> cost : planMaterials(plans).entrySet()) {
            missing += Math.max(0, cost.getValue() - core.items.get(cost.getKey()));
        }
        return missing;
    }

    static boolean buildWallExtractor(CoreBuild core, Block scraper) {
        return buildWallExtractor(core, scraper, true);
    }

    private static boolean buildWallExtractor(CoreBuild core, Block scraper, boolean requestMaterials) {
        if (scraper == null || !scraper.environmentBuildable() || count(core, b -> b.block == scraper) > 0) return false;
        if (requestMaterials && prioritizeWorkshopMaterials(core, scraper)) return false;
        if (!requestMaterials && !canSupportDrill(core, scraper)) return false;
        Seq<BuildPlan> sites = new Seq<>();
        for (int dx = -30; dx <= 30; dx++) for (int dy = -30; dy <= 30; dy++) {
            Tile tile = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (tile == null || tile.block() != Blocks.air) continue;
            for (int direction = 0; direction < 4; direction++) {
                if (!Build.validPlace(scraper, core.team, tile.x, tile.y, direction, false)) continue;
                sites.add(new BuildPlan(tile.x, tile.y, direction, scraper));
                break;
            }
        }
        sites.sort((a, b) -> Float.compare(core.dst2(a), core.dst2(b)));
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        java.util.Map<Item, Integer> cheapestShortages = null;
        int cheapestPlanSize = Integer.MAX_VALUE;
        for (BuildPlan site : sites) {
            Item output = ((mindustry.world.blocks.production.WallCrafter) scraper).output;
            Seq<BuildPlan> plans = extractorPlans(core, scraper, Vars.world.tile(site.x, site.y), site.rotation, output);
            if (plans == null || plans.size >= cheapestPlanSize) continue;
            java.util.Map<Item, Integer> shortages = new java.util.HashMap<>();
            for (java.util.Map.Entry<Item, Integer> cost : planMaterials(plans).entrySet()) {
                Item material = cost.getKey();
                int needed = cost.getValue() + reserve.getOrDefault(material, 0);
                if (core.items.get(material) < needed) shortages.put(material, needed);
            }
            if (shortages.isEmpty()) return queueConstructionBatch(core, plans, true);
            cheapestPlanSize = plans.size; // Every option uses the same scraper and duct type.
            cheapestShortages = shortages;
        }
        if (cheapestShortages != null && requestMaterials) {
            for (java.util.Map.Entry<Item, Integer> shortage : cheapestShortages.entrySet()) {
                if (prioritizeProducerDependency(core, shortage.getKey(), shortage.getValue())) {
                    return stalled("brak materiału na transport z bloku „" + scraper.localizedName + "”: " + shortage.getKey().localizedName);
                }
            }
            return stalled("brak materiałów na blok „" + scraper.localizedName + "” i transport");
        }
        return !sites.isEmpty() && stalled("brak trasy z bloku „" + scraper.localizedName + "” do rdzenia");
    }

    private static Seq<BuildPlan> extractorPlans(CoreBuild core, Block drill, Tile site, int rotation, Item output) {
        if (output == mindustry.content.Items.sand && !output.buildable
            && ((mindustry.world.blocks.storage.CoreBlock) core.block).incinerateNonBuildable) {
            for (Building furnace : coreWorkshops(core, mindustry.content.Items.silicon)) {
                if (furnace.team != core.team || !furnace.enabled || furnace.closestCore() != core
                    || !isSiliconArcFurnace(furnace.block)) continue;
                Seq<BuildPlan> direct = extractorPlansTo(core, drill, site, rotation, furnace);
                if (direct != null) return direct;
            }
        }
        return extractorPlansTo(core, drill, site, rotation, core);
    }

    private static Seq<BuildPlan> extractorPlansTo(CoreBuild core, Block drill, Tile site, int rotation, Building destination) {
        Block duct = Blocks.duct;
        // Plan against a virtual footprint: no temporary or permanent block is spawned here.
        Seq<Tile> path = Astar.pathfind(site, destination.tile, tile -> 1f,
            t -> t.build == destination || insideFootprint(t, site, drill)
                || (t.block() == Blocks.air && Build.validPlace(duct, core.team, t.x, t.y, 0, false)));
        if (path == null || path.isEmpty()) return null;
        Seq<BuildPlan> plans = Seq.with(new BuildPlan(site.x, site.y, rotation, drill));
        for (int i = 0; i < path.size - 1; i++) {
            Tile t = path.get(i), next = path.get(i + 1);
            if (insideFootprint(t, site, drill) || t.build != null) continue;
            plans.add(new BuildPlan(t.x, t.y, t.relativeTo(next), duct));
        }
        return plans;
    }

    static boolean insideFootprint(Tile tile, Tile anchor, Block block) {
        int min = -(block.size - 1) / 2, max = block.size / 2;
        return tile.x >= anchor.x + min && tile.x <= anchor.x + max
            && tile.y >= anchor.y + min && tile.y <= anchor.y + max;
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
        boolean recoveringTungsten = demand.item == mindustry.content.Items.tungsten;
        if (hasRecoveryGoal(core, demand.item) || needsArcFurnaceSand(core, demand)) {
            drainStoneCrusherSand(core);
        }
        if (recoveringTungsten || needsArcFurnaceSand(core, demand)) {
            drainSalvagerByproducts(core, !recoveringTungsten);
            if (core.items.get(demand.item) >= demand.amount) return;
        }
        if (demand.item == mindustry.content.Items.silicon || demand.item == mindustry.content.Items.tungsten
            || demand.item == metrum.content.MetrumItems.steel) {
            deliverBufferedOutput(core, demand);
            if (core.items.get(demand.item) >= demand.amount) return;
        }
        boolean recoveryNeedsSteam = demand.item == metrum.content.MetrumItems.steel
            || demand.item == mindustry.content.Items.tungsten;
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        if (isSandForSilicon(core, demand)) bufferArcFurnaceGraphite(core, reserve);
        workshops: for (Building factory : coreWorkshops(core, demand.item)) {
            if (factory.team != core.team || !factory.enabled || factory.items == null
                || factory.closestCore() != core) continue;
            boolean recoveryBoiler = recoveryNeedsSteam
                && factory.block == metrum.content.MetrumProduction.steamFurnace;
            boolean recoveryCrusher = demand.item == mindustry.content.Items.coal
                && hasRecoveryGoal(core, demand.item)
                && factory.block == metrum.content.MetrumProduction.stoneCrusher;
            if (!recoveryBoiler && !recoveryCrusher && !producesItem(factory.block, demand.item)) continue;
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
                if (missing == 0) continue;
                if (input.getValue() > factory.getMaximumAccepted(item)) continue workshops;
                if (core.items.get(item) - reserve.getOrDefault(item, 0) < missing) {
                    // Feed spare rock while waiting for coal without replacing the coal priority.
                    if (recoveryCrusher) continue workshops;
                    if (prioritizeProducerDependency(core, item, missing + reserve.getOrDefault(item, 0))) {
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

    private static void deliverBufferedOutput(CoreBuild core, ItemStack demand) {
        Item item = demand.item;
        int room = Math.min(demand.amount, core.storageCapacity) - core.items.get(item);
        if (room <= 0) return;
        Block recoveryWorkshop = item == metrum.content.MetrumItems.steel
            ? metrum.content.MetrumProduction.infusionSmelter : metrum.content.MetrumProduction.salvagerAdvanced;
        for (Building factory : coreWorkshops(core, item)) {
            if (factory.team != core.team || !factory.enabled || factory.items == null || factory.closestCore() != core) continue;
            boolean source = item == mindustry.content.Items.silicon ? isSiliconArcFurnace(factory.block)
                : factory.block == recoveryWorkshop;
            if (!source || factory.items.get(item) == 0) continue;
            int transferred = factory.removeStack(item, Math.min(room, factory.items.get(item)));
            if (transferred <= 0) continue;
            core.handleStack(item, transferred, factory);
            room -= transferred;
            if (room <= 0) return;
        }
    }

    private static void drainStoneCrusherSand(CoreBuild core) {
        Item sand = mindustry.content.Items.sand;
        for (Building factory : core.proximity) {
            if (factory.block != metrum.content.MetrumProduction.stoneCrusher || factory.team != core.team
                || factory.items == null || factory.closestCore() != core) continue;
            int buffered = factory.items.get(sand);
            if (buffered <= 0) continue;
            if (coreBurnsSand(core)) {
                Building furnace = findSandArcFurnace(core);
                if (furnace == null) continue;
                int missing = Math.min(requiredPerBatch(furnace, sand), furnace.getMaximumAccepted(sand))
                    - furnace.items.get(sand);
                if (missing > 0) furnace.items.add(sand, factory.removeStack(sand, Math.min(missing, buffered)));
            } else {
                int room = core.storageCapacity - core.items.get(sand);
                if (room > 0) core.handleStack(sand, factory.removeStack(sand, Math.min(room, buffered)), factory);
            }
        }
    }

    private static void drainSalvagerByproducts(CoreBuild core, boolean sandOnly) {
        Item sand = mindustry.content.Items.sand;
        Item tungsten = mindustry.content.Items.tungsten;
        for (Building factory : core.proximity) {
            if (factory.block != metrum.content.MetrumProduction.salvagerAdvanced || factory.team != core.team
                || factory.items == null || factory.closestCore() != core) continue;
            mindustry.world.blocks.production.Separator salvager = (mindustry.world.blocks.production.Separator) factory.block;
            for (ItemStack output : salvager.results) {
                Item item = output.item;
                int buffered = factory.items.get(item);
                if (item == tungsten || buffered <= 0 || (sandOnly && item != sand)) continue;
                if (item == sand && coreBurnsSand(core)) {
                    Building furnace = findSandArcFurnace(core);
                    if (furnace == null) continue;
                    int missing = requiredPerBatch(furnace, sand) - furnace.items.get(sand);
                    if (missing > 0) furnace.items.add(sand, factory.removeStack(sand, Math.min(missing, buffered)));
                } else {
                    int room = core.storageCapacity - core.items.get(item);
                    if (room <= 0) continue;
                    int transferred = factory.removeStack(item, Math.min(room, buffered));
                    if (transferred > 0) core.handleStack(item, transferred, factory);
                }
            }
        }
    }

    private static void bufferArcFurnaceGraphite(CoreBuild core, java.util.Map<Item, Integer> reserve) {
        Building furnace = findSandArcFurnace(core);
        if (furnace == null) return;
        Item graphite = mindustry.content.Items.graphite;
        int batch = requiredPerBatch(furnace, graphite);
        int missing = batch - furnace.items.get(graphite);
        if (missing <= 0 || batch > furnace.getMaximumAccepted(graphite)
            || core.items.get(graphite) - reserve.getOrDefault(graphite, 0) < missing) return;
        core.items.remove(graphite, missing);
        furnace.items.add(graphite, missing);
    }
    static boolean buildDemandProducer(CoreBuild core, Item item) {
        if (item == mindustry.content.Items.silicon && coreWorkshops(core, item).contains(factory -> factory.team == core.team
            && factory.enabled && factory.items != null && factory.closestCore() == core
            && (factory.block == Blocks.siliconArcFurnace
                || factory.block == metrum.content.MetrumProduction.arcFurnaceOverclocked))) return false;
        Block ready = findProducer(core, item);
        Block selected = ready != null ? ready : findProducer(core, item, true);
        if (selected == null) {
            if (item == mindustry.content.Items.tungsten) {
                prioritizeMissingWorkshop(core, metrum.content.MetrumProduction.salvagerAdvanced);
            } else if (item == metrum.content.MetrumItems.steel) {
                prioritizeMissingWorkshop(core, metrum.content.MetrumProduction.infusionSmelter);
            } else if (item == mindustry.content.Items.coal && hasRecoveryGoal(core, item)) {
                prioritizeMissingWorkshop(core, metrum.content.MetrumProduction.graphiteDecompressor);
            } else if (item == mindustry.content.Items.silicon && hasRecoveryGoal(core, item)) {
                prioritizeMissingWorkshop(core, Blocks.siliconArcFurnace);
            }
            return false;
        }
        if (selected == metrum.content.MetrumProduction.stoneCrusher && hasRecoveryGoal(core, item)) {
            Block scraper = metrum.content.MetrumBlocks.rockScraper;
            if (count(core, b -> b.block == scraper && b.enabled && b.closestCore() == core) == 0) {
                return buildWallExtractor(core, scraper);
            }
        }
        Tile site = findCoreWorkshopSite(core, selected);
        if (site == null && selected == metrum.content.MetrumProduction.arcFurnaceOverclocked
            && Blocks.siliconArcFurnace.environmentBuildable()) {
            selected = Blocks.siliconArcFurnace;
            site = findCoreWorkshopSite(core, selected);
            if (site != null && prioritizeWorkshopMaterials(core, selected)) return false;
        }
        if (site == null && isSiliconArcFurnace(selected)) {
            arc.func.Boolf<Block> fits = block -> !isSiliconArcFurnace(block) && findCoreWorkshopSite(core, block) != null;
            Block alternative = findProducer(core, item, false, fits);
            if (alternative == null) alternative = findProducer(core, item, true, fits);
            if (alternative == null) {
                blockedSiliconSites.add(core);
                return false;
            }
            selected = alternative;
            site = findCoreWorkshopSite(core, selected);
        }
        final Block producer = selected;
        if (count(core, b -> b.block == producer) > 0) return false;
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
            if (input.item == mindustry.content.Items.sand && isSiliconArcFurnace(producer) && coreBurnsSand(core)) continue;
            int needed = input.amount + reserve.getOrDefault(input.item, 0);
            for (ItemStack cost : producer.requirements) if (cost.item == input.item) {
                needed += Math.round(cost.amount * Vars.state.rules.buildCostMultiplier);
            }
            if (core.items.get(input.item) >= needed) continue;
            prioritizeProducerDependency(core, input.item, needed);
            return false;
        }
        return site != null && queueConstructionBatch(core, Seq.with(new BuildPlan(site.x, site.y, 0, producer)), true);
    }

    private static Seq<Building> coreWorkshops(CoreBuild core, Item item) {
        if (item != mindustry.content.Items.silicon) return core.proximity;
        Seq<Building> workshops = new Seq<>();
        workshops.addAll(core.proximity);
        for (Building storage : core.proximity) {
            if (!isLinkedStorage(core, storage)) continue;
            for (Building factory : storage.proximity) {
                if (isSiliconArcFurnace(factory.block) && !workshops.contains(factory)) workshops.add(factory);
            }
        }
        return workshops;
    }

    private static boolean isLinkedStorage(CoreBuild core, Building building) {
        return building.team == core.team && building instanceof mindustry.world.blocks.storage.StorageBlock.StorageBuild
            && ((mindustry.world.blocks.storage.StorageBlock.StorageBuild) building).linkedCore == core;
    }

    private static boolean isSiliconArcFurnace(Block block) {
        return block == Blocks.siliconArcFurnace || block == metrum.content.MetrumProduction.arcFurnaceOverclocked;
    }

    static boolean buildSteamForRecovery(CoreBuild core, Item item) {
        if (item != metrum.content.MetrumItems.steel && item != mindustry.content.Items.tungsten) return false;
        Block boiler = metrum.content.MetrumProduction.steamFurnace;
        if (boiler == null || !boiler.environmentBuildable()) return false;
        mindustry.type.Liquid steam = metrum.content.MetrumLiquids.steam;
        if (count(core, b -> b.block == boiler || (b.enabled && b.block.outputsLiquid
            && b.liquids != null && b.liquids.get(steam) > 1f)) > 0) return false;
        if (prioritizeWorkshopMaterials(core, boiler)) return false;
        return queueCoreWorkshop(core, boiler);
    }

    static boolean queueCoreWorkshop(CoreBuild core, Block producer) {
        Tile site = findCoreWorkshopSite(core, producer);
        return site != null && queueConstructionBatch(core, Seq.with(new BuildPlan(site.x, site.y, 0, producer)), true);
    }

    private static Tile findCoreWorkshopSite(CoreBuild core, Block producer) {
        mindustry.type.Liquid steam = metrum.content.MetrumLiquids.steam;
        boolean needsSteam = steam != null && producer.consumesLiquid(steam);
        boolean scanCoreEdge = needsSteam || isSiliconArcFurnace(producer);
        int coreMin = -(core.block.size - 1) / 2, coreMax = core.block.size / 2;
        int factoryMin = -(producer.size - 1) / 2, factoryMax = producer.size / 2;
        int offsetMin = scanCoreEdge ? coreMin - factoryMax : 0;
        int offsetMax = scanCoreEdge ? coreMax - factoryMin : 0;
        Tile fallback = null;
        for (int dir = 0; dir < 4; dir++) {
            int distance = (dir == 0 || dir == 1) ? coreMax - factoryMin + 1 : factoryMax - coreMin + 1;
            int dx = arc.math.geom.Geometry.d4x(dir), dy = arc.math.geom.Geometry.d4y(dir);
            for (int offset = offsetMin; offset <= offsetMax; offset++) {
                Tile site = Vars.world.tile(core.tileX() + dx * distance - dy * offset,
                    core.tileY() + dy * distance + dx * offset);
                if (site == null || !Build.validPlace(producer, core.team, site.x, site.y, 0, false)) continue;
                if (fallback == null) fallback = site;
                if (!needsSteam) {
                    if (offset == 0) return site;
                    continue;
                }
                for (arc.math.geom.Point2 edge : mindustry.world.Edges.getEdges(producer.size)) {
                    Tile adjacent = Vars.world.tile(site.x + edge.x, site.y + edge.y);
                    if (adjacent != null && adjacent.build != null && adjacent.build.team == core.team
                        && isSteamSource(adjacent.build)) {
                        return site;
                    }
                }
            }
        }
        return fallback == null && isSiliconArcFurnace(producer) ? findLinkedWorkshopSite(core, producer) : fallback;
    }

    private static Tile findLinkedWorkshopSite(CoreBuild core, Block producer) {
        int factoryMin = -(producer.size - 1) / 2, factoryMax = producer.size / 2;
        for (Building storage : core.proximity) {
            if (!isLinkedStorage(core, storage)) continue;
            int storageMin = -(storage.block.size - 1) / 2, storageMax = storage.block.size / 2;
            for (int dir = 0; dir < 4; dir++) {
                int distance = dir < 2 ? storageMax - factoryMin + 1 : factoryMax - storageMin + 1;
                int dx = arc.math.geom.Geometry.d4x(dir), dy = arc.math.geom.Geometry.d4y(dir);
                for (int offset = storageMin - factoryMax; offset <= storageMax - factoryMin; offset++) {
                    Tile site = Vars.world.tile(storage.tileX() + dx * distance - dy * offset,
                        storage.tileY() + dy * distance + dx * offset);
                    if (site != null && Build.validPlace(producer, core.team, site.x, site.y, 0, false)) return site;
                }
            }
        }
        return null;
    }

    private static void prioritizeMissingWorkshop(CoreBuild core, Block workshop) {
        if (workshop == null || !workshop.environmentBuildable() || count(core, b -> b.block == workshop) > 0) return;
        prioritizeWorkshopMaterials(core, workshop);
    }

    private static boolean prioritizeWorkshopMaterials(CoreBuild core, Block workshop) {
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        for (ItemStack cost : workshop.requirements) {
            int needed = Math.round(cost.amount * Vars.state.rules.buildCostMultiplier)
                + reserve.getOrDefault(cost.item, 0);
            if (core.items.get(cost.item) < needed && prioritizeProducerDependency(core, cost.item, needed)) return true;
        }
        return false;
    }

    static boolean prioritizeProducerDependency(CoreBuild core, Item item, int needed) {
        ItemStack parent = miningDemand.get(core);
        if (parent == null || parent.item == item) return false;
        // Don't start graphite decompression without a way to get its graphite first.
        if (parent.item == mindustry.content.Items.coal && item == mindustry.content.Items.graphite
            && !Vars.indexer.hasOre(item) && !Vars.indexer.hasWallOre(item) && findProducer(core, item) == null) return false;
        java.util.Deque<ItemStack> pending = deferredDemand.computeIfAbsent(core, ignored -> new java.util.ArrayDeque<>());
        if (pending.stream().anyMatch(goal -> goal.item == item)) return false;
        pending.push(parent);
        miningDemand.put(core, new ItemStack(item, needed));
        return true;
    }

    static Block findProducer(CoreBuild core, Item item) {
        return findProducer(core, item, false);
    }

    static boolean producesItem(Block block, Item item) {
        ItemStack[] outputs;
        if (block instanceof mindustry.world.blocks.production.Separator) {
            outputs = ((mindustry.world.blocks.production.Separator) block).results;
        } else if (block instanceof mindustry.world.blocks.production.GenericCrafter) {
            mindustry.world.blocks.production.GenericCrafter crafter = (mindustry.world.blocks.production.GenericCrafter) block;
            if (crafter.outputItem != null && crafter.outputItem.item == item) return true;
            outputs = crafter.outputItems;
        } else return false;
        if (outputs != null) for (ItemStack output : outputs) {
            if (output.item == item && output.amount > 0) return true;
        }
        return false;
    }

    static Block findProducer(CoreBuild core, Item item, boolean allowMissingInputs) {
        return findProducer(core, item, allowMissingInputs, block -> true);
    }

    private static Block findProducer(CoreBuild core, Item item, boolean allowMissingInputs, arc.func.Boolf<Block> usable) {
        Block best = null;
        int bestPriority = -1;
        long bestCost = Long.MAX_VALUE;
        java.util.Map<Item, Integer> reserve = economicReserve(core);
        candidates: for (Block block : Vars.content.blocks()) {
            if (!producesItem(block, item) || !block.environmentBuildable()
                || block instanceof mindustry.world.blocks.production.HeatCrafter) continue;
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
                    if ((liquid != mindustry.content.Liquids.water && liquid != metrum.content.MetrumLiquids.steam) || count(core,
                        b -> b.block.outputsLiquid && b.liquids != null && b.liquids.get(liquid) > 1f) == 0) continue candidates;
                } else continue candidates;
            }
            for (java.util.Map.Entry<Item, Integer> need : required.entrySet()) {
                if (core.items.get(need.getKey()) - reserve.getOrDefault(need.getKey(), 0) < need.getValue()) continue candidates;
            }
            if (!usable.get(block)) continue;
            int priority = 0;
            if (item == mindustry.content.Items.silicon) {
                if (block == metrum.content.MetrumProduction.arcFurnaceOverclocked) priority = 2;
                else if (block == Blocks.siliconArcFurnace) priority = 1;
            }
            if (priority > bestPriority || (priority == bestPriority && totalCost < bestCost)) {
                best = block;
                bestPriority = priority;
                bestCost = totalCost;
            }
        }
        return best;
    }    static float thermalWaterRate(Building source) {
        if (!source.enabled || !(source instanceof mindustry.world.blocks.power.ThermalGenerator.ThermalGeneratorBuild)) return 0f;
        mindustry.world.blocks.power.ThermalGenerator generator = (mindustry.world.blocks.power.ThermalGenerator) source.block;
        if (generator.outputLiquid == null || generator.outputLiquid.liquid != mindustry.content.Liquids.water) return 0f;
        return generator.outputLiquid.amount * ((mindustry.world.blocks.power.ThermalGenerator.ThermalGeneratorBuild) source).productionEfficiency
            * source.timeScale();
    }

    static float requiredWaterRate(Building consumer) {
        if (!consumer.enabled) return 0f;
        float result = 0f;
        for (mindustry.world.consumers.Consume consume : consumer.block.nonOptionalConsumers) {
            if (consume instanceof mindustry.world.consumers.ConsumeLiquid
                && ((mindustry.world.consumers.ConsumeLiquid) consume).liquid == mindustry.content.Liquids.water)
                result += ((mindustry.world.consumers.ConsumeLiquid) consume).amount * consumer.timeScale();
        }
        return result;
    }

    static boolean hasWaterDeficit(CoreBuild core) {
        float[] rates = {0f, 0f};
        Vars.indexer.eachBlock(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.liquids != null, b -> { rates[0] += waterProductionRate(b); rates[1] += requiredWaterRate(b); });
        return rates[1] > rates[0] + 0.001f;
    }

    private static float waterProductionRate(Building source) {
        if (!(source instanceof mindustry.world.blocks.production.Pump.PumpBuild)) return thermalWaterRate(source);
        mindustry.world.blocks.production.Pump.PumpBuild pump = (mindustry.world.blocks.production.Pump.PumpBuild) source;
        if (!pump.enabled || pump.liquidDrop != mindustry.content.Liquids.water) return 0f;
        mindustry.world.blocks.production.Pump block = (mindustry.world.blocks.production.Pump) pump.block;
        return pump.amount * block.pumpAmount * pump.efficiency * pump.timeScale();
    }

    private static boolean needsRecoveryWater(CoreBuild core) {
        return count(core, b -> b.enabled && b.closestCore() == core
            && (b.block == metrum.content.MetrumProduction.steamFurnace
                || b.block == metrum.content.MetrumProduction.stoneCrusher)
            && b.liquids != null && b.liquids.get(mindustry.content.Liquids.water) < 1f) > 0;
    }
    static boolean canSupplyWater(Building source) {
        if (!source.enabled || !source.block.outputsLiquid || source.liquids == null) return false;
        mindustry.type.Liquid water = mindustry.content.Liquids.water;
        boolean stocked = source.liquids.get(water) > 1f;
        if (source.block instanceof mindustry.world.blocks.power.ThermalGenerator) {
            mindustry.type.LiquidStack output = ((mindustry.world.blocks.power.ThermalGenerator) source.block).outputLiquid;
            return output != null && output.liquid == water && output.amount > 0f
                && (stocked || source.efficiency > 0f);
        }
        if (source instanceof mindustry.world.blocks.production.Pump.PumpBuild) {
            return stocked && ((mindustry.world.blocks.production.Pump.PumpBuild) source).liquidDrop == water;
        }
        if (source.block instanceof mindustry.world.blocks.production.GenericCrafter) {
            mindustry.world.blocks.production.GenericCrafter crafter =
                (mindustry.world.blocks.production.GenericCrafter) source.block;
            if (!stocked) return false;
            if (crafter.outputLiquid != null && crafter.outputLiquid.liquid == water) return true;
            if (crafter.outputLiquids != null) for (mindustry.type.LiquidStack output : crafter.outputLiquids) {
                if (output.liquid == water) return true;
            }
            return false;
        }
        return stocked && !source.block.consumesLiquid(water);
    }
    static boolean canSupportDrill(CoreBuild core, Block drill) {
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
            if (count(core, b -> canSupplyWater(b)) == 0) return false;
        }
        return true;
    }
    static Tile findFloorOreSpot(CoreBuild core, mindustry.world.blocks.production.Drill drill, int radius, arc.struct.IntSet rejected) {
        Tile best = null;
        ItemStack demand = miningDemand.get(core);
        boolean bestMeetsDemand = false;
        int least = Integer.MAX_VALUE;
        float nearest = Float.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
            Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
            if (t == null || t.block() != Blocks.air || rejected.contains(t.pos()) || !drill.canMine(t)
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
    static Tile findOreSpot(CoreBuild core, Block drill, int radius, arc.struct.IntSet rejected) {
        java.util.Set<Item> mined = minedResources(core);
        Tile best = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestAmount = Integer.MAX_VALUE;
        float bestDst = Float.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                Tile t = Vars.world.tile(core.tileX() + dx, core.tileY() + dy);
                if (t == null || t.block() != Blocks.air || rejected.contains(t.pos())) continue;
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
