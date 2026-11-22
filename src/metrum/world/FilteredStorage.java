package metrum.world;

import arc.struct.ObjectSet;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.storage.StorageBlock;

/** Storage that only accepts a fixed set of items (parts magazine, weapons magazine). */
public class FilteredStorage extends StorageBlock {
    public final ObjectSet<Item> allowed = new ObjectSet<>();

    public FilteredStorage(String name, Item... items) {
        super(name);
        allowed.addAll(items);
        coreMerge = false;
    }

    public class FilteredStorageBuild extends StorageBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            return allowed.contains(item) && super.acceptItem(source, item);
        }
    }
}
