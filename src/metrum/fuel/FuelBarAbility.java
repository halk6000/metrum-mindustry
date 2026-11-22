package metrum.fuel;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.Unit;
import mindustry.ui.Bar;

/** Invisible ability that only adds the fuel bar to the unit info panel. */
public class FuelBarAbility extends mindustry.entities.abilities.Ability {
    static final String[] grade = {"", "slag", "magma", "plasma"};

    public FuelBarAbility() {
        display = false;
    }

    @Override
    public void displayBars(Unit unit, Table bars) {
        bars.add(new Bar(
            () -> {
                int q = FuelSystem.quality(unit);
                String fuel = Core.bundle.format("metrum.fuel", (int) FuelSystem.fuel(unit), (int) FuelSystem.capacity(unit));
                return q > 0 ? fuel + " [" + Core.bundle.get("metrum.fuel." + grade[q]) + " +" + (q == 1 ? 10 : q == 2 ? 30 : 50) + "%]" : fuel;
            },
            () -> FuelSystem.barColor,
            () -> FuelSystem.capacity(unit) <= 0f ? 0f : FuelSystem.fuel(unit) / FuelSystem.capacity(unit)
        )).growX();
        bars.row();
    }
}
