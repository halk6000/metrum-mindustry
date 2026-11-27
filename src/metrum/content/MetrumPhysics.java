package metrum.content;

import mindustry.content.Items;
import mindustry.content.Liquids;
import metrum.world.ChemicalSplitter;
import metrum.world.ChemicalSplitter.Product;
import mindustry.type.Category;
import metrum.world.Atomizer;
import metrum.world.AtomizerFactory;
import metrum.world.HadronCollider;

import static mindustry.type.ItemStack.with;
import static metrum.content.MetrumItems.*;

/** Particle physics line: atomizer -> hadron collider. */
public class MetrumPhysics {
    public static Atomizer atomizer, deAtomizer;
    public static AtomizerFactory atomizerFactory;
    public static HadronCollider hadronCollider;
    public static ChemicalSplitter chemicalSplitter;
    public static metrum.world.CompoundDensifier compoundDensifier;

    public static void load() {
        chemicalSplitter = new ChemicalSplitter("chemical-splitter");
        chemicalSplitter.requirements(Category.crafting, with(steel,75,Items.oxide,60,Items.silicon,40));
        chemicalSplitter.size=4;
        chemicalSplitter.craftTime=60f;
        chemicalSplitter.consumePower(250f/60f);
        chemicalSplitter.boostItem=quarkMatter;
        chemicalSplitter.boostMultiplier=2.5f;
        // Game units represent elemental equivalents, not molecular gas volumes.
        chemicalSplitter.recipe(Liquids.water,new Product(Liquids.hydrogen,4),new Product(MetrumLiquids.oxygen,2))
            .recipe(Liquids.ozone,new Product(MetrumLiquids.oxygen,6))
            .recipe(Liquids.cyanogen,new Product(Items.graphite,4),new Product(Liquids.nitrogen,4))
            .recipe(Items.sand,new Product(Items.silicon,2),new Product(MetrumLiquids.oxygen,4))
            .recipe(Items.oxide,new Product(Items.beryllium,2),new Product(MetrumLiquids.oxygen,2))
            .recipe(carborundum,new Product(Items.silicon,2),new Product(Items.graphite,2))
            .recipe(tungstenCarbide,new Product(Items.tungsten,2),new Product(Items.graphite,2))
            .recipe(Liquids.arkycite,new Product(Items.graphite,4),new Product(MetrumLiquids.ammonia,2))
            .recipe(MetrumLiquids.ammonia,new Product(Liquids.hydrogen,6),new Product(Liquids.nitrogen,2));

        compoundDensifier = new metrum.world.CompoundDensifier("compound-densifier");
        compoundDensifier.requirements(Category.crafting, with(steel,40,Items.oxide,60,Items.silicon,30));
        compoundDensifier.size=2;
        compoundDensifier.craftTime=60f;
        compoundDensifier.consumePower(90f/60f);
        compoundDensifier.boostItem=quarkMatter;
        compoundDensifier.boostMultiplier=2.5f;
        compoundDensifier.recipe(MetrumLiquids.oxygen,6,Liquids.ozone,2);

        atomizer = new Atomizer("atomizer");
        atomizer.requirements(Category.crafting, with(steel, 60, Items.silicon, 80, Items.tungsten, 40, Items.graphite, 40));
        atomizer.size = 2;
        atomizer.craftTime = 45f;
        atomizer.inputAmount = 2;
        atomizer.consumePower(1.5f);
        atomizer.convert(Items.graphite, atomicCarbon)
            .convert(Items.silicon, atomicSilicon)
            .convert(Items.beryllium, atomicBeryllium)
            .convert(iron, atomicIron)
            .convert(Items.tungsten, atomicTungsten)
            .convert(Items.thorium, atomicThorium)
            .convert(Items.copper, atomicCopper)
            .convert(Items.lead, atomicLead)
            .convert(Items.titanium, atomicTitanium);

        atomizerFactory = new AtomizerFactory("atomizer-factory");
        atomizerFactory.requirements(Category.crafting, with(steel, 100, carborundum, 10, Items.surgeAlloy, 12));
        atomizerFactory.size = 3;
        atomizerFactory.health = 750;
        atomizerFactory.conversions.putAll(atomizer.conversions);
        atomizerFactory.consumePowerDynamic((AtomizerFactory.FactoryBuild b) -> b.activeSlots() * 2.125f);

        // reverse: atoms back into the material (1 atom -> 1 item)
        deAtomizer = new Atomizer("de-atomizer");
        deAtomizer.requirements(Category.crafting, with(steel, 60, Items.silicon, 80, Items.tungsten, 40, Items.graphite, 40));
        deAtomizer.size = 2;
        deAtomizer.craftTime = 30f;
        deAtomizer.inputAmount = 1;
        deAtomizer.consumePower(1f);
        deAtomizer.convert(atomicCarbon, Items.graphite).convert(atomicSilicon, Items.silicon).convert(atomicBeryllium, Items.beryllium)
            .convert(atomicIron, iron).convert(atomicTungsten, Items.tungsten).convert(atomicThorium, Items.thorium)
            .convert(atomicCopper, Items.copper).convert(atomicLead, Items.lead).convert(atomicTitanium, Items.titanium);
    
        hadronCollider = new HadronCollider("hadron-collider");
        hadronCollider.requirements(Category.crafting,
            with(steel, 200, Items.tungsten, 150, Items.silicon, 200, Items.surgeAlloy, 60, Items.oxide, 80));
        hadronCollider.size = 3;
        hadronCollider.craftTime = 90f;
        hadronCollider.consumePower(8f);
        hadronCollider.recipe(atomicCarbon, atomicSilicon, carborundum, 1)
            .recipe(atomicTungsten, atomicCarbon, tungstenCarbide, 1)
            .recipe(atomicBeryllium, atomicIron, berylliumBronze, 2)
            .recipe(atomicIron, atomicTungsten, ferrotungsten, 1)
            .recipe(atomicThorium, atomicThorium, quarkMatter, 1);
    }
}
