package metrum.world;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;

/**
 * Reaction control panel. Placed next to a Fusion Reactor it sets the reaction target:
 * the reactor fuses atoms only up to the target element and stops there; the panel then
 * takes the finished target atoms out of the reactor and outputs them.
 */
public class ReactionControl extends Block {
    public final Seq<Item> targets = new Seq<>();

    public ReactionControl(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        itemCapacity = 20;
        saveConfig = true;
        clearOnDoubleTap = true;
        config(Item.class, (ReactionControlBuild b, Item i) -> b.target = targets.contains(i) ? i : null);
        configClear((ReactionControlBuild b) -> b.target = null);
    }

    public class ReactionControlBuild extends Building {
        public Item target;

        @Override
        public boolean acceptItem(Building source, Item item) {
            return item == target && items.get(item) < itemCapacity;
        }

        @Override
        public void updateTile() {
            if (target != null && items.get(target) > 0) dump(target);
        }

        @Override
        public boolean canDump(Building to, Item item) {
            return !(to instanceof FusionReactor.FusionReactorBuild);
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(ReactionControl.this, table, targets, () -> target, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Object config() {
            return target;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(target == null ? -1 : target.id);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            short id = read.s();
            target = id < 0 ? null : mindustry.Vars.content.item(id);
        }
    }
}
