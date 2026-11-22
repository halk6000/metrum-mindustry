package metrum.boost;

import arc.Events;
import arc.graphics.Color;
import arc.struct.IntSet;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import metrum.content.MetrumItems;

/**
 * Booster drones: "boosted" = +20% damage and +20% range.
 * Range is applied by stretching the lifetime of bullets fired by boosted units.
 * Cost per boost: 20 of the weakest material, 2 less for each more advanced one (min 2).
 */
public class BoostSystem {
    public static final float rangeMultiplier = metrum.Balance.f("boost.range", 1.2f);
    public static StatusEffect boosted;

    /** weakest -> most advanced */
    public static final Seq<Item> ladder = new Seq<>();
    private static final ObjectIntMap<Item> costs = new ObjectIntMap<>();
    private static final IntSet stretched = new IntSet();

    public static void load() {
        boosted = new StatusEffect("boosted");
        boosted.damageMultiplier = metrum.Balance.f("boost.damage", 1.2f);
        boosted.color = Color.valueOf("7fe3ff");
    }

    public static void init() {
        ladder.addAll(Items.beryllium, MetrumItems.iron, Items.graphite, Items.silicon, MetrumItems.steel,
            Items.tungsten, Items.oxide, Items.thorium, Items.carbide, MetrumItems.berylliumBronze,
            MetrumItems.ferrotungsten, Items.surgeAlloy, MetrumItems.tungstenCarbide, Items.phaseFabric,
            MetrumItems.carborundum, MetrumItems.quarkMatter);
        for (int i = 0; i < ladder.size; i++) costs.put(ladder.get(i), Math.max(metrum.Balance.i("boost.baseCost", 20) - metrum.Balance.i("boost.costStep", 2) * i, metrum.Balance.i("boost.minCost", 2)));

        Events.on(WorldLoadEvent.class, e -> stretched.clear());
        Events.run(Trigger.update, BoostSystem::update);
    }

    /** cost of one boost paid in this item, or -1 if the item can't fuel boosts */
    public static int cost(Item item) {
        return costs.get(item, -1);
    }

    static void update() {
        if (!Vars.state.isPlaying()) return;
        Groups.bullet.each(b -> {
            if (stretched.contains(b.id)) return;
            stretched.add(b.id);
            if (b.owner instanceof Unit && ((Unit) b.owner).hasEffect(boosted)) {
                b.lifetime *= rangeMultiplier;
            }
        });
        if (stretched.size > 4096) {
            IntSet alive = new IntSet();
            Groups.bullet.each(b -> alive.add(b.id));
            stretched.clear();
            stretched.addAll(alive);
        }
    }
}
