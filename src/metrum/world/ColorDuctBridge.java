package metrum.world;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.graphics.Layer;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.DuctBridge;

/**
 * Duct bridge with circuit colours: a bridge only links to the next bridge of the SAME colour
 * (bridges of other colours on the way are skipped), so parallel lines no longer merge and clog.
 * Uses the vanilla duct-bridge art, tinted with the circuit colour.
 */
public class ColorDuctBridge extends DuctBridge {
    public static final Color[] colors = {
        Color.valueOf("ffd37f"), Color.valueOf("ff5a5a"), Color.valueOf("5ad1ff"),
        Color.valueOf("7cff7a"), Color.valueOf("c88cff"), Color.valueOf("f0f0f0")};

    public ColorDuctBridge(String name) {
        super(name);
        configurable = true;
        saveConfig = true;
        config(Integer.class, (ColorBridgeBuild b, Integer c) -> b.circuit = Math.floorMod(c, colors.length));
    }

    @Override
    public void load() {
        super.load();
        // borrow the vanilla duct bridge art
        region = Core.atlas.find("duct-bridge");
        bridgeRegion = Core.atlas.find("duct-bridge-bridge");
        bridgeBotRegion = Core.atlas.find("duct-bridge-bridge-bottom");
        bridgeLiquidRegion = Core.atlas.find("duct-bridge-bridge-liquid");
        arrowRegion = Core.atlas.find("duct-bridge-arrow");
        dirRegion = Core.atlas.find("duct-bridge-dir");
    }

    public class ColorBridgeBuild extends DuctBridgeBuild {
        public int circuit;

        @Override
        public DirectionBridgeBuild findLink() {
            for (int i = 1; i <= range; i++) {
                Tile other = tile.nearby(arc.math.geom.Geometry.d4x(rotation) * i, arc.math.geom.Geometry.d4y(rotation) * i);
                if (other != null && other.build instanceof ColorBridgeBuild && other.build.block == block && other.build.team == team
                    && ((ColorBridgeBuild) other.build).circuit == circuit) {
                    return (DirectionBridgeBuild) other.build;
                }
            }
            return null;
        }

        @Override
        public void buildConfiguration(Table table) {
            table.background(Tex.pane);
            for (int i = 0; i < colors.length; i++) {
                int c = i;
                table.button(b -> b.image(Tex.whiteui).color(colors[c]).size(26f), () -> configure(c))
                    .size(40f).checked(b -> circuit == c);
            }
        }

        @Override
        public Object config() {
            return circuit;
        }

        @Override
        public void draw() {
            super.draw();
            Draw.z(Layer.blockOver);
            Draw.color(colors[circuit]);
            Fill.square(x, y, 1.4f, 45f);
            Draw.reset();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.b(circuit);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            circuit = read.b();
        }
    }
}
