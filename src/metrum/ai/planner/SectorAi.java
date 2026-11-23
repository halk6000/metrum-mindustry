package metrum.ai.planner;

import arc.Core;
import arc.math.Rand;
import arc.struct.Seq;
import mindustry.type.Planet;
import mindustry.type.Sector;

/**
 * Planet difficulty (5 steps, Settings -> Metrum) decides how the AI levels are spread over the
 * planet's sectors (table from ROADMAP.md). The assignment is rolled once per planet+difficulty and remembered.
 * The planet's main sector ({@link #mainSector}) is always a Battle Tactician and is counted separately.
 */
public class SectorAi {
    public static final String difficultySetting = "metrum-planet-difficulty";
    /** id of the main sector to conquer per planet name; -1 = none yet */
    public static int mainSector = -1;

    // columns follow AiLevel order: DUMB, EASY, HARD, TACTICAL, CHESS, BATTLE_TACTICIAN (percent)
    static final int[][] table = {
        {40, 60, 0, 0, 0, 0},   // 1. Casual
        {20, 60, 10, 2, 6, 2},  // 2. Easy
        {10, 40, 25, 5, 15, 5}, // 3. Normal
        {0, 10, 40, 10, 30, 10},// 4. Hard
        {0, 0, 50, 10, 30, 10}, // 5. Extermination (+ at least 7 Battle Tacticians)
    };

    public static int difficulty() {
        return Math.max(1, Math.min(5, Core.settings.getInt(difficultySetting, 3)));
    }

    /** AI level of a sector (rolled on first use for the current planet difficulty) */
    public static AiLevel level(Sector sector) {
        if (sector.id == mainSector) return AiLevel.BATTLE_TACTICIAN;
        String key = "metrum-sectorai-" + sector.planet.name + "-" + difficulty();
        String data = Core.settings.getString(key, "");
        if (data.isEmpty()) {
            data = roll(sector.planet);
            Core.settings.put(key, data);
        }
        String[] parts = data.split(",");
        int idx = sector.id < parts.length ? parts[sector.id].charAt(0) - '0' : 2;
        return AiLevel.values()[Math.max(0, Math.min(AiLevel.values().length - 1, idx))];
    }

    /** one digit (AiLevel ordinal) per sector id, distributed by the difficulty table */
    static String roll(Planet planet) {
        int diff = difficulty();
        Seq<Sector> pool = new Seq<>();
        for (Sector s : planet.sectors) if (s.id != mainSector) pool.add(s);
        int n = pool.size;
        int[] pct = table[diff - 1];
        // largest-remainder rounding so the counts add up to n exactly
        int[] counts = new int[pct.length];
        float[] rem = new float[pct.length];
        int total = 0;
        for (int i = 0; i < pct.length; i++) {
            float exact = n * pct[i] / 100f;
            counts[i] = (int) exact;
            rem[i] = exact - counts[i];
            total += counts[i];
        }
        while (total < n) {
            int best = 0;
            for (int i = 1; i < pct.length; i++) if (pct[i] > 0 && rem[i] > rem[best] || pct[best] == 0) best = i;
            counts[best]++;
            rem[best] = -1f;
            total++;
        }
        // Extermination: at least 7 Battle Tacticians on the planet, main sector included
        if (diff == 5) {
            int bt = AiLevel.BATTLE_TACTICIAN.ordinal();
            int need = Math.min(7 - (mainSector >= 0 ? 1 : 0), n) - counts[bt];
            for (int i = 0; i < counts.length && need > 0; i++) {
                if (i == bt) continue;
                while (counts[i] > 0 && need > 0 && i == AiLevel.HARD.ordinal()) { counts[i]--; counts[bt]++; need--; }
            }
        }
        // shuffle sectors deterministically per planet, then hand out levels
        Rand r = new Rand(planet.name.hashCode() * 31L + diff);
        for (int i = pool.size - 1; i > 0; i--) pool.swap(i, r.random(i));
        char[] out = new char[planet.sectors.size];
        java.util.Arrays.fill(out, (char) ('0' + AiLevel.HARD.ordinal()));
        int k = 0;
        for (int lvl = 0; lvl < counts.length; lvl++) {
            for (int c = 0; c < counts[lvl] && k < pool.size; c++) out[pool.get(k++).id] = (char) ('0' + lvl);
        }
        if (mainSector >= 0 && mainSector < out.length) out[mainSector] = (char) ('0' + AiLevel.BATTLE_TACTICIAN.ordinal());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.length; i++) { if (i > 0) sb.append(','); sb.append(out[i]); }
        return sb.toString();
    }
}
