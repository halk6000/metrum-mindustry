package metrum.ai.planner;

import arc.Core;

/**
 * Metrum enemy AI difficulty levels. Chosen in Settings -> Metrum AI.
 * Each level changes how fast and how cleverly the enemy builds its base.
 */
public enum AiLevel {
    //            build interval (s), steps per tick, turret magazines, gap in walls under heaviest fire, unit micro
    DUMB(        30f, 1, false, false, 0),
    EASY(        20f, 1, false, true,  0),
    HARD(        12f, 2, true,  true,  1),
    TACTICAL(    9f,  2, true,  true,  2),
    CHESS(       7f,  3, true,  true,  3),
    BATTLE_TACTICIAN(5f, 4, true, true, 4);

    public final float buildInterval;
    public final int stepsPerTick;
    public final boolean turretMagazines, wallGaps;
    public final int micro;

    AiLevel(float buildInterval, int stepsPerTick, boolean turretMagazines, boolean wallGaps, int micro) {
        this.buildInterval = buildInterval;
        this.stepsPerTick = stepsPerTick;
        this.turretMagazines = turretMagazines;
        this.wallGaps = wallGaps;
        this.micro = micro;
    }

    public static final String setting = "metrum-ai-level";

    public static AiLevel current() {
        // campaign on a Metrum planet: the sector's own level, rolled from the planet difficulty
        mindustry.type.Sector sector = mindustry.Vars.state == null ? null : mindustry.Vars.state.rules.sector;
        if (mindustry.Vars.state != null && mindustry.Vars.state.isCampaign() && sector != null && BasePlanner.planets.contains(sector.planet)) {
            return SectorAi.level(sector);
        }
        int i = Core.settings == null ? 2 : Core.settings.getInt(setting, 2);
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }

    public String localized() {
        return Core.bundle.get("metrum.ai." + name().toLowerCase());
    }
}
