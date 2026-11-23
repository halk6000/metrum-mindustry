package metrum.content;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.HeatCrafter;
import mindustry.world.blocks.production.Separator;
import mindustry.world.draw.*;

import static mindustry.type.ItemStack.with;

/** Crafting blocks ported from the original Metrum JSON mod. */
public class MetrumProduction {
    public static Block graphiteDecompressor, graphiteDecompressorOverclocked,
        ammoFactory, steamFurnace, largeSteamFurnace, salvagerOverclocked, salvagerAdvanced, heatInverter, magmaForge, plasmaIonizer, stoneCrusher, infusionSmelter, salvager;

    private static final Color flame = Color.valueOf("fdb380");

    public static void load() {
        graphiteDecompressor = decompressor("graphite-decompressor", 1, 2, 2f,
            with(Items.graphite, 80, Items.silicon, 40, Items.beryllium, 40));
        graphiteDecompressorOverclocked = decompressor("graphite-decompressor-overclocked", 2, 5, 2.5f,
            with(Items.graphite, 20, Items.silicon, 60, Items.beryllium, 40, MetrumItems.iron, 20));

        steamFurnace = new GenericCrafter("steam-furnace") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 40, Items.graphite, 20, Items.silicon, 20));
            size = 2;
            craftTime = 40f;
            itemCapacity = 20;
            liquidCapacity = 100f;
            updateEffect = Fx.smeltsmoke;
            consumeItem(Items.coal);
            consumeLiquid(Liquids.water, 0.2f);
            outputLiquid = new LiquidStack(MetrumLiquids.steam, 0.6f);
        }};

        largeSteamFurnace = new GenericCrafter("large-steam-furnace") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 120, MetrumItems.iron, 60, Items.graphite, 60, Items.silicon, 50));
            size = 3;
            craftTime = 30f;
            itemCapacity = 30;
            liquidCapacity = 300f;
            updateEffect = Fx.smeltsmoke;
            consumePower(0.5f);
            consumeItem(Items.coal);
            consumeLiquid(Liquids.water, 0.5f);
            outputLiquid = new LiquidStack(MetrumLiquids.steam, 2f);
        }};

        // Heat inverter: accepts heat (Erekir heaters) on one side, boils water into steam.
        heatInverter = new HeatCrafter("heat-inverter") {{
            requirements(Category.crafting, with(MetrumItems.iron, 40, Items.graphite, 30, Items.silicon, 30, Items.beryllium, 40));
            size = 2;
            craftTime = 60f;
            liquidCapacity = 120f;
            heatRequirement = 6f;
            maxEfficiency = 3f;
            consumeLiquid(Liquids.water, 0.25f);
            outputLiquid = new LiquidStack(MetrumLiquids.steam, 0.5f);
            updateEffect = Fx.steam;
            updateEffectChance = 0.04f;
        }};

        // 2 iron + 1 steel + 3 beryllium + water -> 2 ammo crates, steam as by-product
        ammoFactory = new GenericCrafter("ammo-factory") {{
            requirements(Category.crafting, with(MetrumItems.steel, 80, MetrumItems.iron, 100, Items.silicon, 60, Items.beryllium, 120));
            size = 3;
            craftTime = metrum.Balance.f("ammo.craftSeconds", 1.5f) * 60f;
            itemCapacity = 30;
            liquidCapacity = 60f;
            consumePower(1.2f);
            consumeItems(with(MetrumItems.iron, 2, MetrumItems.steel, 1, Items.beryllium, 3));
            consumeLiquid(Liquids.water, 0.15f);
            outputItem = new ItemStack(MetrumItems.ammoCrate, 2);
            outputLiquid = new LiquidStack(MetrumLiquids.steam, 0.1f);
            updateEffect = Fx.steam;
            updateEffectChance = 0.03f;
        }};

        // fuel liquids for the fuel station
        magmaForge = new HeatCrafter("magma-forge") {{
            requirements(Category.crafting, with(MetrumItems.steel, 40, Items.tungsten, 30, Items.graphite, 40));
            size = 2;
            craftTime = 40f;
            itemCapacity = 20;
            liquidCapacity = 120f;
            heatRequirement = 10f;
            maxEfficiency = 2f;
            consumeItem(MetrumItems.pieceRock, 2);
            outputLiquid = new LiquidStack(MetrumLiquids.magma, 0.25f);
            updateEffect = Fx.melting;
            updateEffectChance = 0.05f;
        }};

        plasmaIonizer = new GenericCrafter("plasma-ionizer") {{
            requirements(Category.crafting, with(MetrumItems.steel, 100, Items.silicon, 120, Items.surgeAlloy, 40, MetrumItems.ferrotungsten, 30));
            size = 3;
            craftTime = 60f;
            liquidCapacity = 200f;
            consumePower(6f);
            consumeLiquid(MetrumLiquids.magma, 0.2f);
            consumeItem(MetrumItems.atomicCarbon, 1);
            outputLiquid = new LiquidStack(MetrumLiquids.plasma, 0.2f);
            updateEffect = Fx.smeltsmoke;
        }};

        stoneCrusher = new GenericCrafter("stone-crusher") {{
            requirements(Category.crafting, with(Items.beryllium, 120, Items.graphite, 80, Items.silicon, 20));
            size = 2;
            craftTime = 40f;
            itemCapacity = 20;
            liquidCapacity = 40f;
            updateEffect = Fx.smeltsmoke;
            consumePower(0.58f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(Liquids.water, 0.33f);
            outputItem = new ItemStack(Items.sand, 5);
        }};

        infusionSmelter = new GenericCrafter("infusion-smelter") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 60, MetrumItems.iron, 30, Items.silicon, 20));
            size = 2;
            craftTime = 160f;
            itemCapacity = 60;
            liquidCapacity = 100f;
            updateEffect = Fx.smeltsmoke;
            consumePower(6f);
            consumeItems(with(MetrumItems.iron, 2, Items.coal, 1));
            consumeLiquid(MetrumLiquids.steam, 0.33f);
            outputItem = new ItemStack(MetrumItems.steel, 2);
        }};

        // overclocked 2x2: twice as fast as the Salvager; 80% iron / 20% sand (~2 iron per Salvager cycle)
        salvagerOverclocked = new Separator("salvager-overclocked") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 40, Items.graphite, 80, Items.silicon, 40, MetrumItems.steel, 20));
            size = 2;
            craftTime = 10f;
            itemCapacity = 20;
            liquidCapacity = 40f;
            consumePower(3f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(MetrumLiquids.steam, 0.15f);
            results = with(MetrumItems.iron, 80, Items.sand, 20);
        }};

        // advanced 3x3: also recovers beryllium and tungsten - iron 60% / sand 20% / beryllium 15% / tungsten 5%
        salvagerAdvanced = new Separator("salvager-advanced") {{
            requirements(Category.crafting, with(MetrumItems.steel, 80, Items.graphite, 100, Items.silicon, 80, Items.tungsten, 40));
            size = 3;
            craftTime = 12f;
            itemCapacity = 30;
            liquidCapacity = 60f;
            consumePower(4f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(MetrumLiquids.steam, 0.2f);
            results = with(MetrumItems.iron, 60, Items.sand, 20, Items.beryllium, 15, Items.tungsten, 5);
        }};

        salvager = new Separator("salvager") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 30, Items.graphite, 80, Items.silicon, 20));
            size = 2;
            craftTime = 20f;
            itemCapacity = 20;
            liquidCapacity = 40f;
            consumePower(2f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(MetrumLiquids.steam, 0.12f);
            results = with(MetrumItems.iron, 2, Items.sand, 3);
        }};
    }

    private static GenericCrafter decompressor(String name, int graphite, int coal, float power, ItemStack[] req) {
        return new GenericCrafter(name) {{
            requirements(Category.crafting, req);
            size = 3;
            health = 240;
            craftTime = 120f;
            itemCapacity = 30;
            squareSprite = false;
            consumePower(power);
            consumeItem(Items.graphite, graphite);
            outputItem = new ItemStack(Items.coal, coal);
            craftEffect = Fx.smeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion() {{ color = flame; }},
                new DrawCrucibleFlame() {{ flameColor = flame; midColor = flame; particleRad = 12f; particleSize = 1f; }});
        }};
    }
}
