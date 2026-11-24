package metrum.world;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.consumers.ConsumeLiquid;

/** Draws liquid from connected engines without copying or duplicating their inventories. */
public class SharedSteamConsumer extends ConsumeLiquid {
    public SharedSteamConsumer(Liquid liquid, float amount) {
        super(liquid, amount);
    }

    @Override
    public float efficiency(Building build) {
        if (!(build instanceof SteamEngine.SteamEngineBuild)) return super.efficiency(build);
        float available = 0f;
        for (SteamEngine.SteamEngineBuild member : ((SteamEngine.SteamEngineBuild) build).connectedEngines())
            available += member.liquids.get(liquid);
        float required = amount * build.edelta();
        return required <= 0f ? 0f : Math.min(1f, available / required);
    }

    @Override
    public void update(Building build) {
        if (!(build instanceof SteamEngine.SteamEngineBuild)) {
            super.update(build);
            return;
        }
        float remaining = amount * build.edelta();
        for (SteamEngine.SteamEngineBuild member : ((SteamEngine.SteamEngineBuild) build).connectedEngines()) {
            float taken = Math.min(remaining, member.liquids.get(liquid));
            member.liquids.remove(liquid, taken);
            remaining -= taken;
            if (remaining <= 0f) break;
        }
    }
}
