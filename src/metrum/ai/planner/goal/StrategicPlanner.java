package metrum.ai.planner.goal;

import arc.Core;
import mindustry.Vars;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import metrum.ai.planner.AiLevel;
import metrum.ai.planner.BasePlanner;
import metrum.ai.planner.task.*;

/** Save-backed strategic sequence. A task may enqueue work, but only its readiness test advances a goal. */
public final class StrategicPlanner {
    private StrategicPlanner() {}
    private static String key(CoreBuild core, String suffix) {
        return "metrum-strategy-v1-" + core.team.id + "-" + core.pos() + "-" + suffix;
    }
    public static boolean polish() { return Core.bundle != null && "pl".equals(Core.bundle.getLocale().getLanguage()); }
    public static String text(String pl, String en) { return polish() ? pl : en; }
    public static boolean flag(CoreBuild core, String name) {
        return "1".equals(Vars.state.rules.tags.get(key(core, name), "0"));
    }
    public static void flag(CoreBuild core, String name, boolean on) {
        Vars.state.rules.tags.put(key(core, name), on ? "1" : "0");
    }
    public static DevelopmentGoal goal(CoreBuild core) {
        try { return DevelopmentGoal.valueOf(Vars.state.rules.tags.get(key(core, "goal"), "ECONOMY")); }
        catch (IllegalArgumentException ignored) { return DevelopmentGoal.ECONOMY; }
    }
    private static void set(CoreBuild core, DevelopmentGoal goal) {
        Vars.state.rules.tags.put(key(core, "goal"), goal.name());
    }
    public static void coreAttacked(CoreBuild core) {
        if (!Vars.net.client() && core.team == Vars.state.rules.waveTeam) flag(core, "emergency", true);
    }
    public static int turretTarget(CoreBuild core, DevelopmentGoal goal) {
        return goal.turretLimit(CoreUpgradeTask.tier(core)) + (flag(core, "emergency") ? 1 : 0);
    }
    public static String status(CoreBuild core) {
        DevelopmentGoal goal = goal(core);
        return text("Etap ", "Stage ") + Math.min(7, goal.ordinal() + 1) + "/7 — " + goal.label(polish())
            + (flag(core, "emergency") ? text(" | alarm +1 wieża", " | alarm +1 turret") : "");
    }
    /** Called at the same cadence as the existing planner. No independent competing builder. */
    public static boolean step(CoreBuild core, AiLevel level) {
        DevelopmentGoal stage = goal(core);
        int tier = CoreUpgradeTask.tier(core);
        String savedTier = Vars.state.rules.tags.get(key(core, "tier"), Integer.toString(tier));
        Vars.state.rules.tags.put(key(core, "tier"), Integer.toString(tier));
        if (!savedTier.equals(Integer.toString(tier))) {
            stage = DevelopmentGoal.ECONOMY;
            set(core, stage);
            flag(core, "unit-produced", false);
            flag(core, "silicon-produced", false);
        }
        // An emergency is a bounded exception, not a second infinite turret queue.
        if (flag(core, "emergency") && DefenceTask.count(core) < turretTarget(core, stage)
                && DefenceTask.emergency(core, level)) return true;
        // Repair critical services before expansion, without discarding the saved goal or material stack.
        if (stage != DevelopmentGoal.ECONOMY && !EconomyTask.infrastructureReady(core)) {
            if (EconomyTask.infrastructure(core, level)) return true;
            if (BasePlanner.hasDemand(core)) return BasePlanner.recoverEconomy(core, level);
            return BasePlanner.stalled(text("odbudowa podstawowej gospodarki", "restore basic economy"));
        }
        // Production and transport tasks requested by the current goal share the existing dependency stack.
        if (BasePlanner.hasDemand(core)) return BasePlanner.recoverEconomy(core, level);
        if (stage.ordinal()>DevelopmentGoal.FIRST_TURRET.ordinal() && !DefenceTask.ready(core,turretTarget(core,DevelopmentGoal.FIRST_TURRET)))
            return DefenceTask.turrets(core,level,turretTarget(core,DevelopmentGoal.FIRST_TURRET));
        for (int i = 0; i < DevelopmentGoal.values().length; i++) {
            switch (stage) {
                case ECONOMY:
                    if (!EconomyTask.infrastructureReady(core)) return EconomyTask.infrastructure(core, level);
                    break;
                case SILICON:
                    if (!EconomyTask.siliconReady(core)) return EconomyTask.silicon(core, level);
                    break;
                case FIRST_TURRET:
                    if (!DefenceTask.ready(core, turretTarget(core, stage)))
                        return DefenceTask.turrets(core, level, turretTarget(core, stage));
                    break;
                case UNIT_FACTORY:
                    if (!UnitProductionTask.ready(core)) return UnitProductionTask.step(core, level);
                    break;
                case OPTIMIZE:
                    if (!EconomyTask.optimized(core)) return EconomyTask.optimize(core, level);
                    break;
                case DEFENCE:
                    if (!DefenceTask.ready(core, turretTarget(core, stage)))
                        return DefenceTask.turrets(core, level, turretTarget(core, stage));
                    if (!DefenceTask.wallsReady(core)) return DefenceTask.walls(core);
                    break;
                case CORE_UPGRADE:
                    if (!DefenceTask.ready(core, turretTarget(core, stage)))
                        return DefenceTask.turrets(core, level, turretTarget(core, stage));
                    if (!DefenceTask.wallsReady(core)) return DefenceTask.walls(core);
                    if (CoreUpgradeTask.next(core) != null) {
                        if (CoreUpgradeTask.step(core)) return true;
                        // While a legal upgrade is blocked by capacity or space, continue useful production.
                        if (!BasePlanner.hasDemand(core)) return UnitProductionTask.step(core, level);
                        return false;
                    }
                    break;
                case DEVELOPED:
                    if (!DefenceTask.ready(core, turretTarget(core, stage)))
                        return DefenceTask.turrets(core, level, turretTarget(core, stage));
                    if (!DefenceTask.wallsReady(core)) return DefenceTask.walls(core);
                    return UnitProductionTask.step(core, level);
                default: return false;
            }
            stage = stage.next();
            set(core, stage);
        }
        return false;
    }
}
