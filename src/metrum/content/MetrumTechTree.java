package metrum.content;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;

import static metrum.content.MetrumItems.*;
import static metrum.content.MetrumBlocks.*;
import static metrum.content.MetrumProduction.*;
import static metrum.content.MetrumPower.*;
import static metrum.content.MetrumUnitBlocks.*;
import static metrum.content.MetrumUnits.*;

/** Hooks Metrum content into the Erekir tech tree (vanilla trees are loaded before mods). */
public class MetrumTechTree {

    public static void load() {
        // items & liquids
        node(Items.sand, pieceRock);
        node(pieceRock, iron);
        node(iron, steel);
        node(iron, weaponPart);
        node(weaponPart, advancedWeaponPart);
        node(Liquids.water, MetrumLiquids.steam);

        // drills
        node(Blocks.plasmaBore, streamDrillT1);
        node(streamDrillT1, streamDrillT1Adv);
        node(streamDrillT1, streamDrillT2);
        node(streamDrillT2, streamDrillT2Adv);
        node(Blocks.cliffCrusher, rockScraper);

        // crafting
        node(Blocks.siliconArcFurnace, graphiteDecompressor);
        node(graphiteDecompressor, graphiteDecompressorOverclocked);
        node(rockScraper, steamFurnace);
        node(rockScraper, stoneCrusher);
        node(steamFurnace, largeSteamFurnace);
        node(steamFurnace, heatInverter);
        node(steamFurnace, infusionSmelter);
        node(steamFurnace, salvager);

        // power
        node(Blocks.turbineCondenser, turbineAdvanced);

        // drones
        node(Blocks.tankFabricator, airMinerDock);
        node(airMinerDock, airMinerRebuilder);
        node(airMinerDock, monoMetT1);
        node(monoMetT1, monoMetT1b);
        node(monoMetT1, monoMetT2);
        node(monoMetT2, monoMetT2b);
        node(monoMetT2, monoMetT3);
        node(monoMetT3, monoMetT3b);
    }

    /** Attaches {@code content} under {@code parent}'s node; skips silently if the parent has no node. */
    public static TechNode node(UnlockableContent parent, UnlockableContent content) {
        TechNode p = parent.techNode;
        if (p == null) {
            arc.util.Log.warn("[Metrum] no tech node for @, @ left out of tree", parent.name, content.name);
            return null;
        }
        TechNode n = new TechNode(p, content, content.researchRequirements());
        n.planet = p.planet;
        content.techNode = n;
        return n;
    }
}
