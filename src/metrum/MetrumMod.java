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
        MetrumItems.load();
        MetrumLiquids.load();
        MetrumBlocks.load();
        MetrumProduction.load();
        MetrumPower.load();
        MetrumUnits.load();
        MetrumUnitBlocks.load();
        MetrumPhysics.load();
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
            mark("init FAILED
" + sw);
            Log.err("[Metrum] init failed", t);
        }
    }

    /** Appends a line to saves/metrum-trace.txt (plain java.io, works even if Vars is half-ready). */
    static void mark(String what) {
        try {
            arc.files.Fi f = mindustry.Vars.dataDirectory == null ? arc.Core.files.local("metrum-trace.txt") : mindustry.Vars.dataDirectory.child("metrum-trace.txt");
            f.writeString(new java.util.Date() + " " + what + "
", true);
        } catch (Throwable ignored) {
        }
    }

    void initInner() {
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
        FuelSystem.init();
        BoostSystem.init();
        BaseExpander.planets.add(MetrumPlanets.ferrox);
        BaseExpander.init();
        Log.info("[Metrum] init done");
        // ClientLoadEvent can fire before mod init on some builds; post to the next frame instead
        arc.Core.app.post(() -> {
            try {
                writeLoadReport();
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
                if (c instanceof mindustry.ctype.UnlockableContent) {
                    mindustry.ctype.UnlockableContent u = (mindustry.ctype.UnlockableContent) c;
                    if (u.uiIcon == null || !arc.Core.atlas.isFound(u.uiIcon)) {
                        missing++;
                        sb.append("NO SPRITE ").append(u.name).append("\n");
                    }
                }
            }
        }
        sb.append("content=").append(count).append(" missingSprites=").append(missing).append(" errors=").append(errors).append("\n");
        mindustry.Vars.dataDirectory.child("metrum-load.txt").writeString(sb.toString());
        Log.info("[Metrum] " + sb.toString().trim().replace("\n", " | "));
    }
}
