package metrum.content;

import arc.struct.Seq;
import metrum.fuel.FuelStation;
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
    public static Block airMinerDock, airMinerRebuilder, boosterRebuilder;
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
    
        // B -> C: booster variants
        boosterRebuilder = new Reconstructor("booster-rebuilder") {{
            requirements(Category.units, with(MetrumItems.steel, 120, Items.silicon, 100, Items.tungsten, 80, MetrumItems.berylliumBronze, 40));
            size = 3;
            health = 1500;
            regionSuffix = "-dark";
            constructTime = 1200f;
            consumePower(3f);
            consumeItems(with(MetrumItems.steel, 10, MetrumItems.berylliumBronze, 4));
            consumeLiquid(MetrumLiquids.steam, 0.15f);
            upgrades.addAll(
                new UnitType[]{MetrumUnits.monoMetT1b, MetrumUnits.monoMetT1c},
                new UnitType[]{MetrumUnits.monoMetT2b, MetrumUnits.monoMetT2c},
                new UnitType[]{MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c});
        }};

        fuelStation = new FuelStation("fuel-station");
        fuelStation.requirements(Category.units, with(MetrumItems.steel, 60, Items.silicon, 50, Items.graphite, 40, Items.beryllium, 80));
        fuelStation.fuel(Liquids.slag, 1).fuel(MetrumLiquids.magma, 2).fuel(MetrumLiquids.plasma, 3);
    }
}
