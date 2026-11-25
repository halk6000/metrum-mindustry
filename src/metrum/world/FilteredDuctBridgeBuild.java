package metrum.world;

import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.DirectionalUnloader.DirectionalUnloaderBuild;
import mindustry.world.blocks.distribution.DuctBridge;

public class FilteredDuctBridgeBuild extends DuctBridge.DuctBridgeBuild {
    public FilteredDuctBridgeBuild(DuctBridge bridge) {
        bridge.super();
    }

    public static void installOnDuctBridge() {
        DuctBridge bridge = (DuctBridge) Blocks.ductBridge;
        bridge.buildType = () -> new FilteredDuctBridgeBuild(bridge);
    }

    @Override
    public void updateTile() {
        if (!items.any()) {
            super.updateTile();
            return;
        }
        // Leave one matching item available for each working side unloader's next cycle.
        Item left = reserveForUnloader((rotation + 1) % 4);
        Item right = reserveForUnloader((rotation + 3) % 4);
        try {
            super.updateTile();
        } finally {
            if (left != null) items.add(left, 1);
            if (right != null) items.add(right, 1);
        }
    }

    private Item reserveForUnloader(int side) {
        Building neighbor = nearby(side);
        if (!(neighbor instanceof DirectionalUnloaderBuild)) return null;
        DirectionalUnloaderBuild unloader = (DirectionalUnloaderBuild) neighbor;
        Item filter = unloader.unloadItem;
        if (unloader.rotation != side || unloader.team != team || !unloader.enabled
            || unloader.edelta() <= 0f || filter == null || !items.has(filter)) return null;
        Building output = unloader.front();
        if (output == null || output.team != team || !output.acceptItem(unloader, filter)) return null;
        items.remove(filter, 1);
        return filter;
    }
}
