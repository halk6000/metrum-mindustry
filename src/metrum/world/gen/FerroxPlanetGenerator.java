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
    }
}
