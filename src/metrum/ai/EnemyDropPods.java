package metrum.ai;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Sector;
import mindustry.type.UnitType;
import metrum.content.MetrumUnits;

/**
 * Enemy side of the drop pods: same rule as for the player - pods only land next to the team's OWN core.
 * When you attack an enemy base on a Metrum planet, and the enemy still holds other bases there,
 * it periodically sends reinforcements from those sectors (late game only).
 */
public class EnemyDropPods {
    public static float interval = 60f * 180f; // every 3 minutes
    public static int minEnemyBases = 2;        // "later phases": the enemy must hold other bases
    static float timer;

    public static void init() {
        Events.run(Trigger.update, EnemyDropPods::update);
    }

    static int enemyBasesElsewhere(Sector here) {
        int n = 0;
        for (Sector s : here.planet.sectors) if (s != here && s.hasEnemyBase()) n++;
        return n;
    }

    static void update() {
        if (!Vars.state.isPlaying() || Vars.net.client() || !Vars.state.isCampaign() || !Vars.state.rules.attackMode) return;
        Sector here = Vars.state.rules.sector;
        if (here == null || !BaseExpander.planets.contains(here.planet)) return;
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;
        int others = enemyBasesElsewhere(here);
        if (others < minEnemyBases) return;

        Team team = Vars.state.rules.waveTeam;
        Building core = team.core();
        if (core == null) return;
        // more bases elsewhere -> bigger and heavier drops
        int count = Math.min(2 + others, 8);
        UnitType[] pool = others >= 5
            ? new UnitType[]{MetrumUnits.monoMetT2c, MetrumUnits.monoMetT3b, MetrumUnits.monoMetT3c}
            : new UnitType[]{MetrumUnits.monoMetT1b, MetrumUnits.monoMetT2, MetrumUnits.monoMetT2b};
        for (int i = 0; i < count; i++) {
            UnitType type = pool[Mathf.random(pool.length - 1)];
            float x = core.x + Mathf.range(56f), y = core.y + Mathf.range(56f);
            Time.run(i * 12f, () -> {
                Fx.launchPod.at(x, y);
                type.spawn(team, x, y);
                Fx.spawnShockwave.at(x, y);
            });
        }
        if (!Vars.headless) Vars.ui.hudfrag.showToast(Core.bundle.format("metrum.pod.enemy", others));
    }
}
