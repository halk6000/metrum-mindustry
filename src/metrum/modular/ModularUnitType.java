package metrum.modular;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Weapon;
import metrum.graphics.DetailedUnitType;
import mindustry.ui.Fonts;

/** A drone built from a hull + modules. Slots = how many modules the hull can carry. */
public class ModularUnitType extends DetailedUnitType {
    public final Item hull;
    public final int slots;

    public ModularUnitType(String name, Item hull, int slots) {
        super(name);
        this.hull = hull;
        this.slots = slots;
    }

    /** weapon that only updates and draws when its module is mounted on the unit */
    public static class ModuleWeapon extends Weapon {
        public Item module;

        public ModuleWeapon(String name, Item module) {
            super(name);
            this.module = module;
        }

        @Override
        public void update(Unit unit, mindustry.entities.units.WeaponMount mount) {
            if (Loadouts.has(unit, module)) super.update(unit, mount);
        }

        @Override
        public void draw(Unit unit, mindustry.entities.units.WeaponMount mount) {
            if (Loadouts.has(unit, module)) super.draw(unit, mount);
        }

        @Override
        public void drawOutline(Unit unit, mindustry.entities.units.WeaponMount mount) {
            if (Loadouts.has(unit, module)) super.drawOutline(unit, mount);
        }
    }

    @Override
    public void draw(Unit unit) {
        super.draw(unit);
        if (metrum.world.MetrumVisibility.masked(unit)) return;
        String n = Loadouts.name(unit);
        if (n == null) return;
        // ship name above the drone
        Draw.z(Layer.playerName);
        Font font = Fonts.outline;
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f));
        font.draw(n, unit.x, unit.y + unit.hitSize / 2f + 6f, 0, Align.center, false);
        font.getData().setScale(1f);
        font.setUseIntegerPositions(ints);
        Draw.reset();
    }
}
