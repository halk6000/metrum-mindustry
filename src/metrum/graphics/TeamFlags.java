package metrum.graphics;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;

/** The team emblem (as in the team list: Sharded, Crux, Malis...) above every unit, so it is clear who a drone works for. */
public class TeamFlags {
    public static final String setting = "metrum-team-flags";

    public static void init() {
        if (Vars.headless) return;
        Events.run(Trigger.draw, TeamFlags::draw);
    }

    static void draw() {
        if (!Vars.state.isGame() || !Core.settings.getBool(setting, true)) return;
        float z = Draw.z();
        Draw.z(Layer.overlayUI - 1f);
        Font font = Fonts.outline;
        boolean ints = font.usesIntegerPositions();
        float scaleX = font.getScaleX(), scaleY = font.getScaleY();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.3f / Scl.scl(1f));
        Groups.unit.each(unit -> {
            if (unit.dead() || unit.inFogTo(Vars.player.team())
                || !Core.camera.bounds(arc.util.Tmp.r1).overlaps(unit.x - 20f, unit.y - 20f, 40f, 60f)) return;
            float x = unit.x, y = unit.y + unit.hitSize * 0.6f + 6f + Mathf.absin(Time.time + unit.id * 9f, 12f, 0.8f);
            String emblem = unit.team.emoji;
            if (emblem == null || emblem.isEmpty()) {
                // teams without an emblem get a team-coloured diamond
                Draw.color(Color.black, 0.6f);
                Fill.square(x, y, 3.2f, 45f);
                Draw.color(unit.team.color);
                Fill.square(x, y, 2.4f, 45f);
                Draw.color();
                return;
            }
            font.setColor(unit.team.color);
            font.draw(emblem, x, y + 3f, Align.center);
        });
        font.setColor(Color.white);
        font.getData().setScale(scaleX, scaleY);
        font.setUseIntegerPositions(ints);
        Draw.reset();
        Draw.z(z);
    }
}
