package metrum.world;

import arc.struct.ObjectIntMap;
import mindustry.world.Block;

/**
 * Upgrade families: a block may be built directly over another block of its family with the same size,
 * in either direction (better or weaker), like the vanilla turbine and the advanced turbine.
 */
public final class Upgrades {
    private static final ObjectIntMap<Block> family = new ObjectIntMap<>();
    private static int next = 1;

    private Upgrades() {}

    public static void family(Block... blocks) {
        int id = next++;
        for (Block b : blocks) if (b != null) family.put(b, id);
    }

    public static boolean canReplace(Block self, Block other) {
        if (self == other || other == null || self.size != other.size || !other.replaceable) return false;
        int a = family.get(self, 0);
        return a != 0 && a == family.get(other, 0);
    }
}
