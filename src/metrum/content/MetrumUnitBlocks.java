package metrum.content;

import arc.struct.Seq;
import metrum.fuel.FuelStation;
import metrum.world.WarReconstructor;
import mindustry.content.Liquids;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.type.ItemStack.with;

/** Drone factory and reconstructor ported from the original Metrum JSON mod. */
public class MetrumUnitBlocks {
    public static Block airMinerDock, airMinerRebuilder;
    public static WarReconstructor warReconstructor;
    public static FuelStation fuelStation;

    public static void load() {
        airMinerDock = new UnitFactory("air-miner-dock") {{
            requirements(Category.units, with(Items.beryllium, 250, Items.graphite, 80, Items.silicon, 20));
            size = 3;
            health = 300;
            regionSuffix = "-dark";
            consumePower(0.3f);
            plans = Seq.with(
                new UnitPlan(MetrumUnits.monoMetT1, 200f, with(Items.beryllium, 15)),
                new UnitPlan(MetrumUnits.monoMetT2, 800f, with(Items.graphite, 20, Items.beryllium, 80)),
                new UnitPlan(MetrumUnits.monoMetT3, 1000f, with(Items.beryllium, 100, Items.silicon, 30, Items.graphite, 60)));
        }};

        airMinerRebuilder = new Reconstructor("air-miner-rebuilder") {{
            requirements(Category.units, with(Items.beryllium, 60, MetrumItems.iron, 20, Items.silicon, 20));
            size = 3;
            health = 1200;
            regionSuffix = "-dark";
            constructTime = 800f;
            consumePower(2f);
            consumeItems(with(MetrumItems.iron, 8));
            consumeLiquid(MetrumLiquids.steam, 0.1f);
            upgrades.addAll(
                new UnitType[]{MetrumUnits.monoMetT1, MetrumUnits.monoMetT1b},
                new UnitType[]{MetrumUnits.monoMetT2, MetrumUnits.monoMetT2b},
                new UnitType[]{MetrumUnits.monoMetT3, MetrumUnits.monoMetT3b});
        }};
    
        // The final reconstructor: B -> C and C -> D, cost x1.5 per level
        warReconstructor = new WarReconstructor("war-reconstructor");
        warReconstructor.requirements(Category.units, with(MetrumItems.steel, 120, Items.silicon, 100, Items.tungsten, 80, MetrumItems.berylliumBronze, 40));
        warReconstructor.size = 3;
        warReconstructor.health = 1500;
        warReconstructor.regionSuffix = "-dark";
        warReconstructor.constructTime = 1200f;
        warReconstructor.consumePower(3f);
        warReconstructor.baseItems = with(MetrumItems.ammoCrate, 1, MetrumItems.steel, 2);
        warReconstructor.liquid = MetrumLiquids.steam;
        warReconstructor.baseLiquidPerSecond = 0.09f;
        warReconstructor.costGrowth = 1.5f;
        warReconstructor
            .upgrade(MetrumUnits.monoMetT1b, MetrumUnits.monoMetT1c, 1)
            .upgrade(MetrumUnits.monoMetT2b, MetrumUnits.monoMetT2c, 1)
            .upgrade(MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c, 1)
            .upgrade(MetrumUnits.monoMetT1c, MetrumUnits.monoMetT1d, 2)
            .upgrade(MetrumUnits.monoMetT2c, MetrumUnits.monoMetT2d, 2)
            .upgrade(MetrumUnits.monoMetT3c, MetrumUnits.monoMetT3d, 2);
        warReconstructor.setupConsumers();

        fuelStation = new FuelStation("fuel-station");
        fuelStation.requirements(Category.units, with(MetrumItems.steel, 60, Items.silicon, 50, Items.graphite, 40, Items.beryllium, 80));
        fuelStation.fuel(Liquids.slag, 1).fuel(MetrumLiquids.magma, 2).fuel(MetrumLiquids.plasma, 3);
    }
}
