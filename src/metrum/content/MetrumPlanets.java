package metrum.content;

import arc.graphics.Color;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Team;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.maps.planet.ErekirPlanetGenerator;
import mindustry.type.Planet;
import mindustry.world.meta.Env;

/** Ferrox - Metrum's industrial planet orbiting the sun beyond Erekir. */
public class MetrumPlanets {
    public static Planet ferrox;
    public static mindustry.type.SectorPreset oldRavine;
    private static Runnable pendingBriefing;

    public static void load() {
        ferrox = new Planet("ferrox", Planets.sun, 1f, 2) {{
            // TODO: own generator (iron-rich terrain); Erekir terrain for now
            generator = new metrum.world.gen.FerroxPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 5);
            cloudMeshLoader = () -> new MultiMesh(
                new HexSkyMesh(this, 3, 0.12f, 0.13f, 5, Color.valueOf("c89a7a").a(0.7f), 2, 0.42f, 1f, 0.43f),
                new HexSkyMesh(this, 7, 0.4f, 0.15f, 5, Color.valueOf("e6c9a8").a(0.6f), 2, 0.42f, 1.2f, 0.45f));
            orbitRadius = 52f;
            sectorSeed = 7;
            startSector = 12;
            alwaysUnlocked = true;
            accessible = true;
            // Expose the game's standard campaign difficulty/settings dialog.
            allowCampaignRules = true;
            visible = true;
            hasAtmosphere = true;
            atmosphereColor = Color.valueOf("b0683f");
            landCloudColor = Color.valueOf("c89a7a");
            iconColor = Color.valueOf("d87a4a");
            defaultEnv = Env.scorching | Env.terrestrial;
            defaultCore = Blocks.coreBastion;
            allowLaunchToNumbered = false;
            // enemies rebuild faster here; smarter base AI comes in a later step
            enemyBuildSpeedMultiplier = 0.6f;
            enemyInfiniteItems = false; // enemy industry must use actual stored and mined resources
            unlockedOnLand.add(Blocks.coreBastion);
            // share Erekir research so Metrum content stays unlocked in one tree
            techTree = Planets.erekir.techTree;
            ruleSetter = r -> {
                r.waveTeam = Team.crux; // red enemy, as placed in the hand-made maps
                r.teams.get(r.waveTeam).infiniteResources = false;
                r.placeRangeCheck = false;
                r.coreDestroyClear = true;
                // smarter enemy: vanilla base builder + RTS squads, plus BaseExpander fortifications
                r.teams.get(r.waveTeam).buildAi = true;
                r.teams.get(r.waveTeam).buildAiTier = 1f;
                r.teams.get(r.waveTeam).rtsAi = true;
                r.teams.get(r.waveTeam).rtsMinSquad = 4;
                r.teams.get(r.waveTeam).aiCoreSpawn = true;
            };
        }};
    
        metrum.content.MetrumProduction.openPitMine.byPlanet.put(ferrox, metrum.content.MetrumProduction.openPitMine.discoveries);

        // first (starting) sector: hand-made map "Old Ravine" (Stary Wawoz) - AI test ground
        oldRavine = new mindustry.type.SectorPreset("old-ravine", ferrox, 12) {{
            alwaysUnlocked = true;
            addStartingItems = true;
            difficulty = 2;
            captureWave = 0; // captured by destroying the enemy core, not by surviving waves
            rules = r -> {
                r.waves = true;
                r.waveTimer = true;
                // the goal of Old Ravine is to destroy the enemy core
                r.attackMode = true;
            };
        }};
        arc.Events.on(mindustry.game.EventType.SectorLaunchEvent.class, e -> {
            if (mindustry.Vars.headless || e.sector.preset != oldRavine) return;
            pendingBriefing = () -> {
                if (!mindustry.Vars.state.isCampaign() || mindustry.Vars.state.rules.sector != e.sector) return;
                mindustry.ui.dialogs.BaseDialog briefing = new mindustry.ui.dialogs.BaseDialog(
                    arc.Core.bundle.get("metrum.ravine.briefing.title")) {{ shouldPause = true; }};
                float width = Math.max(180f, Math.min(560f,
                    arc.Core.graphics.getWidth() / arc.scene.ui.layout.Scl.scl(1f) - 80f));
                briefing.cont.pane(t -> t.add(arc.Core.bundle.get("metrum.ravine.briefing.text"))
                    .width(width).wrap().left()).maxHeight(arc.Core.graphics.getHeight() * 0.65f);
                briefing.buttons.button(arc.Core.bundle.get("metrum.ravine.briefing.start"), briefing::hide).size(260f, 54f);
                briefing.addCloseListener();
                briefing.show();
            };
        });
        arc.Events.run(mindustry.game.EventType.Trigger.update, () -> {
            if (pendingBriefing == null || mindustry.Vars.headless || !mindustry.Vars.state.isGame()
                || mindustry.Vars.ui.loadfrag.shown()) return;
            Runnable ready = pendingBriefing;
            pendingBriefing = null;
            ready.run();
        });
        // the hand-made map has no enemy core: give the AI one on load, as far from the player as possible
        arc.Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> {
            if (mindustry.Vars.state.rules.sector == null || mindustry.Vars.state.rules.sector.preset != oldRavine || mindustry.Vars.net.client()) return;
            mindustry.game.Team ai = mindustry.Vars.state.rules.waveTeam;
            if (!ai.cores().isEmpty()) return;
            mindustry.gen.Building player = mindustry.Vars.state.rules.defaultTeam.core();
            if (player == null) return;
            mindustry.world.Block core = mindustry.content.Blocks.coreBastion;
            mindustry.world.Tile best = null;
            float bestDst = 0f;
            for (mindustry.world.Tile t : mindustry.Vars.world.tiles) {
                if (t.x % 3 != 0 || t.y % 3 != 0) continue;
                if (!mindustry.world.Build.validPlace(core, ai, t.x, t.y, 0, false)) continue;
                float d = player.dst(t);
                if (d > bestDst) { bestDst = d; best = t; }
            }
            if (best != null) {
                best.setNet(core, ai, 0);
                if (best.build != null) best.build.items.add(mindustry.content.Items.beryllium, 2000);
            }
        });
    }
}
