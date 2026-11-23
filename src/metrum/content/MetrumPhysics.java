package metrum.content;

import mindustry.content.Items;
import mindustry.type.Category;
import metrum.world.Atomizer;
import metrum.world.HadronCollider;

import static mindustry.type.ItemStack.with;
import static metrum.content.MetrumItems.*;

/** Particle physics line: atomizer -> hadron collider. */
public class MetrumPhysics {
    public static Atomizer atomizer, deAtomizer;
    public static HadronCollider hadronCollider;

    public static void load() {
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
