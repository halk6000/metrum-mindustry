package metrum;

import arc.util.Log;
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
        // Metrum MonoMets all come from one dock, so set their tiers explicitly.
        FuelSystem.setTier(MetrumUnits.monoMetT1, 1);
        FuelSystem.setTier(MetrumUnits.monoMetT2, 2);
        FuelSystem.setTier(MetrumUnits.monoMetT3, 3);
        FuelSystem.setTier(MetrumUnits.monoMetT1b, 2);
        FuelSystem.setTier(MetrumUnits.monoMetT2b, 3);
        FuelSystem.setTier(MetrumUnits.monoMetT3b, 4);
        FuelSystem.setTier(MetrumUnits.boosterDrone, 3);
        FuelSystem.init();
        BoostSystem.init();
    }
}
