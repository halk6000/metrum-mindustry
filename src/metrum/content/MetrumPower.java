package metrum.content;

import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.power.ThermalGenerator;
import mindustry.world.draw.*;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockGroup;

import static mindustry.type.ItemStack.with;

/** Power blocks ported from the original Metrum JSON mod. */
public class MetrumPower {
    public static Block turbineAdvanced;
    public static metrum.world.SteamEngine steamEngine;
    public static metrum.world.FusionReactor fusionReactor;
    public static metrum.world.DecayReactor decayReactor;
    public static metrum.world.ReactionControl reactionControl;

    public static void load() {
        turbineAdvanced = new SteamTurbine("turbine-advanced") {{
            requirements(Category.power, with(Items.tungsten, 40, Items.silicon, 160));
            attribute = Attribute.steam;
            group = BlockGroup.liquids;
            size = 3;
            health = 1500;
            powerProduction = 0.77f;
            minEfficiency = 0.2f;
            displayEfficiencyScale = 1f;
            squareSprite = false;
            generateEffect = Fx.turbinegenerate;
            effectChance = 0.04f;
            consumeLiquid(MetrumLiquids.steam, 0.4f).boost();
            outputLiquid = new LiquidStack(Liquids.water, 0.05f);
            drawer = new DrawMulti(new DrawDefault(),
                new DrawBlurSpin("-rotator", 2.7f) {{ blurThresh = 0.01f; }});
        }};
    }

    public static void loadReactors() {
        // steam engine: 600 power/s from steam, heats every neighbour that needs heat
        steamEngine = new metrum.world.SteamEngine("steam-engine");
        steamEngine.requirements(Category.power, with(MetrumItems.iron, 60, Items.graphite, 40, Items.beryllium, 50));
        steamEngine.size = 2;
        steamEngine.powerProduction = metrum.Balance.f("steamEngine.powerPerSecond", 600f) / 60f;
        steamEngine.heatOutput = metrum.Balance.f("steamEngine.heat", 12f);
        steamEngine.consumeLiquid(MetrumLiquids.steam, metrum.Balance.f("steamEngine.steamPerSecond", 12f) / 60f);

        fusionReactor = new metrum.world.FusionReactor("fusion-reactor");
        fusionReactor.requirements(Category.power, with(MetrumItems.tungstenCarbide, 300, MetrumItems.ferrotungsten, 200, Items.surgeAlloy, 200, Items.silicon, 400, MetrumItems.steel, 400));
        fusionReactor.size = 5;
        fusionReactor.health = 4000;
        fusionReactor.powerProduction = metrum.Balance.f("reactor.fusion.powerPerSecond", 9000f) / 60f;
        fusionReactor.fuel = MetrumItems.quarkMatter;
        fusionReactor.coolant = Liquids.water;
        fusionReactor.coolantPerSecond = metrum.Balance.f("reactor.fusion.waterPerSecond", 90f);
        fusionReactor.steam = MetrumLiquids.steam;
        // fusion chain by atomic number: Be(4) C(6) Si(14) Ti(22) Fe(26) Cu(29) W(74) Pb(82) Th(90)
        mindustry.type.Item[] chain = {MetrumItems.atomicBeryllium, MetrumItems.atomicCarbon, MetrumItems.atomicSilicon,
            MetrumItems.atomicTitanium, MetrumItems.atomicIron, MetrumItems.atomicCopper, MetrumItems.atomicTungsten,
            MetrumItems.atomicLead, MetrumItems.atomicThorium};
        for (int i = 0; i < chain.length - 1; i++) fusionReactor.transmute.put(chain[i], chain[i + 1]);

        reactionControl = new metrum.world.ReactionControl("reaction-control");
        reactionControl.requirements(Category.power, with(Items.silicon, 60, MetrumItems.steel, 40, Items.graphite, 30));
        reactionControl.size = 1;
        reactionControl.targets.addAll(chain);

        // safe sibling: steady power, no meltdown, random resources from each decay
        decayReactor = new metrum.world.DecayReactor("decay-reactor");
        decayReactor.requirements(Category.power, with(MetrumItems.tungstenCarbide, 150, MetrumItems.ferrotungsten, 100, Items.silicon, 250, MetrumItems.steel, 250));
        decayReactor.size = 4;
        decayReactor.health = 2500;
        decayReactor.powerProduction = metrum.Balance.f("reactor.decay.powerPerSecond", 3000f) / 60f;
        decayReactor.fuel = MetrumItems.quarkMatter;
        decayReactor.decayTime = metrum.Balance.f("reactor.decay.seconds", 10f) * 60f;
        decayReactor.product(Items.thorium, 4).product(Items.tungsten, 4).product(Items.beryllium, 5).product(Items.graphite, 5)
            .product(Items.oxide, 2).product(Items.carbide, 1).product(Items.surgeAlloy, 1).product(Items.phaseFabric, 1)
            .product(MetrumItems.atomicThorium, 1);
    }

    /** Thermal generator that runs at 75% output when no steam is supplied. */
    public static class SteamTurbine extends ThermalGenerator {
        public float noSteamMultiplier = 0.75f;

        public SteamTurbine(String name) {
            super(name);
        }

        public class SteamTurbineBuild extends ThermalGeneratorBuild {
            @Override
            public float getPowerProduction() {
                return super.getPowerProduction() * (optionalEfficiency > 0f ? 1f : noSteamMultiplier);
            }
        }
    }
}
