package metrum.world;

import arc.graphics.g2d.Draw;
import metrum.graphics.ProductionGraphics;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.blocks.units.UnitFactory;

/** Same factory mechanics; only the construction preview is resized. */
public class ScaledUnitFactory extends UnitFactory {
    public ScaledUnitFactory(String name) {
        super(name);
        buildType = ScaledFactoryBuild::new;
    }

    public class ScaledFactoryBuild extends UnitFactoryBuild {
        @Override public void draw() {
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());
            if (currentPlan >= 0 && currentPlan < plans.size) {
                UnitPlan plan = plans.get(currentPlan);
                Draw.draw(Layer.blockOver, () -> Drawf.construct(this,
                    ProductionGraphics.icon(this, plan.unit), rotdeg() - 90f,
                    progress / plan.time, speedScl, time));
            }
            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }
    }
}
