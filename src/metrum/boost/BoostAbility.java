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
    /** containers / vaults this close act as boost-material depots */
    public float depotRange = 8f * Vars.tilesize;
    public Color color = Color.valueOf("7fe3ff");

    /** separate boost-material tank: never mixed with the unit's mining cargo */
    public Item boostItem;
    public int boostAmount;
    public int tankCapacity = 60;

    protected float timer;
    protected float flash;

    @Override
    public void update(Unit unit) {
        flash = Math.max(flash - Time.delta / 30f, 0f);
        refill(unit);
        timer += Time.delta;
        if (timer < reload) return;

        int cost = boostAmount > 0 ? BoostSystem.cost(boostItem) : -1;
        if (cost < 0 || boostAmount < cost) return;

        boolean[] any = {false};
        Units.nearby(unit.team, unit.x, unit.y, range, other -> {
            if (other != unit) any[0] = true;
        });
        if (!any[0]) return;

        timer = 0f;
        boostAmount -= cost;
        if (boostAmount <= 0) {
            boostAmount = 0;
            boostItem = null;
        }
        Units.nearby(unit.team, unit.x, unit.y, range, other -> other.apply(BoostSystem.boosted, duration));
        Fx.healWaveDynamic.at(unit.x, unit.y, range, color);
        flash = 1f;
    }

    public boolean hasCharge() {
        return boostItem != null && boostAmount >= BoostSystem.cost(boostItem);
    }

    /**
     * Refill the boost tank (weakest material first, keeps rare items):
     * 1) from mined cargo if it is boost material, 2) from the core within coreRange, 3) from a container/vault depot.
     */
    void refill(Unit unit) {
        if (hasCharge() || Vars.net.client()) return;
        if (unit.stack.amount > 0 && BoostSystem.cost(unit.item()) > 0 && (boostItem == null || boostItem == unit.item())) {
            int take = Math.min(unit.stack.amount, tankCapacity - boostAmount);
            boostItem = unit.item();
            boostAmount += take;
            unit.stack.amount -= take;
            if (unit.stack.amount <= 0) unit.clearItem();
            if (hasCharge()) return;
        }
        Building core = unit.closestCore();
        if (core == null || !unit.within(core, coreRange)) {
            core = Vars.indexer.findTile(unit.team, unit.x, unit.y, depotRange,
                b -> b instanceof mindustry.world.blocks.storage.StorageBlock.StorageBuild && hasBoostMaterial(b));
            if (core == null) return;
        }
        for (int i = 0; i < BoostSystem.ladder.size; i++) {
            Item it = BoostSystem.ladder.get(i);
            if (boostItem != null && boostItem != it) continue;
            int take = Math.min(core.items.get(it), tankCapacity - boostAmount);
            if (boostAmount + take >= BoostSystem.cost(it)) {
                core.items.remove(it, take);
                boostItem = it;
                boostAmount += take;
                return;
            }
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars) {
        bars.add(new mindustry.ui.Bar(
            () -> arc.Core.bundle.format("metrum.boosttank", boostItem == null ? "-" : boostItem.localizedName, boostAmount, tankCapacity),
            () -> color,
            () -> boostAmount / (float) tankCapacity)).growX();
        bars.row();
    }

    static boolean hasBoostMaterial(Building b) {
        for (Item it : BoostSystem.ladder) if (b.items.get(it) >= BoostSystem.cost(it)) return true;
        return false;
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
