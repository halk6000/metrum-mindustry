package metrum.content;

import arc.graphics.Color;
import mindustry.type.Item;

/** Items ported from the original Metrum JSON mod. */
public class MetrumItems {
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
    }
}
