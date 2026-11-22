package metrum.world;

import arc.struct.ObjectIntMap;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.consumers.ConsumeItemDynamic;
import mindustry.world.consumers.ConsumeLiquidsDynamic;

/**
 * The final reconstructor: handles every upgrade from C onwards (B->C, C->D, ...).
 * The resource cost grows exponentially: base cost x {@link #costGrowth}^(level - 1),
 * where level 1 = first upgrade it performs (B->C).
 */
public class WarReconstructor extends Reconstructor {
    public ItemStack[] baseItems = {};
    public Liquid liquid;
    /** liquid per second at level 1 */
    public float baseLiquidPerSecond;
    public float costGrowth = 1.5f;
    /** upgrade target -> level (1 = B->C, 2 = C->D, ...) */
    public final ObjectIntMap<UnitType> levels = new ObjectIntMap<>();

    private static final ItemStack[] none = {};
    private static final LiquidStack[] noLiquid = {};

    public WarReconstructor(String name) {
        super(name);
    }

    /** registers an upgrade pair at the given level */
    public WarReconstructor upgrade(UnitType from, UnitType to, int level) {
        upgrades.add(new UnitType[]{from, to});
        levels.put(to, level);
        return this;
    }

    public float multiplier(int level) {
        return (float) Math.pow(costGrowth, Math.max(level - 1, 0));
    }

    /** call after setting baseItems / liquid */
    public void setupConsumers() {
        if (liquid != null) {
            hasLiquids = true;
            liquidCapacity = Math.max(liquidCapacity, 30f);
        }
        consume(new ConsumeItemDynamic((WarReconstructorBuild b) -> b.itemCost()));
        if (liquid != null) {
            consume(new ConsumeLiquidsDynamic((WarReconstructorBuild b) -> b.liquidCost()));
        }
    }

    public class WarReconstructorBuild extends ReconstructorBuild {
        /** one upgrade per visit: like other factories, the upgraded unit is pushed out, not upgraded again */
        public boolean upgradedHere;

        @Override
        public boolean hasUpgrade(UnitType type) {
            return !upgradedHere && super.hasUpgrade(type);
        }

        @Override
        public void updateTile() {
            UnitType before = payload == null ? null : payload.unit.type;
            super.updateTile();
            if (payload == null) upgradedHere = false;
            else if (before != null && payload.unit.type != before) upgradedHere = true;
        }


        int level() {
            if (payload == null) return 0;
            UnitType to = upgrade(payload.unit.type);
            return to == null ? 0 : levels.get(to, 1);
        }

        public ItemStack[] itemCost() {
            int level = level();
            if (level == 0) return none;
            float m = multiplier(level);
            ItemStack[] out = new ItemStack[baseItems.length];
            for (int i = 0; i < baseItems.length; i++) {
                Item it = baseItems[i].item;
                out[i] = new ItemStack(it, (int) Math.ceil(baseItems[i].amount * m));
            }
            return out;
        }

        /** stock up to twice the highest-level cost of each base item */
        @Override
        public int getMaximumAccepted(Item item) {
            int maxLevel = 1;
            for (ObjectIntMap.Entry<UnitType> e : levels) maxLevel = Math.max(maxLevel, e.value);
            for (ItemStack s : baseItems) {
                if (s.item == item) return (int) Math.ceil(s.amount * multiplier(maxLevel)) * 2;
            }
            return 0;
        }

        /** dynamic liquid consumers don't register a liquid filter, so accept the configured liquid explicitly */
        @Override
        public boolean acceptLiquid(Building source, Liquid l) {
            return l == liquid && liquids.get(l) < liquidCapacity;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.get(item) < getMaximumAccepted(item);
        }

        public LiquidStack[] liquidCost() {
            int level = level();
            if (level == 0) return noLiquid;
            return new LiquidStack[]{new LiquidStack(liquid, baseLiquidPerSecond / 60f * multiplier(level))};
        }
    }
}
