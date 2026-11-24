package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import mindustry.gen.Building;
import mindustry.world.draw.DrawBlock;

/** Twin crushing rollers driven by the separator's working progress. */
public class DrawSalvager extends DrawBlock {
    public boolean overclocked;
    public boolean advanced;
    private final Color edge = Color.valueOf("30343c"), metal = Color.valueOf("656b76"),
        bright = Color.valueOf("a6adb8"), accent = Color.valueOf("c5a66b");

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
        if (advanced) {
            Draw.color(edge);
            Fill.square(x, y, 11.8f);
            Draw.color(metal);
            Fill.rect(x, y + 9f, 21f, 3f);
            Fill.rect(x, y - 9f, 21f, 3f);
            for (int side = -1; side <= 1; side += 2) {
                float cx = x + side * 9.5f;
                Draw.color(metal);
                Fill.rect(cx, y, 3.5f, 13f);
                Draw.color(bright);
                Fill.circle(cx, y, 1.6f);
                Draw.color(edge);
                Lines.stroke(0.6f);
                Lines.lineAngleCenter(cx, y, side * build.totalProgress() * 2f, 2.8f);
                Draw.color(Color.valueOf("82b9b0"));
                Fill.rect(cx, y + 4f, 2f, 1f);
                Fill.rect(cx, y - 4f, 2f, 1f);
            }
        }
        Draw.color(edge);
        Fill.square(x, y, 7.8f);
        Draw.color(metal);
        Fill.rect(x, y + 6f, 13f, 2f);
        Fill.rect(x, y - 6f, 13f, 2f);
        Fill.rect(x - 6f, y, 2f, 10f);
        Fill.rect(x + 6f, y, 2f, 10f);
        for (int side = -1; side <= 1; side += 2) {
            float cx = x + side * 2.7f;
            Draw.color(metal);
            Fill.circle(cx, y, 2.5f);
            Draw.color(bright);
            Lines.stroke(0.6f);
            for (int tooth = 0; tooth < 4; tooth++) {
                float angle = tooth * 90f + side * build.totalProgress() * (overclocked ? 4f : 3f);
                Lines.lineAngle(cx, y, angle, 2.2f);
            }
            Draw.color(accent);
            Fill.circle(cx, y, 0.7f);
        }
        if (overclocked) {
            Draw.color(accent);
            Fill.rect(x - 6f, y, 2f, 10f);
            Fill.rect(x + 6f, y, 2f, 10f);
            Fill.rect(x, y + 6f, 11f, 2f);
            Draw.color(edge);
            Lines.stroke(0.8f);
            for (int mark = -1; mark <= 1; mark++) {
                float mx = x + mark * 3f;
                Lines.line(mx - 0.6f, y + 5.2f, mx + 0.6f, y + 6.8f);
            }
            Draw.color(edge);
            Fill.rect(x, y - 6f, 6f, 2.5f);
            Draw.color(bright);
            Lines.stroke(0.5f);
            for (int fin = -1; fin <= 1; fin++)
                Lines.line(x + fin * 1.5f, y - 7f, x + fin * 1.5f, y - 5f);
        }
        Lines.stroke(stroke);
        Draw.reset();
    }
}
