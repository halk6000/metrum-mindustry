package metrum.content;

import arc.graphics.Color;
import mindustry.type.Liquid;

/** Liquids ported from the original Metrum JSON mod ("Stream" = steam). */
public class MetrumLiquids {
    public static Liquid steam, magma, plasma, oxygen, ammonia;

    public static void load() {
        oxygen = new Liquid("oxygen", Color.valueOf("a9d8f8")) {{ gas = true; }};
        steam = new Liquid("steam", Color.valueOf("f5f5f5")) {{
            gas = true;
            temperature = 0.9f;
            heatCapacity = 0.4f;
            barColor = Color.valueOf("d8d8d8");
        }};
    
        // fuels for the fuel station (slag is vanilla)
        magma = new Liquid("magma", Color.valueOf("ff6a2e")) {{
            temperature = 1f;
            viscosity = 0.8f;
            lightColor = Color.valueOf("ff6a2e").a(0.5f);
        }};
        plasma = new Liquid("plasma", Color.valueOf("c896ff")) {{
            gas = true;
            temperature = 1f;
            lightColor = Color.valueOf("c896ff").a(0.6f);
        }};

        ammonia = new Liquid("ammonia", Color.valueOf("d1e5a0")) {{ gas = true; }};
    }
}
