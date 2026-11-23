package metrum;

import arc.struct.Seq;
import arc.util.Log;
import metrum.ai.BaseExpander;
import metrum.boost.BoostSystem;
import metrum.fuel.FuelSystem;
import metrum.content.MetrumBlocks;
import metrum.content.MetrumItems;
import metrum.content.MetrumPower;
import metrum.content.MetrumProduction;
import metrum.content.MetrumPhysics;
import metrum.content.MetrumPlanets;
import metrum.content.MetrumTechTree;
import metrum.content.MetrumUnitBlocks;
import metrum.content.MetrumUnits;
import metrum.content.MetrumLiquids;
import mindustry.mod.Mod;

public class MetrumMod extends Mod {
    @Override
    public void loadContent() {
        FuelSystem.load();
        BoostSystem.load();
        metrum.modular.Loadouts.load();
        MetrumItems.load();
        MetrumLiquids.load();
        MetrumBlocks.load();
        MetrumProduction.load();
        MetrumPower.load();
        MetrumUnits.load();
        metrum.modular.MetrumModular.load();
        MetrumUnitBlocks.load();
        MetrumPhysics.load();
        MetrumPower.loadReactors();
        MetrumTechTree.load();
        MetrumPlanets.load();
        Log.info("[Metrum] content loaded");
        mark("content loaded");
    }

    @Override
    public void init() {
        mark("init start");
        try {
            initInner();
            mark("init ok");
        } catch (Throwable t) {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            mark("init FAILED " + System.lineSeparator() + sw);
            Log.err("[Metrum] init failed", t);
        }
    }

    /** Appends a line to saves/metrum-trace.txt (plain java.io, works even if Vars is half-ready). */
    static void mark(String what) {
        try {
            arc.files.Fi f = mindustry.Vars.dataDirectory == null ? arc.Core.files.local("metrum-trace.txt") : mindustry.Vars.dataDirectory.child("metrum-trace.txt");
            f.writeString(new java.util.Date() + " " + what + System.lineSeparator(), true);
        } catch (Throwable ignored) {
        }
    }

    void initInner() {
        // Factories/reconstructors only take units that are researched (unlockedNow / unlockedNowHost).
        // The reconstructor block itself is the research gate, so upgraded variants are always unlocked.
        for (mindustry.type.UnitType t : new mindustry.type.UnitType[]{
            MetrumUnits.monoMetT1, MetrumUnits.monoMetT2, MetrumUnits.monoMetT3,
            MetrumUnits.monoMetT1b, MetrumUnits.monoMetT2b, MetrumUnits.monoMetT3b,
            MetrumUnits.monoMetT1c, MetrumUnits.monoMetT2c, MetrumUnits.monoMetT3c,
            MetrumUnits.monoMetT1d, MetrumUnits.monoMetT2d, MetrumUnits.monoMetT3d}) {
            t.alwaysUnlocked = true;
        }

        // Metrum tiers: T1-T3, upgrades B / C / D. Fuel tanks start at C.
        // level: 0 = base, 1 = B, 2 = C, 3 = D
        FuelSystem.register(MetrumUnits.monoMetT1, 1, 0);
        FuelSystem.register(MetrumUnits.monoMetT2, 2, 0);
        FuelSystem.register(MetrumUnits.monoMetT3, 3, 0);
        FuelSystem.register(MetrumUnits.monoMetT1b, 1, 1);
        FuelSystem.register(MetrumUnits.monoMetT2b, 2, 1);
        FuelSystem.register(MetrumUnits.monoMetT3b, 3, 1);
        FuelSystem.register(MetrumUnits.monoMetT1c, 1, 2);
        FuelSystem.register(MetrumUnits.monoMetT2c, 2, 2);
        FuelSystem.register(MetrumUnits.monoMetT3c, 3, 2);
        FuelSystem.register(MetrumUnits.monoMetT1d, 1, 3);
        FuelSystem.register(MetrumUnits.monoMetT2d, 2, 3);
        FuelSystem.register(MetrumUnits.monoMetT3d, 3, 3);
        // modular drones: light/medium/heavy hulls use the T1/T2/T3 tanks (C-level and above)
        FuelSystem.register(metrum.modular.MetrumModular.droneLight, 1, 2);
        FuelSystem.register(metrum.modular.MetrumModular.droneMedium, 2, 2);
        FuelSystem.register(metrum.modular.MetrumModular.droneHeavy, 3, 2);
        FuelSystem.init();
        BoostSystem.init();
        BaseExpander.planets.add(MetrumPlanets.ferrox);
        BaseExpander.init();
        metrum.ai.planner.BasePlanner.planets.add(MetrumPlanets.ferrox);
        metrum.ai.planner.BasePlanner.init();
        // Settings -> Metrum: enemy AI level (separate AI mode)
        if (!mindustry.Vars.headless) {
            mindustry.Vars.ui.settings.addCategory(arc.Core.bundle.get("metrum.settings"), mindustry.gen.Icon.terrain, t ->
                t.sliderPref(metrum.ai.planner.AiLevel.setting, 2, 0, metrum.ai.planner.AiLevel.values().length - 1, 1,
                    i -> metrum.ai.planner.AiLevel.values()[i].localized()));
        }
        metrum.modular.Loadouts.init();
        metrum.ai.SmartMining.init(MetrumUnits.monoMetT1, MetrumUnits.monoMetT2, MetrumUnits.monoMetT3,
            MetrumUnits.monoMetT1b, MetrumUnits.monoMetT2b, MetrumUnits.monoMetT3b,
            MetrumUnits.monoMetT1c, MetrumUnits.monoMetT2c, MetrumUnits.monoMetT3c,
            MetrumUnits.monoMetT1d, MetrumUnits.monoMetT2d, MetrumUnits.monoMetT3d);
        // "take care" command: rebuild + repair
        metrum.ai.CaretakerAI.init(MetrumUnits.monoMetT1, MetrumUnits.monoMetT2, MetrumUnits.monoMetT3,
            MetrumUnits.monoMetT1b, MetrumUnits.monoMetT2b, MetrumUnits.monoMetT3b,
            MetrumUnits.monoMetT1c, MetrumUnits.monoMetT2c, MetrumUnits.monoMetT3c,
            MetrumUnits.monoMetT1d, MetrumUnits.monoMetT2d, MetrumUnits.monoMetT3d);
        // mining grows with each upgrade (B: +graphite, C: +tungsten, D: +thorium) and freshly built drones
        // start in smart mining "least stored" mode
        mindustry.type.UnitType[][] lines = {
            {MetrumUnits.monoMetT1, MetrumUnits.monoMetT1b, MetrumUnits.monoMetT1c, MetrumUnits.monoMetT1d},
            {MetrumUnits.monoMetT2, MetrumUnits.monoMetT2b, MetrumUnits.monoMetT2c, MetrumUnits.monoMetT2d},
            {MetrumUnits.monoMetT3, MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c, MetrumUnits.monoMetT3d}};
        mindustry.type.Item[] extra = {null, mindustry.content.Items.graphite, mindustry.content.Items.tungsten, mindustry.content.Items.thorium};
        for (mindustry.type.UnitType[] line : lines) {
            for (int lvl = 0; lvl < line.length; lvl++) {
                mindustry.type.UnitType t = line[lvl];
                t.mineItems = new arc.struct.Seq<>(t.mineItems); // C/D shared the list with B - give each its own
                if (t.mineTier < 0) t.mineTier = 3; // C/D boosters can mine again
                t.mineTier = Math.max(t.mineTier, 3 + Math.min(lvl, 2));
                for (int k = 1; k <= lvl; k++) if (!t.mineItems.contains(extra[k])) t.mineItems.add(extra[k]);
                if (!t.mineItems.contains(MetrumItems.iron)) t.mineItems.add(MetrumItems.iron);
                t.defaultCommand = metrum.ai.SmartMining.command;
            }
        }
        Log.info("[Metrum] init done");
        // ClientLoadEvent can fire before mod init on some builds; post to the next frame instead
        arc.Core.app.post(() -> {
            try {
                writeLoadReport();
                SelfTest.runIfRequested();
                WelcomeDialog.showIfNew();
            } catch (Throwable t) {
                Log.err("[Metrum] load report failed", t);
            }
        });
    }

