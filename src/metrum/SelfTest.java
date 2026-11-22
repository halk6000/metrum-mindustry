package metrum;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.UnitPayload;
import metrum.content.MetrumUnitBlocks;
import metrum.content.MetrumUnits;

/**
 * Runs only when saves/metrum-selftest exists: builds a tiny world, places the drone line
 * and asks each reconstructor whether it accepts the drone from the previous stage.
 * Results go to metrum-trace.txt.
 */
public class SelfTest {
    public static void runIfRequested() {
        if (!Vars.dataDirectory.child("metrum-selftest").exists()) return;
        StringBuilder sb = new StringBuilder("SELFTEST");
        try {
            Vars.world.loadGenerator(40, 20, tiles -> tiles.fill());
            for (Tile t : Vars.world.tiles) t.setFloor(Blocks.rhyolite.asFloor());
            Vars.state.rules.infiniteResources = true;
            Team team = Team.sharded;
            check(sb, team, MetrumUnitBlocks.airMinerRebuilder, MetrumUnits.monoMetT1, 5);
            check(sb, team, MetrumUnitBlocks.warReconstructor, MetrumUnits.monoMetT1b, 15);
            check(sb, team, MetrumUnitBlocks.warReconstructorD, MetrumUnits.monoMetT1c, 27);
        } catch (Throwable t) {
            sb.append(" ERROR ").append(t);
        } finally {
            try { Vars.logic.reset(); } catch (Throwable ignored) {}
        }
        MetrumMod.mark(sb.toString());
    }

    static void check(StringBuilder sb, Team team, Block reconstructor, UnitType unit, int x) {
        // source (dock) on the left facing right, reconstructor to its right facing right
        Tile src = Vars.world.tile(x, 10);
        src.setBlock(MetrumUnitBlocks.airMinerDock, team, 0);
        int rx = x + (MetrumUnitBlocks.airMinerDock.size + reconstructor.size) / 2;
        Tile dst = Vars.world.tile(rx, 10);
        dst.setBlock(reconstructor, team, 0);
        Building b = dst.build;
        boolean ok = b != null && b.acceptPayload(src.build, new UnitPayload(unit.create(team)));
        sb.append(" | ").append(reconstructor.name).append(" <- ").append(unit.name).append(": ").append(ok ? "ACCEPT" : "REJECT");
    }
}
