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
            unlockedOnLand.add(Blocks.coreBastion);
            // share Erekir research so Metrum content stays unlocked in one tree
            techTree = Planets.erekir.techTree;
            ruleSetter = r -> {
                r.waveTeam = Team.malis;
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
    
        // first (starting) sector: hand-made map "Old Ravine" (Stary Wawoz) - AI test ground
        oldRavine = new mindustry.type.SectorPreset("old-ravine", ferrox, 12) {{
            alwaysUnlocked = true;
            addStartingItems = true;
            difficulty = 2;
            captureWave = 20;
        }};
    }
}
