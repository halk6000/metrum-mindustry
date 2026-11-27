package metrum.world;

import arc.graphics.g2d.Draw;
import metrum.graphics.ProductionGraphics;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.Reconstructor;

/** Preserves vanilla reconstruction and scales both construction preview icons. */
public class ScaledReconstructor extends Reconstructor {
    public ScaledReconstructor(String name) {
        super(name);
        buildType = ScaledReconstructorBuild::new;
    }

    public class ScaledReconstructorBuild extends ReconstructorBuild {
        @Override public void draw() {
            Draw.rect(region, x, y);
            boolean fallback = true;
            for (int i = 0; i < 4; i++) {
                if (blends(i) && i != rotation) {
                    Draw.rect(inRegion, x, y, i * 90f - 180f);
                    fallback = false;
                }
            }
            if (fallback) Draw.rect(inRegion, x, y, rotation * 90f);
            Draw.rect(outRegion, x, y, rotdeg());
            if (constructing() && hasArrived() && payload != null) {
                UnitType from = payload.unit.type, to = upgrade(from);
                if (to != null) {
                    Draw.draw(Layer.blockOver, () -> {
                        Draw.alpha(1f - progress / constructTime);
                        Draw.rect(ProductionGraphics.icon(this, from), x, y, payload.rotation() - 90f);
                        Draw.reset();
                        Drawf.construct(this, ProductionGraphics.icon(this, to),
                            payload.rotation() - 90f, progress / constructTime, speedScl, time);
                    });
                } else {
                    Draw.z(Layer.blockOver);
                    drawPayload();
                }
            } else {
                Draw.z(Layer.blockOver);
                drawPayload();
            }
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }
    }
}
