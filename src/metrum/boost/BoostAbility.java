package metrum.boost;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.type.Item;

/**
 * Carried by booster drones. Every {@link #reload} ticks, if allies are in range,
 * pays the boost cost from the carried item stack and applies {@link BoostSystem#boosted}.
 * Refills its stack from the team core when close to it.
 */
public class BoostAbility extends mindustry.entities.abilities.Ability {
    public float range = 10f * Vars.tilesize;
    public float reload = 600f;
    public float duration = 600f;
    public float coreRange = 20f * Vars.tilesize;
    public Color color = Color.valueOf("7fe3ff");

    protected float timer;
    protected float flash;

    @Override
    public void update(Unit unit) {
        flash = Math.max(flash - Time.delta / 30f, 0f);
        refill(unit);
        timer += Time.delta;
        if (timer < reload) return;

        Item item = unit.item();
        int cost = unit.stack.amount > 0 ? BoostSystem.cost(item) : -1;
        if (cost < 0 || unit.stack.amount < cost) return;

        boolean[] any = {false};
        Units.nearby(unit.team, unit.x, unit.y, range, other -> {
            if (other != unit) any[0] = true;
        });
        if (!any[0]) return;

        timer = 0f;
        unit.stack.amount -= cost;
        if (unit.stack.amount <= 0) unit.clearItem();
        Units.nearby(unit.team, unit.x, unit.y, range, other -> other.apply(BoostSystem.boosted, duration));
        Fx.healWaveDynamic.at(unit.x, unit.y, range, color);
        flash = 1f;
    }

    /** take the weakest boost material the core has (keeps rare items), if nearby and empty-handed */
    void refill(Unit unit) {
        if (unit.stack.amount > 0 || Vars.net.client()) return;
        Building core = unit.closestCore();
        if (core == null || !unit.within(core, coreRange)) return;
        for (int i = 0; i < BoostSystem.ladder.size; i++) {
            Item it = BoostSystem.ladder.get(i);
            int take = Math.min(core.items.get(it), unit.type.itemCapacity);
            if (take >= BoostSystem.cost(it)) {
                core.items.remove(it, take);
                unit.addItem(it, take);
                return;
            }
        }
    }

    @Override
    public void draw(Unit unit) {
        if (flash <= 0f) return;
        Draw.color(color, flash * 0.6f);
        Lines.stroke(1.5f);
        Lines.circle(unit.x, unit.y, range);
        Draw.reset();
    }

    @Override
    public void addStats(Table t) {
        super.addStats(t);
        t.add("[lightgray]+20% dmg / +20% range, r=" + (int) (range / Vars.tilesize)
            + ", " + (int) (reload / 60f) + "s").row();
    }
}
