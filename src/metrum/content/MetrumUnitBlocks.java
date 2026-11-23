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
    public static metrum.world.DropPodLauncher dropPodLauncher;
    public static metrum.world.HullPrinter hullPrinter, partPrinter, weaponPrinter;
    public static Block partsMagazine, weaponsMagazine;
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
        warReconstructor.constructTime = metrum.Balance.f("war.constructSeconds", 20f) * 60f;
        warReconstructor.consumePower(3f);
        warReconstructor.baseItems = with(MetrumItems.ammoCrate, metrum.Balance.i("war.crates", 1), MetrumItems.steel, metrum.Balance.i("war.steel", 2));
        warReconstructor.liquid = MetrumLiquids.steam;
        warReconstructor.baseLiquidPerSecond = metrum.Balance.f("war.steamPerSecond", 0.09f);
        warReconstructor.costGrowth = metrum.Balance.f("war.costGrowth", 1.5f);
        warReconstructor
            .upgrade(MetrumUnits.monoMetT1b, MetrumUnits.monoMetT1c, 1)
            .upgrade(MetrumUnits.monoMetT2b, MetrumUnits.monoMetT2c, 1)
            .upgrade(MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c, 1)
            .upgrade(MetrumUnits.monoMetT1c, MetrumUnits.monoMetT1d, 2)
            .upgrade(MetrumUnits.monoMetT2c, MetrumUnits.monoMetT2d, 2)
            .upgrade(MetrumUnits.monoMetT3c, MetrumUnits.monoMetT3d, 2);
        warReconstructor.setupConsumers();

        // modular drones, step 1: hulls
        hullPrinter = new metrum.world.HullPrinter("hull-printer");
        hullPrinter.requirements(Category.units, with(MetrumItems.steel, 80, Items.silicon, 80, Items.graphite, 60, Items.beryllium, 100));
        hullPrinter.size = 3;
        hullPrinter.consumePower(1.5f);
        hullPrinter.craftTime = metrum.Balance.f("hull.printSeconds", 20f) * 60f;
        hullPrinter
            .hull(MetrumItems.hullLight, with(MetrumItems.iron, 20, Items.silicon, 10))
            .hull(MetrumItems.hullMedium, with(MetrumItems.steel, 25, Items.silicon, 20, Items.beryllium, 20))
            .hull(MetrumItems.hullHeavy, with(MetrumItems.steel, 40, Items.tungsten, 25, MetrumItems.berylliumBronze, 10));

        // modular drones, step 2: parts + their storage
        partPrinter = new metrum.world.HullPrinter("part-printer");
        partPrinter.requirements(Category.units, with(MetrumItems.steel, 60, Items.silicon, 80, Items.graphite, 60));
        partPrinter.size = 3;
        partPrinter.consumePower(1.2f);
        partPrinter.craftTime = metrum.Balance.f("part.printSeconds", 12f) * 60f;
        partPrinter
            .hull(MetrumItems.partEngine, with(MetrumItems.steel, 10, Items.silicon, 10, Items.graphite, 5))
            .hull(MetrumItems.partWings, with(MetrumItems.iron, 15, Items.silicon, 5))
            .hull(MetrumItems.partLegs, with(MetrumItems.steel, 12, MetrumItems.iron, 8));

        partsMagazine = new metrum.world.FilteredStorage("parts-magazine",
            MetrumItems.hullLight, MetrumItems.hullMedium, MetrumItems.hullHeavy,
            MetrumItems.partEngine, MetrumItems.partWings, MetrumItems.partLegs);
        partsMagazine.requirements(Category.effect, with(MetrumItems.steel, 60, Items.beryllium, 60));
        partsMagazine.size = 2;
        partsMagazine.itemCapacity = 100;

        // modular drones, step 3: weapons & modules + their storage
        weaponPrinter = new metrum.world.HullPrinter("weapon-printer");
        weaponPrinter.requirements(Category.units, with(MetrumItems.steel, 80, Items.silicon, 100, Items.tungsten, 40));
        weaponPrinter.size = 3;
        weaponPrinter.consumePower(2f);
        weaponPrinter.craftTime = metrum.Balance.f("weapon.printSeconds", 15f) * 60f;
        weaponPrinter
            .hull(MetrumItems.weaponMiningLaser, with(Items.silicon, 15, Items.graphite, 10, MetrumItems.iron, 5))
            .hull(MetrumItems.weaponMissiles, with(MetrumItems.steel, 10, Items.silicon, 10, MetrumItems.ammoCrate, 2))
            .hull(MetrumItems.weaponExplosives, with(MetrumItems.ammoCrate, 3, Items.graphite, 10))
            .hull(MetrumItems.weaponFuelTank, with(MetrumItems.steel, 15, Items.beryllium, 10))
            .hull(MetrumItems.weaponMachineGun, with(MetrumItems.steel, 12, MetrumItems.iron, 10, MetrumItems.ammoCrate, 1));

        weaponsMagazine = new metrum.world.FilteredStorage("weapons-magazine",
            MetrumItems.weaponMiningLaser, MetrumItems.weaponMissiles, MetrumItems.weaponExplosives,
            MetrumItems.weaponFuelTank, MetrumItems.weaponMachineGun, MetrumItems.ammoCrate);
        weaponsMagazine.requirements(Category.effect, with(MetrumItems.steel, 60, Items.tungsten, 30));
        weaponsMagazine.size = 2;
        weaponsMagazine.itemCapacity = 100;

        // drop pods: send units to the core of another of your sectors (any planet); 1500 power/s for 15 s
        dropPodLauncher = new metrum.world.DropPodLauncher("drop-pod-launcher");
        dropPodLauncher.requirements(Category.units, with(MetrumItems.steel, 150, Items.silicon, 200, Items.tungsten, 80, Items.graphite, 100));
        dropPodLauncher.size = 3;
        dropPodLauncher.launchPowerPerSecond = metrum.Balance.f("pods.powerPerSecond", 1500f);
        dropPodLauncher.launchTime = metrum.Balance.f("pods.launchSeconds", 15f) * 60f;

        fuelStation = new FuelStation("fuel-station");
        fuelStation.requirements(Category.units, with(MetrumItems.steel, 60, Items.silicon, 50, Items.graphite, 40, Items.beryllium, 80));
        fuelStation.fuel(Liquids.slag, 1).fuel(MetrumLiquids.magma, 2).fuel(MetrumLiquids.plasma, 3);
    }
}
