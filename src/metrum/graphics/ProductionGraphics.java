package metrum.graphics;

import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.UnitType;

/** Production-only icon scaling. Never changes the atlas or the UI portrait. */
public final class ProductionGraphics {
    private ProductionGraphics() {}

    public static TextureRegion icon(Building port, UnitType type) {
        TextureRegion original = type.fullIcon;
        if (original == null || !original.found() || original.width <= 0 || original.height <= 0) return original;
        String name = type.name;
        if (!(name.endsWith("monomet-t3") || name.endsWith("monomet-t3b") ||
              name.endsWith("monomet-t3c") || name.endsWith("monomet-t3d"))) return original;
        float footprint = Math.max(original.width, original.height) * original.scl();
        if (footprint <= 0f) return original;
        float space = Math.max(1f, port.block.size * Vars.tilesize - 4f);
        TextureRegion preview = new TextureRegion(original);
        preview.scale = original.scale * Math.min(0.40f, space / footprint);
        return preview;
    }
}
