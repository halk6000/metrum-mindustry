package metrum.ai.planner.goal;

/** Strategic objectives. Execution belongs to task/, not to the scheduler. */
public enum DevelopmentGoal {
    ECONOMY("Podstawowa gospodarka", "Basic economy"),
    SILICON("Produkcja krzemu", "Silicon production"),
    FIRST_TURRET("Pierwsza obrona", "First defence"),
    UNIT_FACTORY("Produkcja jednostek", "Unit production"),
    OPTIMIZE("Wydajność gospodarki", "Economic throughput"),
    DEFENCE("Rozbudowa obrony", "Defence expansion"),
    CORE_UPGRADE("Ulepszenie rdzenia", "Core upgrade"),
    DEVELOPED("Dalszy rozwój i atak", "Further development and assault");

    private final String pl, en;
    DevelopmentGoal(String pl, String en) { this.pl = pl; this.en = en; }
    public String label(boolean polish) { return polish ? pl : en; }
    public DevelopmentGoal next() { return this == DEVELOPED ? this : values()[ordinal() + 1]; }
    public int turretLimit(int tier) {
        int extra = Math.max(0, tier - 1) * 2;
        if (ordinal() < FIRST_TURRET.ordinal()) return 0;
        return (ordinal() < DEFENCE.ordinal() ? 1 : 2) + extra;
    }
}