    /** Writes saves/metrum-load.txt: content counts, missing sprites, content errors. Used for testing. */
    static void writeLoadReport() {
        StringBuilder sb = new StringBuilder("Metrum load report\n");
        int count = 0, missing = 0, errors = 0;
        for (Seq<mindustry.ctype.Content> list : mindustry.Vars.content.getContentMap()) {
            for (mindustry.ctype.Content c : list) {
                if (c.minfo.mod == null || !"metrum".equals(c.minfo.mod.name)) continue;
                count++;
                if (c.hasErrored()) {
                    errors++;
                    sb.append("ERROR ").append(c).append(": ").append(c.minfo.error).append("\n");
                }
                if (c instanceof mindustry.ctype.UnlockableContent && !(c instanceof mindustry.type.Planet)) {
                    mindustry.ctype.UnlockableContent u = (mindustry.ctype.UnlockableContent) c;
                    if (u.uiIcon == null || !arc.Core.atlas.isFound(u.uiIcon)) {
                        missing++;
                        sb.append("NO SPRITE ").append(u.name).append("\n");
                    }
                }
            }
        }
        // reconstructor sanity: every upgrade target must be unlocked and not banned, or the block refuses the unit
        for (mindustry.world.Block b : mindustry.Vars.content.blocks()) {
            if (!(b instanceof mindustry.world.blocks.units.Reconstructor) || b.minfo.mod == null) continue;
            for (mindustry.type.UnitType[] up : ((mindustry.world.blocks.units.Reconstructor) b).upgrades) {
                if (!up[1].unlockedNowHost() || up[1].isBanned()) {
                    sb.append("UPGRADE BLOCKED ").append(b.name).append(": ").append(up[0].name).append(" -> ").append(up[1].name)
                        .append(" unlocked=").append(up[1].unlockedNowHost()).append(" banned=").append(up[1].isBanned()).append(System.lineSeparator());
                    errors++;
                }
            }
        }
        sb.append("balance fuel.tank.t1=").append(Balance.f("fuel.tank.t1", -1f)).append(" war.costGrowth=").append(Balance.f("war.costGrowth", -1f)).append(System.lineSeparator());
        sb.append("hardness:");
        for (mindustry.type.Item it : mindustry.Vars.content.items()) if (it.hardness > 0) sb.append(' ').append(it.name).append('=').append(it.hardness);
        sb.append(System.lineSeparator());
        sb.append("drill tiers:");
        for (mindustry.world.Block b : mindustry.Vars.content.blocks()) if (b instanceof mindustry.world.blocks.production.BeamDrill) sb.append(' ').append(b.name).append('=').append(((mindustry.world.blocks.production.BeamDrill) b).tier);
        sb.append(System.lineSeparator());
        sb.append("content=").append(count).append(" missingSprites=").append(missing).append(" errors=").append(errors).append("\n");
        mindustry.Vars.dataDirectory.child("metrum-load.txt").writeString(sb.toString());
        Log.info("[Metrum] " + sb.toString().trim().replace("\n", " | "));
    }
}
