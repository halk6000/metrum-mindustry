package metrum.world.gen;

import arc.graphics.Color;
import arc.math.geom.Vec3;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.maps.planet.ErekirPlanetGenerator;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * Ferrox terrain: Erekir's layout, re-skinned into a rusty red world.
 * Pale stone becomes red stone / rhyolite, both on the globe and on sector maps.
 */
public class FerroxPlanetGenerator extends ErekirPlanetGenerator {
    static final Color rust = Color.valueOf("b0552f");
    final ObjectMap<Block, Block> floors = new ObjectMap<>(), walls = new ObjectMap<>();

    public FerroxPlanetGenerator() {
        floors.put(Blocks.regolith, Blocks.redStone);
        floors.put(Blocks.yellowStone, Blocks.rhyolite);
        floors.put(Blocks.yellowStonePlates, Blocks.rhyoliteCrater);
        floors.put(Blocks.carbonStone, Blocks.rhyolite);
        walls.put(Blocks.regolithWall, Blocks.redStoneWall);
        walls.put(Blocks.yellowStoneWall, Blocks.rhyoliteWall);
        walls.put(Blocks.carbonWall, Blocks.rhyoliteWall);
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        super.getColor(position, out);
        out.lerp(rust, 0.45f);
    }

    @Override
    protected void generate() {
        super.generate();
        for (Tile tile : Vars.world.tiles) {
            Block f = floors.get(tile.floor());
            if (f != null) tile.setFloor(f.asFloor());
            Block w = walls.get(tile.block());
            if (w != null) tile.setBlock(w);
        }
        addVents();
        addIronOre();
    }

    /** iron ore veins on Ferrox's red ground */
    void addIronOre() {
        Block ore = metrum.content.MetrumBlocks.oreIron;
        int seed = Vars.state.rules.sector == null ? 3 : Vars.state.rules.sector.id;
        for (Tile tile : Vars.world.tiles) {
            if (tile.block() != Blocks.air || tile.overlay() != Blocks.air || tile.floor().isLiquid) continue;
            if (tile.floor() != Blocks.redStone && tile.floor() != Blocks.rhyolite) continue;
            float n = arc.util.noise.Simplex.noise2d(seed, 3, 0.5f, 1f / 18f, tile.x, tile.y);
            if (n > 0.7f) tile.setOverlay(ore);
        }
    }

    /** sprinkle extra rhyolite steam vents on open ground (Ferrox is volcanically active) */
    void addVents() {
        arc.math.Rand r = new arc.math.Rand(Vars.state.rules.sector == null ? 1 : Vars.state.rules.sector.id * 7919L);
        int w = Vars.world.width(), h = Vars.world.height();
        int target = Math.max(2, w * h / 3000), placed = 0;
        for (int attempt = 0; attempt < target * 40 && placed < target; attempt++) {
            int x = r.random(4, w - 5), y = r.random(4, h - 5);
            if (!ventFits(x, y)) continue;
            for (arc.math.geom.Point2 p : mindustry.world.blocks.environment.SteamVent.offsets) {
                Vars.world.tile(x + p.x, y + p.y).setFloor(Blocks.rhyoliteVent.asFloor());
            }
            placed++;
        }
    }

    boolean ventFits(int x, int y) {
        for (arc.math.geom.Point2 p : mindustry.world.blocks.environment.SteamVent.offsets) {
            Tile t = Vars.world.tile(x + p.x, y + p.y);
            if (t == null || t.block() != Blocks.air || t.floor().isLiquid || t.floor() instanceof mindustry.world.blocks.environment.SteamVent || t.overlay() == Blocks.spawn) return false;
        }
        // keep clear of cores and enemy spawns
        Tile c = Vars.world.tile(x, y);
        for (mindustry.game.Team team : mindustry.game.Team.baseTeams) {
            for (mindustry.world.blocks.storage.CoreBlock.CoreBuild core : team.cores()) {
                if (core.tile.dst(c) < 12f * Vars.tilesize) return false;
            }
        }
        return true;
    }
}
