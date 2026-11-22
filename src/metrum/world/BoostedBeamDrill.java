package metrum.world;

import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.production.BeamDrill;

/**
 * Beam drill with an item booster kept in its own slot: the booster is accepted only as fuel
 * and never dumped/offloaded as if it were mined ore.
 */
public class BoostedBeamDrill extends BeamDrill {
    public Item boostItem;

    public BoostedBeamDrill(String name) {
        super(name);
    }

    public class BoostedBeamDrillBuild extends BeamDrillBuild {
        @Override
        public boolean canDump(Building to, Item item) {
            return item != boostItem && super.canDump(to, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return item == boostItem && items.get(item) < itemCapacity;
        }
    }
}
