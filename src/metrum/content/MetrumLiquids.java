package metrum.content;

import arc.graphics.Color;
import mindustry.type.Liquid;

/** Liquids ported from the original Metrum JSON mod ("Stream" = steam). */
public class MetrumLiquids {
    public static Liquid steam;

    public static void load() {
        steam = new Liquid("steam", Color.valueOf("f5f5f5")) {{
            gas = true;
            temperature = 0.9f;
            heatCapacity = 0.4f;
            barColor = Color.valueOf("d8d8d8");
        }};
    }
}
