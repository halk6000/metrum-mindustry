package metrum.ai.planner;

import arc.Core;
import arc.math.Rand;
import arc.struct.Seq;
import mindustry.type.Planet;
import mindustry.type.Sector;

public class SectorAi {
    public static int mainSector = -1;

    // Upewnij się, że kolejność kolumn odpowiada dokładnie kolejności enum AiLevel!
    // Poniżej układ zgodny z tabelą z ROADMAP.md: DUMB, EASY, HARD, CHESS, TACTICAL, BATTLE_TACTICIAN
    static final int[][] table = {
        {40, 60, 0,  0,  0,  0},  // 1. Casual (Swobodny)
        {20, 60, 10, 6,  2,  2},  // 2. Easy (Łatwy)
        {10, 40, 25, 15, 5,  5},  // 3. Normal (Normalny)
        {0,  10, 40, 30, 10, 10}, // 4. Hard (Trudny)
        {0,  0,  50, 30, 10, 10}, // 5. Extermination (Eksterminacja)
    };

    public static int difficulty(Planet planet) {
        return planet.campaignRules.difficulty.ordinal() + 1;
    }

    public static AiLevel level(Sector sector) {
        if (sector.id == mainSector) return AiLevel.BATTLE_TACTICIAN;
        String key = "metrum-sectorai-v2-" + sector.planet.name + "-" + difficulty(sector.planet);
        String data = Core.settings.getString(key, "");
        if (data.isEmpty()) {
            data = roll(sector.planet);
            Core.settings.put(key, data);
        }
        String[] parts = data.split(",");
        int idx = sector.id < parts.length ? parts[sector.id].charAt(0) - '0' : AiLevel.HARD.ordinal();
        return AiLevel.values()[Math.max(0, Math.min(AiLevel.values().length - 1, idx))];
    }

    static String roll(Planet planet) {
        int diff = difficulty(planet);
        Seq<Sector> pool = new Seq<>();
        for (Sector s : planet.sectors) if (s.id != mainSector) pool.add(s);
        int n = pool.size;
        int[] pct = table[diff - 1];

        int[] counts = new int[pct.length];
        float[] rem = new float[pct.length];
        int total = 0;
        for (int i = 0; i < pct.length; i++) {
            if (pct[i] == 0) {
                rem[i] = -1f;
                continue;
            }
            float exact = n * pct[i] / 100f;
            counts[i] = (int) exact;
            rem[i] = exact - counts[i];
            total += counts[i];
        }

        while (total < n) {
            int best = -1;
            for (int i = 0; i < pct.length; i++) {
                if (pct[i] > 0 && (best == -1 || rem[i] > rem[best])) {
                    best = i;
                }
            }
            if (best == -1) break;
            counts[best]++;
            rem[best] = -1f;
            total++;
        }

        // Eksterminacja: minimum 7 Taktyków bitewnych na planecie (wliczając główny sektor)
        if (diff == 5) {
            int bt = AiLevel.BATTLE_TACTICIAN.ordinal();
            int mainBonus = (mainSector >= 0) ? 1 : 0;
            int targetBt = Math.min(7, n + mainBonus) - mainBonus;
            int need = Math.max(0, targetBt - counts[bt]);

            // Pobieramy brakujące miejsca najpierw z HARD, potem CHESS, potem TACTICAL
            int[] donors = {
                AiLevel.HARD.ordinal(),
                AiLevel.CHESS.ordinal(),
                AiLevel.TACTICAL.ordinal()
            };
            for (int donor : donors) {
                while (counts[donor] > 0 && need > 0) {
                    counts[donor]--;
                    counts[bt]++;
                    need--;
                }
            }
        }

        Rand r = new Rand(planet.name.hashCode() * 31L + diff);
        for (int i = pool.size - 1; i > 0; i--) pool.swap(i, r.random(i));

        char[] out = new char[planet.sectors.size];
        java.util.Arrays.fill(out, (char) ('0' + AiLevel.HARD.ordinal()));
        int k = 0;
        for (int lvl = 0; lvl < counts.length; lvl++) {
            for (int c = 0; c < counts[lvl] && k < pool.size; c++) {
                out[pool.get(k++).id] = (char) ('0' + lvl);
            }
        }
        if (mainSector >= 0 && mainSector < out.length) {
            out[mainSector] = (char) ('0' + AiLevel.BATTLE_TACTICIAN.ordinal());
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(out[i]);
        }
        return sb.toString();
    }
}
