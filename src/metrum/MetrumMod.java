package metrum;

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
    }

    @Override
    public void init() {
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
        FuelSystem.init();
        BoostSystem.init();
        BaseExpander.planets.add(MetrumPlanets.ferrox);
        BaseExpander.init();
    }
}
