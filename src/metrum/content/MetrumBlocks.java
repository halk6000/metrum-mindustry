package metrum.content;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.WallCrafter;
import mindustry.world.meta.Attribute;

import static mindustry.type.ItemStack.with;

/** Blocks ported from the original Metrum JSON mod. */
public class MetrumBlocks {
    public static Block oreIron;
    public static Block streamDrillT1, streamDrillT1Adv, streamDrillT2, streamDrillT2Adv, rockScraper;

    public static void load() {
        // iron ore: generated on Ferrox, mineable by drills and MonoMet drones
        oreIron = new mindustry.world.blocks.environment.OreBlock("ore-iron", MetrumItems.iron);

        streamDrillT1 = streamDrill("stream-drill-t1", 3, 150f, 0.09f, 0.001f, 1.25f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 65));
        streamDrillT1Adv = streamDrill("stream-drill-t1-adv", 4, 100f, 0.12f, 0.001f, 1.15f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 60, MetrumItems.iron, 60));
        streamDrillT2 = streamDrill("stream-drill-t2", 3, 150f, 0.08f, 0.006f, 1.75f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 80));
        streamDrillT2Adv = streamDrill("stream-drill-t2-adv", 4, 120f, 0.06f, 0.025f, 3f,
            with(Items.graphite, 45, Items.silicon, 30, Items.beryllium, 80, MetrumItems.iron, 90));

        rockScraper = new WallCrafter("rock-scraper") {{
            requirements(Category.production, with(Items.silicon, 100, Items.beryllium, 200, Items.graphite, 200));
            consumePower(0.6f);
            output = MetrumItems.pieceRock;
            attribute = Attribute.sand;
            drillTime = 40f;
            size = 2;
        }};
    }

    private static BeamDrill streamDrill(String name, int tier, float drillTime, float power,
                                         float steamUse, float boost, ItemStack[] req) {
        return new BeamDrill(name) {{
            requirements(Category.production, req);
            this.tier = tier;
            this.drillTime = drillTime;
            size = 2;
            range = 6;
            optionalBoostIntensity = boost;
            itemCapacity = 20;
            consumePower(power);
            consumeLiquid(MetrumLiquids.steam, steamUse).boost();
        }};
    }
}
