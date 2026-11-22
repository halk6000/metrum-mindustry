package metrum.ai;

import mindustry.Vars;
import mindustry.ai.types.FlyingFollowAI;
import mindustry.gen.Building;
import metrum.boost.BoostSystem;

/** Follows the army; when out of boost material, flies back to the nearest core to refill. */
public class BoosterAI extends FlyingFollowAI {
    @Override
    public void updateMovement() {
        metrum.boost.BoostAbility tank = null;
        for (mindustry.entities.abilities.Ability a : unit.abilities) if (a instanceof metrum.boost.BoostAbility) tank = (metrum.boost.BoostAbility) a;
        if (tank != null && !tank.hasCharge()) {
            Building core = unit.closestCore();
            if (core != null) {
                moveTo(core, Vars.tilesize * 6f);
                return;
            }
        }
        super.updateMovement();
    }
}
