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

    /** optional HD art: <block>-frame-0..15, -flame-0..3, -steam-0..3 (any size, drawn on the block footprint) */
    private arc.graphics.g2d.TextureRegion[] frames, flames, steams;

    @Override
    public void load(mindustry.world.Block block) {
        frames = regions(block.name + "-frame-", 16);
        flames = regions(block.name + "-flame-", 4);
        steams = regions(block.name + "-steam-", 4);
    }

    private static arc.graphics.g2d.TextureRegion[] regions(String prefix, int max) {
        arc.struct.Seq<arc.graphics.g2d.TextureRegion> found = new arc.struct.Seq<>();
        for (int i = 0; i < max; i++) {
            arc.graphics.g2d.TextureRegion r = arc.Core.atlas.find(prefix + i);
            if (!r.found()) break;
            found.add(r);
        }
        return found.toArray(arc.graphics.g2d.TextureRegion.class);
    }

    /** HD frames: loop 0-3 while working, frame 0 when idle; flame and steam layers fade with warm-up */
    private boolean drawFrames(Building build) {
        if (frames == null || frames.length == 0) return false;
        float w = build.block.size * mindustry.Vars.tilesize, warm = build.warmup(), t = build.totalProgress();
        int loop = Math.min(4, frames.length);
        Draw.rect(frames[warm > 0.1f ? (int) (t / 6f) % loop : 0], build.x, build.y, w, w);
        if (flames.length > 0 && warm > 0.01f) {
            Draw.alpha(warm);
            Draw.rect(flames[(int) (t / 5f) % flames.length], build.x, build.y, w, w);
        }
        if (steams.length > 0 && warm > 0.3f) {
            float z = Draw.z();
            Draw.z(mindustry.graphics.Layer.effect);
            Draw.alpha(warm);
            Draw.rect(steams[(int) (t / 8f) % steams.length], build.x, build.y, w, w);
            Draw.z(z);
        }
        Draw.reset();
        return true;
    }

    @Override
    public void draw(Building build) {
        if (drawFrames(build)) return;
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
