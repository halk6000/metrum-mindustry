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
        ammoFactory, steamFurnace, largeSteamFurnace, salvagerOverclocked, salvagerAdvanced, arcFurnaceOverclocked, heatInverter, magmaForge, plasmaIonizer, stoneCrusher, infusionSmelter, salvager, rockMelter;
    public static metrum.world.OpenPitMine openPitMine;

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
            drawer = new metrum.world.DrawSteamBoiler();
        }};

        largeSteamFurnace = new metrum.world.OxidizingFurnace("large-steam-furnace") {{
            health = 360;
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
            drawer = new metrum.world.DrawSteamBoiler();
        }};
        // earned, not built: a large furnace that runs 80 s on ozone without exploding turns into this
        metrum.world.OxidizingFurnace safeSteamFurnace = new metrum.world.OxidizingFurnace("safe-steam-furnace") {{
            buildVisibility = mindustry.world.meta.BuildVisibility.hidden; // earned only, never on the build list
            health = 540; // +50%
            ozoneOnly = true;
            ozoneSpeed = 2f;
            explosionChance = radarExplosionChance = 0.01f;
            explosionInterval = 30f;
            explosionDamageFraction = 0.05f;
            explosionSplashDamage = 20f;
            // same cost as the large furnace; rebuilding also takes rebuildFee (charged once the rebuild finishes)
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
            drawer = new metrum.world.DrawSteamBoiler();
        }};
        ((metrum.world.OxidizingFurnace) largeSteamFurnace).upgradeTo = safeSteamFurnace;
        safeSteamFurnace.rebuildAs = (metrum.world.OxidizingFurnace) largeSteamFurnace;
        safeSteamFurnace.rebuildFee = new mindustry.type.ItemStack(MetrumItems.quarkMatter, 5);

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

        // primitive slag source: 1 rock + 1 coal -> 12 slag per cycle
        rockMelter = new GenericCrafter("rock-melter") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 60, MetrumItems.iron, 40, Items.silicon, 30));
            size = 2;
            craftTime = metrum.Balance.f("melter.cycleSeconds", 1f) * 60f;
            itemCapacity = 20;
            liquidCapacity = 120f;
            updateEffect = Fx.smeltsmoke;
            consumePower(metrum.Balance.f("melter.powerPerSecond", 300f) / 60f);
            consumeItems(with(MetrumItems.pieceRock, 1, Items.coal, 1));
            outputLiquid = new LiquidStack(Liquids.slag, metrum.Balance.f("melter.slagPerCycle", 12f) / craftTime);
            drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion() {{ color = Color.valueOf("ff9a3c"); }});
        }};

        // digs 1% per step; every step may reveal underground resources (discovery amounts are weights)
        openPitMine = new metrum.world.OpenPitMine("open-pit-mine") {{
            requirements(Category.production, with(Items.beryllium, 1000, MetrumItems.steel, 200));
            size = 4;
            stepTime = metrum.Balance.f("pit.stepSeconds", 10f) * 60f;
            discoveryChance = metrum.Balance.f("pit.discoveryChance", 0.2f);
            boostedChance = metrum.Balance.f("pit.waterChance", 0.24f);
            radar = mindustry.content.Blocks.radar;
            radarTimeSaved = metrum.Balance.f("pit.radarSeconds", 2f) * 60f;
            liquidCapacity = 60f;
            consumeLiquid(Liquids.water, metrum.Balance.f("pit.waterPerSecond", 12f) / 60f).boost();
            minDiscovery = metrum.Balance.i("pit.minDiscovery", 50);
            maxDiscovery = metrum.Balance.i("pit.maxDiscovery", 200);
            depositPerTile = metrum.Balance.i("pit.depositPerTile", 25);
            discoveries = new ItemStack[]{
                pitWeight("metonia.beryllium", Items.beryllium, 35),
                pitWeight("metonia.graphite", Items.graphite, 25),
                pitWeight("metonia.iron", MetrumItems.iron, 20),
                pitWeight("metonia.tungsten", Items.tungsten, 12),
                pitWeight("metonia.thorium", Items.thorium, 8)};
            byPlanet.put(mindustry.content.Planets.erekir, new ItemStack[]{
                pitWeight("erekir.beryllium", Items.beryllium, 40),
                pitWeight("erekir.graphite", Items.graphite, 30),
                pitWeight("erekir.tungsten", Items.tungsten, 20),
                pitWeight("erekir.thorium", Items.thorium, 10)});
            byPlanet.put(mindustry.content.Planets.serpulo, new ItemStack[]{
                pitWeight("serpulo.copper", Items.copper, 30),
                pitWeight("serpulo.lead", Items.lead, 25),
                pitWeight("serpulo.coal", Items.coal, 15),
                pitWeight("serpulo.titanium", Items.titanium, 14),
                pitWeight("serpulo.scrap", Items.scrap, 10),
                pitWeight("serpulo.thorium", Items.thorium, 6)});
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
            drawer = new metrum.world.DrawInfusionSmelter();
        }};

        // overclocked silicon arc furnace 3x3: 1 graphite + 2 sand -> 6 silicon, 450 power/s, 95% of vanilla time;
        // atomic carbon as booster -> 12 silicon per cycle
        arcFurnaceOverclocked = new metrum.world.BoostedCrafter("arc-furnace-overclocked") {{
            requirements(Category.crafting, with(Items.tungsten, 80, Items.graphite, 100, Items.silicon, 60, MetrumItems.steel, 40));
            size = 3;
            craftTime = 50f * 0.95f;
            itemCapacity = 30;
            consumePower(450f / 60f);
            consumeItems(with(Items.graphite, 1, Items.sand, 2));
            outputItem = new ItemStack(Items.silicon, 6);
            boostItem = MetrumItems.atomicCarbon;
            boostMultiplier = 2f;
            craftEffect = Fx.none;
            updateEffect = Fx.smeltsmoke;
            // Keep the recognizable vanilla arc-furnace body, including its build preview.
            drawer = new DrawMulti(
                new DrawRegion() {{ name = "silicon-arc-furnace"; }},
                new DrawArcSmelt() {{
                    flameColor = Color.valueOf("ffb36b");
                    midColor = Color.valueOf("fff0cc");
                    flameRad = 4f;
                    particleRad = 7f;
                    particles = 16;
                }}
            );
        }
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };

        // Same cycle as the Salvager; each iron roll yields two items, sand yields one.
        salvagerOverclocked = new Separator("salvager-overclocked") {{
            requirements(Category.crafting, with(MetrumItems.pieceRock, 40, Items.graphite, 80, Items.silicon, 40, MetrumItems.steel, 20));
            size = 2;
            craftTime = 20f;
            itemCapacity = 20;
            liquidCapacity = 40f;
            consumePower(3f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(MetrumLiquids.steam, 0.15f);
            results = with(MetrumItems.iron, 80, Items.sand, 20);
            drawer = new metrum.world.DrawSalvager() {{ overclocked = true; }};
            buildType = () -> new SeparatorBuild() {
                @Override
                public boolean shouldConsume() {
                    // Reserve room for the whole iron batch when the belts are blocked.
                    return super.shouldConsume()
                        && items.total() - items.get(MetrumItems.pieceRock) <= itemCapacity - 2;
                }

                @Override
                public void offload(mindustry.type.Item item) {
                    super.offload(item);
                    if (item == MetrumItems.iron) super.offload(item);
                }
            };
        }
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };

        // advanced 3x3: also recovers beryllium and tungsten - iron 60% / sand 20% / beryllium 15% / tungsten 5%
        salvagerAdvanced = new Separator("salvager-advanced") {{
            requirements(Category.crafting, with(MetrumItems.steel, 160, Items.graphite, 100, Items.silicon, 80));
            size = 3;
            craftTime = 12f;
            itemCapacity = 30;
            liquidCapacity = 60f;
            consumePower(4f);
            consumeItem(MetrumItems.pieceRock);
            consumeLiquid(MetrumLiquids.steam, 0.2f);
            results = with(MetrumItems.iron, 60, Items.sand, 20, Items.beryllium, 15, Items.tungsten, 5);
            drawer = new metrum.world.DrawSalvager() {{ advanced = true; }};
        }
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };

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
            drawer = new metrum.world.DrawSalvager();
        }
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };
    }

    /** same-size upgrade families: build the better (or weaker) version straight over the other */
    public static void upgradeFamilies() {
        metrum.world.Upgrades.family(graphiteDecompressor, graphiteDecompressorOverclocked);
        metrum.world.Upgrades.family(salvager, salvagerOverclocked);
        metrum.world.Upgrades.family(mindustry.content.Blocks.siliconArcFurnace, arcFurnaceOverclocked);
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
        }
            @Override public boolean canReplace(mindustry.world.Block other) {
                return metrum.world.Upgrades.canReplace(this, other) || super.canReplace(other);
            }
        };
    }

    /** discovery weight of the open-pit mine, adjustable in the balance editor */
    private static ItemStack pitWeight(String key, mindustry.type.Item item, int weight) {
        return new ItemStack(item, Math.max(0, metrum.Balance.i("pit." + key, weight)));
    }
}
