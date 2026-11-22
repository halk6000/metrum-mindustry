package metrum.content;

import arc.graphics.Color;
import mindustry.type.Item;

/** Items ported from the original Metrum JSON mod. */
public class MetrumItems {
    public static Item atomicCarbon, atomicSilicon, atomicBeryllium, atomicIron, atomicTungsten, atomicThorium;
    public static Item carborundum, tungstenCarbide, berylliumBronze, ferrotungsten, quarkMatter;
    public static Item ammoCrate, hullLight, hullMedium, hullHeavy, partEngine, partWings, partLegs;
    public static Item pieceRock, iron, steel, weaponPart, advancedWeaponPart;

    public static void load() {
        pieceRock = new Item("piece-rock", Color.valueOf("523d43")) {{
            hardness = 1;
            cost = 0.6f;
        }};
        iron = new Item("iron", Color.valueOf("adadac")) {{
            hardness = 2;
            charge = 0.6f;
            cost = 0.8f;
        }};
        steel = new Item("steel", Color.valueOf("504848")) {{
            hardness = 3;
            charge = 0.6f;
            cost = 1.2f;
        }};
        weaponPart = new Item("weapon-part", Color.valueOf("534b2d")) {{
            hardness = 3;
            charge = 0.6f;
            cost = 1.5f;
        }};
        advancedWeaponPart = new Item("advanced-weapon-part", Color.valueOf("484742")) {{
            hardness = 4;
            charge = 0.6f;
            cost = 2.5f;
        }};
    
        atomicCarbon = atomic("atomic-carbon", "50505a", 0f);
        atomicSilicon = atomic("atomic-silicon", "78829a", 0f);
        atomicBeryllium = atomic("atomic-beryllium", "5ac896", 0f);
        atomicIron = atomic("atomic-iron", "bebebe", 0f);
        atomicTungsten = atomic("atomic-tungsten", "788cc8", 0f);
        atomicThorium = atomic("atomic-thorium", "dc6eaa", 1f);

        ammoCrate = new Item("ammo-crate", Color.valueOf("8a7a3a")) {{
            cost = 1.5f;
            explosiveness = 0.3f;
        }};

        // drone hulls (Hull Printer)
        hullLight = new Item("hull-light", Color.valueOf("96a0af")) {{ cost = 2f; }};
        hullMedium = new Item("hull-medium", Color.valueOf("7d8796")) {{ cost = 3f; }};
        hullHeavy = new Item("hull-heavy", Color.valueOf("646c7a")) {{ cost = 4f; }};

        // drone parts (Part Printer)
        partEngine = new Item("part-engine", Color.valueOf("ff963c")) {{ cost = 2f; }};
        partWings = new Item("part-wings", Color.valueOf("8c96a5")) {{ cost = 1.5f; }};
        partLegs = new Item("part-legs", Color.valueOf("787d8c")) {{ cost = 1.5f; }};

        // hadron collider products
        carborundum = product("carborundum", "3c8c8c", 0f);
        tungstenCarbide = product("tungsten-carbide", "5a6478", 0f);
        berylliumBronze = product("beryllium-bronze", "c8a050", 0f);
        ferrotungsten = product("ferrotungsten", "8c8ca0", 0f);
        quarkMatter = product("quark-matter", "ff50c8", 1.5f);
        quarkMatter.charge = 1.5f;
    }

    private static Item product(String name, String color, float radioactivity) {
        Item i = new Item(name, Color.valueOf(color));
        i.radioactivity = radioactivity;
        i.cost = 3f;
        return i;
    }

    /** Atomic form of a mineral - collider feedstock. */
    private static Item atomic(String name, String color, float radioactivity) {
        Item i = new Item(name, Color.valueOf(color));
        i.charge = 0.3f;
        i.radioactivity = radioactivity;
        i.cost = 2f;
        return i;
    }
}
