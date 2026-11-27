package metrum.entities;

import mindustry.Vars;
import mindustry.entities.Fires;
import mindustry.gen.Fire;
import mindustry.world.Tile;

/**
 * Fire that also burns on planets without oxygen (Erekir, Metonia).
 * The game's Fires.create refuses to light anything there, which silently disabled
 * every Metrum fire hazard. Machines and weapons of the mod use this instead.
 */
public final class MetrumFires {
    public static final float lifetime = 1000f;

    private MetrumFires() {}

    public static void create(Tile tile) {
        if (tile == null || Vars.net.client() || !Vars.state.rules.fire) return;
        Fire fire = Fires.get(tile);
        if (fire == null) {
            fire = Fire.create();
            fire.tile = tile;
            fire.lifetime = lifetime;
            fire.set(tile.worldx(), tile.worldy());
            fire.add();
            Fires.register(fire);
        } else {
            fire.lifetime = lifetime;
            fire.time = 0f;
        }
    }
}
