package metrum.ai;

import arc.Core;
import arc.Events;
import arc.struct.IntIntMap;
import arc.struct.IntSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.MinerAI;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;

/**
 * "Smart mine" command for Metrum drones. When given, a dialog asks what to mine:
 * nearest ore, the ore the core has least of, or one specific ore.
 */
public class SmartMining {
    public static final int nearest = -1, leastStored = -2;
    public static UnitCommand command;

    /** unit id -> mode (nearest / leastStored / item id) */
    static final IntIntMap modes = new IntIntMap();
    static final IntSet asked = new IntSet();

    public static void init(UnitType... types) {
        command = new UnitCommand("metrum-smart-mine", "production", u -> new SmartMinerAI());
        for (UnitType t : types) {
            if (t.mineTier >= 0 && !t.commands.contains(command)) t.commands.add(command);
        }
        Events.run(Trigger.update, SmartMining::checkNewOrders);
    }

    public static int mode(Unit u) {
        return modes.get(u.id, leastStored);
    }

    /** open the dialog once for freshly ordered units */
    static void checkNewOrders() {
        if (Vars.headless || !Vars.state.isGame() || Vars.control.input.selectedUnits.isEmpty()) return;
        Seq<Unit> fresh = new Seq<>();
        for (Unit u : Vars.control.input.selectedUnits) {
            if (u.controller() instanceof CommandAI && ((CommandAI) u.controller()).command == command) {
                if (!asked.contains(u.id)) fresh.add(u);
            } else {
                asked.remove(u.id);
            }
        }
        if (fresh.isEmpty()) return;
        for (Unit u : fresh) asked.add(u.id);
        showDialog(fresh);
    }

    static void showDialog(Seq<Unit> units) {
        BaseDialog d = new BaseDialog(Core.bundle.get("metrum.mine.title"));
        d.cont.defaults().size(320f, 54f).pad(4f);
        d.cont.button(Core.bundle.get("metrum.mine.nearest"), () -> { set(units, nearest); d.hide(); }).row();
        d.cont.button(Core.bundle.get("metrum.mine.least"), () -> { set(units, leastStored); d.hide(); }).row();
        d.cont.add(Core.bundle.get("metrum.mine.specific")).height(30f).row();
        Seq<Item> items = new Seq<>();
        for (Unit u : units) for (Item it : u.type.mineItems) if (!items.contains(it)) items.add(it);
        d.cont.table(t -> {
            int i = 0;
            for (Item it : items) {
                t.button(b -> {
                    b.image(it.uiIcon).size(32f).padRight(6f);
                    b.add(it.localizedName);
                }, () -> { set(units, it.id); d.hide(); }).size(200f, 50f).pad(3f);
                if (++i % 2 == 0) t.row();
            }
        }).size(420f, 60f * ((items.size + 1) / 2) + 10f).row();
        d.addCloseButton();
        d.show();
    }

    static void set(Seq<Unit> units, int mode) {
        for (Unit u : units) modes.put(u.id, mode);
    }

    public static boolean canStoreMined(mindustry.gen.Building core, Item item) {
        return core != null && core.items != null && item != null
            && core.items.get(item) < core.getMaximumAccepted(item);
    }
    /** MinerAI whose ore choice follows the selected mode */
    public static class SmartMinerAI extends MinerAI {
        float retarget;

        @Override
        public void updateMovement() {
            super.updateMovement();
            Item want = desired();
            if (want != null && targetItem != want) {
                targetItem = want;
                if (ore != null && ore.drop() != want && ore.wallDrop() != want) ore = null;
            }
        }

        protected Item desired() {
            int m = mode(unit);
            if (m >= 0) return Vars.content.item(m);
            // re-evaluated every 2 s
            if ((retarget -= arc.util.Time.delta) > 0f && targetItem != null && canStoreMined(unit.closestCore(), targetItem)) return targetItem;
            retarget = 120f;
            return m == leastStored ? leastStoredOre() : nearestOre();
        }

        /** mineable ore with a reachable deposit that the core has the least of; ties -> alphabetical */
        Item leastStoredOre() {
            mindustry.gen.Building core = unit.closestCore();
            Item best = null;
            int bestAmount = Integer.MAX_VALUE;
            for (Item it : unit.type.mineItems) {
                if (!unit.canMine(it) || !canStoreMined(core, it) || Vars.indexer.findClosestOre(unit, it) == null) continue;
                int amount = core == null ? 0 : core.items.get(it);
                if (best == null || amount < bestAmount
                    || (amount == bestAmount && it.localizedName.compareToIgnoreCase(best.localizedName) < 0)) {
                    best = it;
                    bestAmount = amount;
                }
            }
            // hysteresis: keep the current ore unless another one is clearly scarcer,
            // so drones don't flip-flop every 2 s when stocks are close
            if (targetItem != null && best != null && targetItem != best && core != null
                && unit.canMine(targetItem) && canStoreMined(core, targetItem) && core.items.get(targetItem) <= bestAmount + 20) return targetItem;
            return best;
        }

        Item nearestOre() {
            Item best = null;
            float bestDst = Float.MAX_VALUE;
            for (Item it : unit.type.mineItems) {
                if (!unit.canMine(it) || !canStoreMined(unit.closestCore(), it)) continue;
                Tile t = Vars.indexer.findClosestOre(unit, it);
                if (t != null && unit.dst(t) < bestDst) {
                    bestDst = unit.dst(t);
                    best = it;
                }
            }
            return best;
        }
    }
}
