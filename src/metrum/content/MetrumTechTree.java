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
        // research cost tuning: endgame tech is pricier than its build cost suggests
        MetrumPhysics.hadronCollider.researchCostMultiplier = 2f;
        MetrumPhysics.atomizer.researchCostMultiplier = 1.5f;
        plasmaIonizer.researchCostMultiplier = 1.5f;
        warReconstructor.researchCostMultiplier = 1.5f;
        warReconstructor.researchCostMultiplier = 2f;
        fuelStation.researchCostMultiplier = 1.2f;
        // early steam tech stays cheap so the campaign opens quickly
        steamFurnace.researchCostMultiplier = 0.6f;
        rockScraper.researchCostMultiplier = 0.6f;

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

        // particle physics
        node(steel, MetrumPhysics.atomizer);
        node(MetrumPhysics.atomizer, atomicCarbon);
        node(MetrumPhysics.atomizer, atomicSilicon);
        node(MetrumPhysics.atomizer, atomicBeryllium);
        node(MetrumPhysics.atomizer, atomicIron);
        node(MetrumPhysics.atomizer, atomicTungsten);
        node(MetrumPhysics.atomizer, atomicThorium);
        node(MetrumPhysics.atomizer, MetrumPhysics.hadronCollider);
        node(MetrumPhysics.hadronCollider, carborundum);
        node(carborundum, carborundumWall);
        node(carborundum, carborundumBore);
        node(carborundumWall, carborundumWallLarge);
        node(MetrumPhysics.hadronCollider, tungstenCarbide);
        node(tungstenCarbide, tungstenCarbideWall);
        node(tungstenCarbideWall, tungstenCarbideWallLarge);
        node(tungstenCarbide, quarkCannon);
        node(MetrumPhysics.hadronCollider, berylliumBronze);
        node(MetrumPhysics.hadronCollider, ferrotungsten);
        node(MetrumPhysics.hadronCollider, quarkMatter);
        node(quarkMatter, infusionReactor);

        // power
        node(Blocks.turbineCondenser, turbineAdvanced);

        // drones
        node(Blocks.tankFabricator, airMinerDock);
        node(airMinerDock, airMinerRebuilder);
        node(airMinerDock, fuelStation);
        node(airMinerDock, hullPrinter);
        node(hullPrinter, hullLight);
        node(hullPrinter, hullMedium);
        node(hullPrinter, hullHeavy);
        node(hullPrinter, partPrinter);
        node(partPrinter, partEngine);
        node(partPrinter, partWings);
        node(partPrinter, partLegs);
        node(partPrinter, partsMagazine);
        node(partPrinter, weaponPrinter);
        node(weaponPrinter, weaponsMagazine);
        node(weaponMachineGun, hullGunTurret);
        node(weaponMissiles, hullMissileTurret);
        node(weaponPrinter, metrum.modular.MetrumModular.assembler);
        node(weaponPrinter, weaponMiningLaser);
        node(weaponPrinter, weaponMissiles);
        node(weaponPrinter, weaponExplosives);
        node(weaponPrinter, weaponFuelTank);
        node(weaponPrinter, weaponMachineGun);
        node(fuelStation, magmaForge);
        node(magmaForge, MetrumLiquids.magma);
        node(magmaForge, plasmaIonizer);
        node(plasmaIonizer, MetrumLiquids.plasma);
        node(airMinerDock, monoMetT1);
        node(monoMetT1, monoMetT1b);
        node(monoMetT1, monoMetT2);
        node(monoMetT2, monoMetT2b);
        node(monoMetT2, monoMetT3);
        node(monoMetT3, monoMetT3b);
        node(airMinerRebuilder, warReconstructor);
        node(infusionSmelter, ammoFactory);
        node(ammoFactory, ammoCrate);
        node(monoMetT1b, monoMetT1c);
        node(monoMetT2b, monoMetT2c);
        node(monoMetT3b, monoMetT3c);
        node(monoMetT1c, monoMetT1d);
        node(monoMetT2c, monoMetT2d);
        node(monoMetT3c, monoMetT3d);
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
