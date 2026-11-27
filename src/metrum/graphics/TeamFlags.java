package metrum.graphics;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;

/** Small waving flag in the team colour above every unit, so it is clear who a drone works for. */
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
        Groups.unit.each(unit -> {
            if (unit.dead() || unit.inFogTo(Vars.player.team()) || !Core.camera.bounds(arc.util.Tmp.r1).overlaps(
                unit.x - 20f, unit.y - 20f, 40f, 60f)) return;
            float x = unit.x - unit.hitSize * 0.35f, y = unit.y + unit.hitSize * 0.6f + 2f;
            float wave = Mathf.sin(Time.time + unit.id * 7f, 6f, 0.8f);
            // pole
            Draw.color(Color.darkGray);
            Lines.stroke(0.8f);
            Lines.line(x, y, x, y + 7f);
            // cloth in the team colour, outlined so red and orange stay readable on rust
            Draw.color(Color.black, 0.6f);
            Fill.tri(x, y + 7.6f, x, y + 2.4f, x + 6.6f, y + 5f + wave);
            Draw.color(unit.team.color);
            Fill.tri(x + 0.3f, y + 7f, x + 0.3f, y + 3f, x + 5.6f, y + 5f + wave);
        });
        Draw.reset();
        Draw.z(z);
    }
}
