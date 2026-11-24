package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.world.draw.DrawBlock;

/** Steel crucible with steam-driven side pumps. */
public class DrawInfusionSmelter extends DrawBlock {
    private final Color edge = Color.valueOf("30343c"), metal = Color.valueOf("656b76"),
        bright = Color.valueOf("a6adb8"), heat = Color.valueOf("ffac64");

    @Override
    public void drawPlan(mindustry.world.Block block, mindustry.entities.units.BuildPlan plan,
                         arc.util.Eachable<mindustry.entities.units.BuildPlan> list) {
        Draw.rect(block.region, plan.drawx(), plan.drawy());
    }

    @Override
    public arc.graphics.g2d.TextureRegion[] icons(mindustry.world.Block block) {
        return new arc.graphics.g2d.TextureRegion[]{block.region};
    }

    @Override
    public void draw(Building build) {
        float x = build.x, y = build.y, stroke = Lines.getStroke();
        Draw.color(edge);
        Fill.square(x, y, 7.8f);
        Draw.color(metal);
        Fill.circle(x, y, 5.5f);
        Fill.rect(x, y + 6f, 11f, 2f);
        Fill.rect(x, y - 6f, 11f, 2f);
        Draw.color(bright);
        Lines.stroke(0.7f);
        Lines.circle(x, y, 4.6f);
        Draw.color(edge);
        Fill.circle(x, y, 3.7f);
        Draw.color(heat);
        Draw.alpha(build.warmup() * (0.8f + Mathf.absin(build.totalProgress(), 5f, 0.2f)));
        Fill.circle(x, y, 3f);
        Draw.color(bright);
        for (int side = -1; side <= 1; side += 2) {
            float cx = x + side * 6f;
            Draw.color(metal);
            Fill.rect(cx, y, 2f, 9f);
            Draw.color(bright);
            float travel = Mathf.sin(build.totalProgress() + side * 8f, 7f, 1.5f) * build.warmup();
            Fill.rect(cx, y + travel, 2.5f, 2.5f);
        }
        Lines.stroke(stroke);
        Draw.reset();
    }
}
