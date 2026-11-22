package metrum.ai;

import mindustry.Vars;
import mindustry.ai.types.FlyingFollowAI;
import mindustry.gen.Building;
import metrum.boost.BoostSystem;

/** Follows the army; when out of boost material, flies back to the nearest core to refill. */
public class BoosterAI extends FlyingFollowAI {
    @Override
    public void updateMovement() {
        int cost = unit.stack.amount > 0 ? BoostSystem.cost(unit.item()) : -1;
        if (cost < 0 || unit.stack.amount < cost) {
            Building core = unit.closestCore();
            if (core != null) {
                moveTo(core, Vars.tilesize * 6f);
                return;
            }
        }
        super.updateMovement();
    }
}
