package metrum.ai;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.EventType.BuildDamageEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import metrum.ai.planner.AiLevel;
import metrum.ai.planner.SectorAi;
import metrum.content.MetrumUnitBlocks;
import metrum.content.MetrumUnits;
import metrum.fuel.FuelSystem;

public class EnemyDropPods {
    public static float interval = 60f * 180f; // 3 minuty
    public static int maxReinforcements = 30;
    static final double reinforcementFlag = 7346282d;

    static float timer, snapshotTimer, underAttackTimer;
    static final double starterSupportFlag = 7346283d;
    static final float starterInterval = 10f * 60f * 60f, alarmDelay = 4f * 60f * 60f;
    static final int starterLimit = 12;
    static float starterTimer, alarmTimer = -1f;

    static boolean starterSector() {
        return Vars.state.isCampaign() && Vars.state.rules.sector != null
            && Vars.state.rules.sector.preset == metrum.content.MetrumPlanets.oldRavine
            && Vars.state.rules.attackMode;
    }

    public static void init() {
        Events.run(Trigger.update, EnemyDropPods::update);
        Events.on(WorldLoadEvent.class, e -> {
            timer = 0f;
            snapshotTimer = 0f;
            underAttackTimer = 0f;
            starterTimer = 0f;
            alarmTimer = -1f;
            if (starterSector() && !Vars.net.client()) {
                starterTimer = savedTimer("metrum-starter-period", 0f);
                alarmTimer = savedTimer("metrum-starter-alarm", -1f);
                Groups.unit.each(u -> {
                    if (u.team == Vars.state.rules.waveTeam && u.flag == starterSupportFlag)
                        u.controller(new mindustry.ai.types.BuilderAI(true, 400f));
                });
            }
        });
        // Wykrywanie ataku na bazę wroga (uszkodzenie budynku lub zniszczenie jednostki AI)
        Events.on(BuildDamageEvent.class, e -> {
            if (e.build != null && e.build.team == Vars.state.rules.waveTeam) {
                underAttackTimer = 60f * 45f; // baza uznawana za atakowaną przez 45 s od ostatniego ciosu
                if (!Vars.net.client() && starterSector()
                    && e.build instanceof mindustry.world.blocks.storage.CoreBlock.CoreBuild
                    && e.source != null && e.source.team != e.build.team
                    && alarmTimer < 0f) {
                    alarmTimer = alarmDelay;
                    saveStarterTimers();
                }
            }
        });
        Events.on(UnitDestroyEvent.class, e -> {
            if (e.unit != null && e.unit.team == Vars.state.rules.waveTeam) {
                underAttackTimer = 60f * 45f;
            }
        });
    }

