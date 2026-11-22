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
import mindustry.world.blocks.production.Separator;
import mindustry.world.draw.*;

import static mindustry.type.ItemStack.with;

/** Crafting blocks ported from the original Metrum JSON mod. */
public class MetrumProduction {
    public static Block graphiteDecompressor, graphiteDecompressorOverclocked,
        steamFurnace, stoneCrusher, infusionSmelter, salvager;

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
            outputLiquid = new LiquidStack(Liquids.water, 0.2f);
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
