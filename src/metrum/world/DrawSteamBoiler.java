package metrum.world;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.world.draw.DrawBlock;

/** Compact boiler with a firebox and a reciprocating pressure pump. */
public class DrawSteamBoiler extends DrawBlock {
    @Override
    public void drawPlan(mindustry.world.Block block, mindustry.entities.units.BuildPlan plan,
                         arc.util.Eachable<mindustry.entities.units.BuildPlan> list) {
        Draw.rect(block.region, plan.drawx(), plan.drawy());
    }

    @Override
    public arc.graphics.g2d.TextureRegion[] icons(mindustry.world.Block block) {
        return new arc.graphics.g2d.TextureRegion[]{block.region};
    }

    private final Color metal = Color.valueOf("656b76"), edge = Color.valueOf("30343c"),
        highlight = Color.valueOf("a6adb8"), hot = Color.valueOf("ffac64");

    @Override
    public void draw(Building build) {
        float x = build.x, y = build.y;
        float stroke = Lines.getStroke();
        if (build.block.size == 3) {
            Draw.color(edge);
            Fill.square(x, y, 11.8f);
            Draw.color(metal);
            Fill.rect(x, y + 9f, 20f, 3f);
            Fill.rect(x, y - 9f, 20f, 3f);
            for (int side = -1; side <= 1; side += 2) {
                Draw.color(metal);
                Fill.rect(x + side * 10f, y, 2f, 18f);
                Draw.color(highlight);
                float travel = Mathf.sin(build.totalProgress() + side * 9f, 6f, 2f) * build.warmup();
                Fill.rect(x + side * 10f, y + travel, 3f, 4f);
            }
        }
        Draw.color(edge);
        Fill.square(x, y, 7.8f);
        Draw.color(metal);
        Fill.rect(x, y + 1f, 10f, 12f);
        Fill.rect(x - 6f, y, 2f, 11f);
        Fill.rect(x + 6f, y, 2f, 11f);
        Draw.color(highlight);
        Fill.rect(x, y + 5f, 8f, 1f);
        Fill.rect(x, y - 3f, 8f, 1f);
        Draw.color(edge);
        Fill.rect(x, y, 6f, 5f);
        Draw.color(hot);
        Draw.alpha(build.warmup() * (0.75f + Mathf.absin(build.totalProgress(), 4f, 0.25f)));
        Fill.rect(x, y, 4f, 3f);
        Draw.color(highlight);
        float travel = Mathf.sin(build.totalProgress(), 6f, 1.2f) * build.warmup();
        Fill.rect(x + 6f, y + travel, 2.5f, 3f);
        Draw.color(edge);
        Lines.stroke(0.5f);
        for (int i = -1; i <= 1; i++) Lines.line(x - 3f, y + i, x + 3f, y + i);
        Lines.stroke(stroke);
        Draw.reset();
    }
}