    static float savedTimer(String key, float fallback) {
        try { return Float.parseFloat(Vars.state.rules.tags.get(key, Float.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    static void saveStarterTimers() {
        Vars.state.rules.tags.put("metrum-starter-period", Float.toString(starterTimer));
        Vars.state.rules.tags.put("metrum-starter-alarm", Float.toString(alarmTimer));
    }

    static void updateStarter(Team team) {
        if (team.core() == null) return;
        starterTimer += Time.delta;
        boolean regular = starterTimer >= starterInterval;
        if (regular) starterTimer %= starterInterval;
        boolean alarm = false;
        if (alarmTimer >= 0f) {
            alarmTimer -= Time.delta;
            if (alarmTimer <= 0f) { alarmTimer = -1f; alarm = true; }
        }
        if (regular) spawnStarter(team);
        if (alarm) spawnStarter(team);
        saveStarterTimers();
    }

    static int starterCount(int alive) {
        return Math.min(3, Math.max(0, starterLimit - alive));
    }

    static void spawnStarter(Team team) {
        Building core = team.core();
        if (core == null) return;
        int alive = Groups.unit.count(u -> u.team == team && !u.dead() && u.flag == starterSupportFlag);
        int count = starterCount(alive);
        UnitType type = MetrumUnits.monoMetT1;
        if (count == 0 || !type.supportsEnv(Vars.state.rules.env)) return;
        for (int i = 0; i < count; i++) {
            float x = Mathf.clamp(core.x + Mathf.range(24f), 0f, Vars.world.unitWidth() - 1f);
            float y = Mathf.clamp(core.y + Mathf.range(24f), 0f, Vars.world.unitHeight() - 1f);
            Fx.launchPod.at(x, y);
            Unit unit = type.spawn(team, x, y);
            unit.flag = starterSupportFlag;
            // Base T1 has no weapon: reinforce by rebuilding, not by pretending to be a combat drone.
            unit.controller(new mindustry.ai.types.BuilderAI(true, 400f));
            Fx.spawnShockwave.at(x, y);
        }
        metrum.ai.planner.task.CoreCapacityTask.drop(team);
        if (!Vars.headless) Vars.ui.hudfrag.showToast(Core.bundle.format("metrum.pod.starter", count));
    }

    static String key(Sector s) {
        return "metrum-pod-source-v1-" + s.planet.name + "-" + s.id;
    }

    static int coreLevel(Building core) {
        if (core.block == Blocks.coreAcropolis || core.block == Blocks.coreNucleus) return 3;
        if (core.block == Blocks.coreCitadel || core.block == Blocks.coreFoundation) return 2;
        return 1;
    }

    static void snapshot(Sector here, Team team) {
        int level = 0, pods = 0, score = 0;
        for (Building core : team.cores()) {
            level = Math.max(level, coreLevel(core));
            score += 200 * coreLevel(core);
        }
        for (Tile tile : Vars.world.tiles) {
            Building b = tile.build;
            if (b == null || b.tile != tile || b.team != team) continue;
            if (b.block == MetrumUnitBlocks.dropPodLauncher) { pods++; score += 600; }
            else if (b.block instanceof mindustry.world.blocks.defense.turrets.Turret) score += 20;
            else if (b.block instanceof mindustry.world.blocks.production.Drill) score += 2;
            else if (b.block instanceof mindustry.world.blocks.production.GenericCrafter) score += 30;
        }
        Core.settings.put(key(here), level + "," + pods + "," + score);
    }

    /**
     * Zwraca [poziom_rdzenia, liczba_kapsul, punkty_gospodarki].
     * Jeśli gracz jeszcze nie odwiedził danego sektora wroga, szacuje jego rozwój
     * na podstawie poziomu AI sektora oraz upływu czasu gry.
     */
    static int[] source(Sector s) {
        String[] parts = Core.settings.getString(key(s), "").split(",");
        if (parts.length == 3) {
            try {
                int lvl = Integer.parseInt(parts[0]);
                int pods = Integer.parseInt(parts[1]);
                int score = Integer.parseInt(parts[2]);
                if (lvl > 0) return new int[]{lvl, pods, score};
            } catch (NumberFormatException ignored) {}
        }
        // Symulacja bazowa dla nieodwiedzonych sektorów wroga zależna od poziomu AI i czasu rozgrywki
        AiLevel ai = SectorAi.level(s);
        float minutes = Time.time / (60f * 60f);
        int estScore = (int) ((150 + ai.ordinal() * 180) + minutes * (15 + ai.ordinal() * 12));
        int estCoreLevel = estScore >= 1600 ? 3 : (estScore >= 650 ? 2 : 1);
        int estPods = estScore >= 500 ? Math.min(3, 1 + estScore / 900) : 0;
        return new int[]{estCoreLevel, estPods, estScore};
    }

    /**
     * Dobór jednostek zależy od poziomu rdzenia sektora wysyłającego (odblokowania tech)
     * oraz jego punktów gospodarczych (możliwości produkcyjnych).
     */
    static UnitType[] pool(int coreLevel, int score) {
        if (coreLevel >= 3 && score >= 1500) {
            return new UnitType[]{MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c};
        }
        if (coreLevel >= 2 && score >= 500) {
            return new UnitType[]{MetrumUnits.monoMetT2, MetrumUnits.monoMetT2b, MetrumUnits.monoMetT2c};
        }
        return new UnitType[]{MetrumUnits.monoMetT1b};
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !Vars.state.isCampaign() || !Vars.state.rules.attackMode) return;
        Sector here = Vars.state.rules.sector;
        if (here == null || !BaseExpander.planets.contains(here.planet)) return;
        Team team = Vars.state.rules.waveTeam;

        snapshotTimer += Time.delta;
        if (snapshotTimer >= 60f * 5f) {
            snapshotTimer = 0f;
            snapshot(here, team);
        }

        if (starterSector()) { updateStarter(team); return; }
        if (here.id == here.planet.startSector) return;

        // Odliczamy czas ataku na bazę wroga
        if (underAttackTimer > 0f) {
            underAttackTimer -= Time.delta;
        } else {
            return; // Brak ataku na bazę = wróg nie wzywa posiłków międzysektorowych
        }

        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        Building core = team.core();
        if (core == null) return;

        int alive = Groups.unit.count(u -> u.team == team && !u.dead() && u.flag == reinforcementFlag);
        int remaining = Math.max(0, maxReinforcements - alive);
        if (remaining <= 0) return;

        // Losujemy kolejność sektorów wspierających, aby nie faworyzować zawsze najniższych ID
        Seq<Sector> supporters = new Seq<>();
        for (Sector s : here.planet.sectors) {
            if (s != here && s.hasEnemyBase() && !s.hasBase()) supporters.add(s);
        }
        supporters.shuffle();

        int sent = 0;
        for (Sector s : supporters) {
            if (remaining <= 0) break;
            int[] data = source(s);
            int coreLvl = Mathf.clamp(data[0], 1, 3);
            int pods = Mathf.clamp(data[1], 0, 30);
            int score = data[2];

            // Wzór z ROADMAP.md: max 2 * poziom_rdzenia * liczba_kapsuł
            int maxFromSector = 2 * coreLvl * pods;
            int count = Math.min(remaining, maxFromSector);
            if (count <= 0) continue;

            UnitType[] types = pool(coreLvl, score);
            for (int i = 0; i < count; i++) {
                UnitType type = types[Mathf.random(types.length - 1)];
                if (!type.supportsEnv(Vars.state.rules.env)) continue;
                float x = Mathf.clamp(core.x + Mathf.range(56f), 0f, Vars.world.unitWidth() - 1f);
                float y = Mathf.clamp(core.y + Mathf.range(56f), 0f, Vars.world.unitHeight() - 1f);
                Fx.launchPod.at(x, y);
                Unit spawned = type.spawn(team, x, y);
                spawned.flag = reinforcementFlag;
                // Tankujemy jednostkę przed startem kapsuły, żeby warianty C nie miały -90% prędkości
                FuelSystem.refuel(spawned, FuelSystem.capacity(spawned), 1); 
                Fx.spawnShockwave.at(x, y);
                remaining--;
                sent++;
            }
        }
        if (sent > 0) metrum.ai.planner.task.CoreCapacityTask.drop(team);
        if (sent > 0 && !Vars.headless) {
            Vars.ui.hudfrag.showToast(Core.bundle.format("metrum.pod.enemy", sent));
        }
    }
}
