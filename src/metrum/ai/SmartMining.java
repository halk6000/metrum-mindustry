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
    public static final int nearest = -1, leastStored = -2, depositsOnly = -3;
    public static UnitCommand command;

    /** unit id -> mode (nearest / leastStored / item id) */
    static final IntIntMap modes = new IntIntMap();
    static final IntSet asked = new IntSet();
    static final Seq<Item> knownOres = new Seq<>();
    // Only smart miners actually waiting for space participate in the buffer.
    static final java.util.Map<Integer, CargoWait> waitingCargo = new java.util.HashMap<>();
    static final float cargoWaitLimit = 120f * 60f;

    static final class CargoWait {
        final Unit unit;
        final Item item;
        float seen;
        CargoWait(Unit unit, Item item) { this.unit = unit; this.item = item; }
    }

    static boolean shouldDiscardCargo(boolean hasRoom, boolean duplicate, float elapsed) {
        return !hasRoom && (duplicate || elapsed >= cargoWaitLimit);
    }

    public static void init(UnitType... types) {
        command = new UnitCommand("metrum-smart-mine", "production", u -> new SmartMinerAI());
        for (UnitType t : types) {
            if (t.mineTier >= 0 && !t.commands.contains(command)) t.commands.add(command);
            for (Item item : t.mineItems) if (!knownOres.contains(item)) knownOres.add(item);
        }
        Events.run(Trigger.update, SmartMining::checkNewOrders);
        Events.on(mindustry.game.EventType.WorldLoadEvent.class, e -> { modes.clear(); asked.clear(); waitingCargo.clear(); });
        Events.on(mindustry.game.EventType.UnitDestroyEvent.class, e -> { modes.remove(e.unit.id, leastStored); asked.remove(e.unit.id); waitingCargo.remove(e.unit.id); });
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
        d.cont.button(Core.bundle.get("metrum.mine.deposits"), () -> { set(units, depositsOnly); d.hide(); }).row();
        d.cont.add(Core.bundle.get("metrum.mine.specific")).height(30f).row();
        Seq<Item> items = new Seq<>();
        items.addAll(knownOres);
        for (Item it : Vars.content.items()) {
            if (!items.contains(it) && (Vars.indexer.hasOre(it) || Vars.indexer.hasWallOre(it))) items.add(it);
        }
        for (Unit u : units) for (Item it : u.type.mineItems) if (!items.contains(it)) items.add(it);
        d.cont.table(t -> {
            int i = 0;
            for (Item it : items) {
                boolean allowed = units.contains(u -> canExtract(u, it));
                t.button(b -> {
                    b.image(it.uiIcon).color(allowed ? arc.graphics.Color.white : arc.graphics.Color.gray).size(32f).padRight(6f);
                    b.add(it.localizedName).color(allowed ? arc.graphics.Color.white : arc.graphics.Color.gray);
                }, () -> { set(units, it.id); d.hide(); }).size(200f, 50f).pad(3f).get().setDisabled(!allowed);
                if (++i % 2 == 0) t.row();
            }
        }).size(420f, 60f * ((items.size + 1) / 2) + 10f).row();
        d.addCloseButton();
        d.show();
    }

    static void set(Seq<Unit> units, int mode) {
        for (Unit u : units) {
            if (mode < 0 || canExtract(u, Vars.content.item(mode))) modes.put(u.id, mode);
        }
    }

    public static boolean canExtract(Unit unit, Item item) {
        return item != null && unit.type.mineItems.contains(item) && unit.canMine(item);
    }

    public static boolean canStoreMined(mindustry.gen.Building core, Item item) {
        return core != null && core.items != null && item != null
            && !burnsItem(core, item) && core.items.get(item) < core.getMaximumAccepted(item);
    }

    public static boolean burnsItem(mindustry.gen.Building building, Item item) {
        return building != null && item != null && !item.buildable
            && building.block instanceof mindustry.world.blocks.storage.CoreBlock
            && ((mindustry.world.blocks.storage.CoreBlock) building.block).incinerateNonBuildable;
    }

    public static int consumerNeed(mindustry.gen.Building building, Item item) {
        if (building == null || item == null || !building.isValid() || !building.enabled
            || building.items == null || burnsItem(building, item) || !building.block.consumesItem(item)) return 0;
        int batch = 0;
        for (mindustry.world.consumers.Consume c : building.block.nonOptionalConsumers) {
            if (c instanceof mindustry.world.consumers.ConsumeItems) {
                for (mindustry.type.ItemStack input : ((mindustry.world.consumers.ConsumeItems)c).items) {
                    if (input.item == item) batch += input.amount;
                }
            }
        }
        int target = batch > 0 ? Math.min(building.getMaximumAccepted(item), batch) : building.getMaximumAccepted(item);
        return Math.max(0, target - building.items.get(item));
    }

    public static mindustry.gen.Building findConsumer(mindustry.gen.Building core, Item item) {
        if (core == null || item == null) return null;
        return Vars.indexer.findTile(core.team, core.x, core.y, 40f * Vars.tilesize,
            b -> b.closestCore() == core && consumerNeed(b, item) > 0);
    }

    public static Tile findMineableOre(Unit unit, Item item) {
        if (!canExtract(unit, item)) return null;
        Tile floorOre = unit.type.mineFloor ? Vars.indexer.findClosestOre(unit, item) : null;
        if (floorOre != null) return floorOre;
        return unit.type.mineWalls ? Vars.indexer.findClosestWallOre(unit, item) : null;
    }
    /** MinerAI whose ore choice follows the selected mode */
    public static class SmartMinerAI extends MinerAI {
        float retarget;
        private mindustry.world.blocks.storage.CoreBlock.CoreBuild home;
        private java.util.function.Supplier<mindustry.type.ItemStack> order;
        private mindustry.entities.units.UnitController previousController;
        private mindustry.gen.Building deliveryTarget;
        private float tungstenStall;
        private int lastTungstenCargo;
        private float blockedCargoTime;
        private Item blockedCargoItem;
        private Item fallbackFrom;

        private void resetCargoWait() {
            waitingCargo.remove(unit.id);
            blockedCargoTime = 0f;
            blockedCargoItem = null;
        }

        private void waitWithCargo(Item cargo) {
            if (Vars.net.client()) return;
            if (blockedCargoItem != cargo) {
                resetCargoWait();
                blockedCargoItem = cargo;
            }
            blockedCargoTime += arc.util.Time.delta;
            float now = arc.util.Time.time;
            waitingCargo.entrySet().removeIf(e -> {
                CargoWait w = e.getValue();
                return w.unit.dead() || !w.unit.isAdded() || w.unit.stack.amount <= 0
                    || w.unit.stack.item != w.item || now - w.seen > 5f;
            });
            CargoWait self = waitingCargo.get(unit.id);
            if (self == null) {
                self = new CargoWait(unit, cargo);
                waitingCargo.put(unit.id, self);
            }
            self.seen = now;
            // Stable keeper: lowest unit id within the same team and resource.
            boolean duplicate = false;
            for (CargoWait other : waitingCargo.values()) {
                if (other.unit != unit && other.unit.team == unit.team && other.item == cargo
                    && other.unit.id < unit.id) { duplicate = true; break; }
            }
            if (!shouldDiscardCargo(false, duplicate, blockedCargoTime)) return;
            unit.clearItem();
            resetCargoWait();
            deliveryTarget = null;
            ore = null;
            targetItem = null;
            retarget = 0f;
            mining = true;
            // Resume an explicit selection once space becomes available again.
            fallbackFrom = mode(unit) >= 0 ? Vars.content.item(mode(unit)) : cargo;
            if (order != null && (demand() == null || !canStoreMined(destinationCore(), cargo))) releaseOrder();
        }

        public void delegate(mindustry.world.blocks.storage.CoreBlock.CoreBuild core,
                             java.util.function.Supplier<mindustry.type.ItemStack> demand,
                             mindustry.entities.units.UnitController previous) {
            home = core;
            order = demand;
            previousController = previous;
        }

        public boolean delegatedTo(mindustry.gen.Building core) { return order != null && home == core; }
        public boolean delegated() { return order != null; }
        public boolean makingTungstenProgress() { return tungstenStall < 1200f; }
        public void releaseOrder() { if (order != null && unit.stack.amount == 0) {
            unit.mineTile = null;
            unit.controller(previousController);
        } }
        private mindustry.type.ItemStack demand() {
            return order != null && home.isValid() && home.team == unit.team ? order.get() : null;
        }
        private mindustry.gen.Building destinationCore() {
            return order != null && home.isValid() && home.team == unit.team ? home : unit.closestCore();
        }
        private boolean safeCore(mindustry.gen.Building core, Item cargo) {
            return core != null && core.isValid() && core.team == unit.team && canStoreMined(core, cargo)
                && core.acceptStack(cargo, unit.stack.amount, unit) > 0;
        }

        private void deliverCargo(mindustry.gen.Building core) {
            mining = false;
            unit.mineTile = null;
            Item cargo = unit.stack.item;
            if (safeCore(deliveryTarget, cargo)) core = deliveryTarget;
            if (!safeCore(core, cargo)) {
                core = null;
                float nearest = Float.MAX_VALUE;
                for (mindustry.gen.Building candidate : unit.team.cores()) {
                    if (!safeCore(candidate, cargo)) continue;
                    float distance = unit.dst2(candidate);
                    if (distance < nearest) { core = candidate; nearest = distance; }
                }
            }
            // A full core is not a reason to remain on an ore tile. Return with the
            // cargo intact and retry when storage becomes available.
            boolean canUnload = core != null;
            if (core == null) {
                core = destinationCore();
                deliveryTarget = null;
                if (core == null || !core.isValid() || core.team != unit.team) return;
            }
            // Weapon range is not a logistics radius; approach the core itself.
            float unloadRange = (core.block.size * 0.5f + 2f) * Vars.tilesize;
            if (canUnload) deliveryTarget = core;
            if (!unit.within(core, unloadRange)) {
                resetCargoWait();
                moveTo(core, unloadRange * 0.8f);
                return;
            }
            if (!canUnload) { waitWithCargo(cargo); return; }
            resetCargoWait();
            int room = Math.max(0, core.getMaximumAccepted(cargo) - core.items.get(cargo));
            int accepted = Math.min(room, Math.min(unit.stack.amount, core.acceptStack(cargo, unit.stack.amount, unit)));
            if (accepted > 0) {
                mindustry.gen.Call.transferItemTo(unit, cargo, accepted, unit.x, unit.y, core);
                if (core == home && cargo == mindustry.content.Items.tungsten) tungstenStall = 0f;
            }
        }

        protected boolean deliverBeforeIncineration(mindustry.gen.Building core) {
            Item cargo = unit.stack.item;
            if (unit.stack.amount <= 0 || !burnsItem(core, cargo)) return false;
            deliverCargo(core);
            return true;
        }

        private int previousMode = Integer.MIN_VALUE;

        private Tile selectedOre(Item item) {
            if (item == null || !canExtract(unit, item)) return null;
            if (order != null || mode(unit) != depositsOnly) return findMineableOre(unit, item);
            Tile best = null;
            float distance = Float.MAX_VALUE;
            for (arc.struct.IntMap.Entry<metrum.world.Deposits.Deposit> e : metrum.world.Deposits.all.entries()) {
                if (e.value.item != item || e.value.amount <= 0) continue;
                Tile t = Vars.world.tile(e.key);
                if (t == null || t.drop() != item || t.block() != mindustry.content.Blocks.air) continue;
                float d = unit.dst2(t);
                if (d < distance) { best = t; distance = d; }
            }
            return best;
        }

        @Override
        public void updateMovement() {
            mindustry.gen.Building core = destinationCore();
            if (unit.stack.amount == 0) { deliveryTarget = null; resetCargoWait(); }
            mindustry.type.ItemStack goal = demand();
            if (order != null) {
                int cargo = unit.stack.item == mindustry.content.Items.tungsten ? unit.stack.amount : 0;
                if (cargo > lastTungstenCargo) tungstenStall = 0f;
                else if (goal != null && goal.item == mindustry.content.Items.tungsten)
                    tungstenStall = Math.min(1200f, tungstenStall + arc.util.Time.delta);
                lastTungstenCargo = cargo;
                if ((goal == null || !canStoreMined(core, goal.item) || !canExtract(unit, goal.item)) && unit.stack.amount == 0) { releaseOrder(); return; }
            }
            if (core == null) { unit.mineTile = null; return; }
            int m = order != null ? (goal == null ? Integer.MIN_VALUE : goal.item.id) : mode(unit);
            if (m != previousMode) { previousMode = m; retarget = 0f; ore = null; targetItem = null; fallbackFrom = null; }
            if (!mining && unit.stack.amount > 0) {
                deliverCargo(core);
                return;
            }
            Item want = desired();
            if (order == null && (m == nearest || m == leastStored || m == depositsOnly || fallbackFrom != null)
                && mining && unit.stack.amount > 0 && targetItem == unit.stack.item
                && canStoreMined(core, targetItem) && ore != null && (unit.validMine(ore, false) && unit.getMineResult(ore) == targetItem)) {
                want = targetItem;
            }
            if (unit.stack.amount > 0) {
                Item cargo = unit.stack.item;
                if (!mining || want != cargo || unit.stack.amount >= unit.type.itemCapacity
                    || !safeCore(core, cargo)
                    || core.getMaximumAccepted(cargo) - core.items.get(cargo) <= unit.stack.amount
                    || (goal != null && goal.item == cargo && unit.stack.amount >= goal.amount - core.items.get(cargo))
                    || ((ore == null || !(unit.validMine(ore, false) && unit.getMineResult(ore) == targetItem)) && findMineableOre(unit, cargo) == null)) {
                    deliverCargo(core);
                    return;
                }
            }
            mining = true;
            if (want == null || !canExtract(unit, want) || !canStoreMined(core, want)) { targetItem = null; ore = null; unit.mineTile = null; return; }
            if (targetItem != want || ore == null || !(unit.validMine(ore, false) && unit.getMineResult(ore) == targetItem)
                || (order == null && m == depositsOnly && !metrum.world.Deposits.all.containsKey(ore.pos()))) {
                targetItem = want;
                ore = selectedOre(want);
            }
            if (ore == null || unit.getMineResult(ore) != want) {
                unit.mineTile = null; ore = null; retarget = 0f; return;
            }
            moveTo(ore, unit.type.mineRange * 0.5f);
            unit.mineTile = unit.within(ore, unit.type.mineRange) && unit.validMine(ore) ? ore : null;
        }

        protected Item desired() {
            if (order != null) {
                mindustry.type.ItemStack goal = demand();
                if (goal == null || !canExtract(unit, goal.item)) return null;
                if (unit.stack.amount > 0 && unit.stack.item != goal.item) return goal.item;
                return canStoreMined(destinationCore(), goal.item) ? goal.item : null;
            }
            int m = mode(unit);
            if (m >= 0) {
                Item item = Vars.content.item(m);
                if (canExtract(unit, item) && canStoreMined(unit.closestCore(), item)) {
                    fallbackFrom = null;
                    return item;
                }
                return fallbackFrom == item ? leastStoredOre() : null;
            }
            // re-evaluated every 2 s
            if ((retarget -= arc.util.Time.delta) > 0f && targetItem != null && canStoreMined(unit.closestCore(), targetItem) && ore != null && (unit.validMine(ore, false) && unit.getMineResult(ore) == targetItem)) return targetItem;
            retarget = 120f;
            return m == nearest ? nearestOre() : leastStoredOre();
        }

        /** mineable ore with a reachable deposit that the core has the least of; ties -> alphabetical */
        Item leastStoredOre() {
            mindustry.gen.Building core = unit.closestCore();
            Item best = null;
            int bestAmount = Integer.MAX_VALUE;
            boolean currentOreAvailable = false;
            for (Item it : unit.type.mineItems) {
                if (!unit.canMine(it) || !canStoreMined(core, it) || selectedOre(it) == null) continue;
                if (it == targetItem) currentOreAvailable = true;
                int amount = core == null ? 0 : core.items.get(it);
                if (best == null || amount < bestAmount
                    || (amount == bestAmount && it.localizedName.compareToIgnoreCase(best.localizedName) < 0)) {
                    best = it;
                    bestAmount = amount;
                }
            }
            // hysteresis: keep the current ore unless another one is clearly scarcer,
            // so drones don't flip-flop every 2 s when stocks are close
            if (currentOreAvailable && core.items.get(targetItem) <= bestAmount + 5) return targetItem;
            return best;
        }

        Item nearestOre() {
            Item best = null;
            float bestDst = Float.MAX_VALUE;
            for (Item it : unit.type.mineItems) {
                if (!unit.canMine(it) || !canStoreMined(unit.closestCore(), it)) continue;
                Tile t = selectedOre(it);
                if (t != null && unit.dst(t) < bestDst) {
                    bestDst = unit.dst(t);
                    best = it;
                }
            }
            return best;
        }
    }
}
