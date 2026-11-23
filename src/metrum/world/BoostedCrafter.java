package metrum.world;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Generic crafter with an optional item booster kept in its own slot:
 * when a booster item is present, each craft uses 1 booster and yields {@link #boostMultiplier} x output.
 */
public class BoostedCrafter extends GenericCrafter {
    public Item boostItem;
    public float boostMultiplier = 2f;

    public BoostedCrafter(String name) {
        super(name);
    }

    @Override
    public void setStats() {
        super.setStats();
        if (boostItem != null) stats.add(Stat.booster, t -> {
            t.image(boostItem.uiIcon).size(24f).padRight(4f);
            t.add("x" + (int) boostMultiplier + " " + StatUnit.items.localized());
        });
    }

    public class BoostedCrafterBuild extends GenericCrafterBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (item == boostItem) return items.get(item) < itemCapacity;
            return super.acceptItem(source, item);
        }

        @Override
        public void craft() {
            boolean boosted = boostItem != null && items.get(boostItem) > 0;
            super.craft();
            if (boosted && outputItems != null) {
                items.remove(boostItem, 1);
                for (ItemStack out : outputItems) {
                    int extra = Math.round(out.amount * (boostMultiplier - 1f));
                    for (int i = 0; i < extra; i++) offload(out.item);
                }
            }
        }

        @Override
        public boolean canDump(Building to, Item item) {
            return item != boostItem && super.canDump(to, item);
        }
    }
}
