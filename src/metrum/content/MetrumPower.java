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
