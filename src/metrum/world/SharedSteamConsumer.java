package metrum.world;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.consumers.ConsumeLiquid;

/**
 * Levels the tanks of connected engines once per tick, then every engine burns only its own share.
 * Steam is moved, never copied, and scarce steam is split evenly regardless of update order.
 */
public class SharedSteamConsumer extends ConsumeLiquid {
    public SharedSteamConsumer(Liquid liquid, float amount) {
        super(liquid, amount);
    }

    @Override
    public float efficiency(Building build) {
        if (!(build instanceof SteamEngine.SteamEngineBuild)) return super.efficiency(build);
        SteamEngine.SteamEngineBuild engine = (SteamEngine.SteamEngineBuild) build;
        balance(engine);
        float required = amount * engine.steamMultiplier() * build.edelta();
        return required <= 0f ? 0f : Math.min(1f, build.liquids.get(liquid) / required);
    }

    @Override
    public void update(Building build) {
        if (!(build instanceof SteamEngine.SteamEngineBuild)) {
            super.update(build);
            return;
        }
        float required = amount * ((SteamEngine.SteamEngineBuild) build).steamMultiplier() * build.edelta();
        build.liquids.remove(liquid, Math.min(required, build.liquids.get(liquid)));
    }

    /** The first engine updated in a tick levels the whole group; the others then skip it. */
    private void balance(SteamEngine.SteamEngineBuild engine) {
        long tick = mindustry.Vars.state.updateId;
        if (engine.lastBalance == tick) return;
        if (!engine.grouped()) {
            engine.lastBalance = tick;
            return;
        }
        arc.struct.Seq<SteamEngine.SteamEngineBuild> group = engine.connectedEngines();
        float total = 0f;
        for (SteamEngine.SteamEngineBuild member : group) total += member.liquids.get(liquid);
        float share = total / group.size;
        for (SteamEngine.SteamEngineBuild member : group) {
            member.liquids.set(liquid, share);
            member.lastBalance = tick;
        }
    }
}
