package metrum.graphics;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Unit;
import mindustry.entities.units.WeaponMount;
import mindustry.type.Weapon;
import mindustry.type.unit.ErekirUnitType;

/** Shared graphics base for MonoMet and modular drones; simulation stays in the original UnitType. */
public class DetailedUnitType extends ErekirUnitType {
    public final UnitGraphics graphics;

    public DetailedUnitType(String name) {
        super(name);
        graphics = UnitGraphics.create(name);
    }

    /** UI portraits are independent of world rendering and its visual scale. */
    @Override public void loadIcon() {
        super.loadIcon();
        TextureRegion portrait = Core.atlas.find(name + "-hd-profile");
        if (portrait != null && portrait.found()) {
            fullIcon = portrait;
            uiIcon = portrait;
        } else {
            TextureRegion fallback = Core.atlas.find(name);
            if (fallback != null && fallback.found()) {
                if (fullIcon == null || !fullIcon.found()) fullIcon = fallback;
                if (uiIcon == null || !uiIcon.found()) uiIcon = fullIcon;
            }
        }
    }

    @Override public void load() {
        super.load();
        if (graphics != null) {
            graphics.load(name);
            clipSize = Math.max(clipSize, Math.max(graphics.width, graphics.height) * 2.4f);
            for (Weapon weapon : weapons) graphics.loadWeapon(weapon);
        }
    }

    @Override public void draw(Unit unit) {
        if (!metrum.world.MetrumVisibility.masked(unit)) { super.draw(unit); return; }
        if (unit.inFogTo(mindustry.Vars.player.team())) return;
        float z = Draw.z();
        try {
            Draw.z(flyingLayer);
            Draw.color(); Draw.alpha(0.2f);
            TextureRegion hull = graphics == null ? region : graphics.body(region);
            if (graphics == null) Draw.rect(hull, unit.x, unit.y, unit.rotation - 90f);
            else Draw.rect(hull, unit.x, unit.y, graphics.width, graphics.height, unit.rotation - 90f);
        } finally { Draw.reset(); Draw.z(z); }
    }

    @Override public void drawBody(Unit unit) {
        if (graphics == null) { super.drawBody(unit); return; }
        applyColor(unit);
        graphics.draw(unit, region);
        Draw.reset();
    }

    @Override public void drawOutline(Unit unit) {
        if (graphics == null) { super.drawOutline(unit); return; }
        // The HD hull already includes its own outline; do not draw the obsolete pixel mask.
        if (graphics.detailed()) { Draw.reset(); return; }
        float x = Draw.xscl, y = Draw.yscl;
        // Vanilla drawOutline resets scale before drawing, so draw the original mask explicitly.
        if (outlineRegion != null && outlineRegion.found()) {
            Draw.reset();
            applyColor(unit);
            applyOutlineColor(unit);
            float w = graphics.width * outlineRegion.width * outlineRegion.scale / (region.width * region.scale);
            float h = graphics.height * outlineRegion.height * outlineRegion.scale / (region.height * region.scale);
            Draw.rect(outlineRegion, unit.x, unit.y, w * x, h * y, unit.rotation - 90f);
            Draw.reset();
        }
    }

    @Override public void drawCell(Unit unit) {
        if (graphics == null) { super.drawCell(unit); return; }
        if (cellRegion == null || !cellRegion.found()) return;
        applyColor(unit);
        Draw.color(cellColor(unit));
        float w = graphics.width * cellRegion.width * cellRegion.scale / (region.width * region.scale);
        float h = graphics.height * cellRegion.height * cellRegion.scale / (region.height * region.scale);
        Draw.rect(cellRegion, unit.x, unit.y, w * Draw.xscl, h * Draw.yscl, unit.rotation - 90f);
        Draw.reset();
    }

    @Override public void drawShadow(Unit unit) {
        if (graphics == null || region == null || region.width <= 0 || region.height <= 0) {
            super.drawShadow(unit); return;
        }
        float x = Draw.xscl, y = Draw.yscl;
        TextureRegion original = shadowRegion;
        try {
            // The original method retains floor visibility, altitude and shadow behaviour.
            if (graphics.detailed()) shadowRegion = graphics.body(region);
            TextureRegion basis = graphics.detailed() ? shadowRegion : region;
            Draw.xscl = x * graphics.width / (basis.width * basis.scl());
            Draw.yscl = y * graphics.height / (basis.height * basis.scl());
            super.drawShadow(unit);
        } finally {
            shadowRegion = original;
            Draw.xscl = x; Draw.yscl = y;
        }
    }

    @Override public void drawSoftShadow(float x, float y, float rotation, float alpha) {
        if (graphics == null || region == null) { super.drawSoftShadow(x, y, rotation, alpha); return; }
        float xs = Draw.xscl, ys = Draw.yscl;
        float old = Math.max(region.width, region.height) * region.scl();
        if (old <= 0f) { super.drawSoftShadow(x, y, rotation, alpha); return; }
        float scale = Math.max(graphics.width, graphics.height) / old;
        try {
            Draw.xscl = xs * scale; Draw.yscl = ys * scale;
            super.drawSoftShadow(x, y, rotation, alpha);
        } finally {
            Draw.xscl = xs; Draw.yscl = ys;
        }
    }

    @Override public void drawWeapons(Unit unit) {
        if (graphics == null || !graphics.detailed()) { super.drawWeapons(unit); return; }
        applyColor(unit);
        for (WeaponMount mount : unit.mounts) graphics.drawWeapon(unit, mount);
        Draw.reset();
    }

    @Override public void drawWeaponOutlines(Unit unit) {
        if (graphics == null || !graphics.detailed()) { super.drawWeaponOutlines(unit); return; }
        applyColor(unit);
        applyOutlineColor(unit);
        for (WeaponMount mount : unit.mounts) {
            if (mount.weapon.top || graphics.hasWeaponArt(mount.weapon)) continue;
            float z = Draw.z();
            try {
                Draw.z(z + mount.weapon.layerOffset);
                mount.weapon.drawOutline(unit, mount);
            } finally { Draw.z(z); }
        }
        Draw.reset();
    }

    @Override public void drawEngines(Unit unit) {
        if (graphics == null || !graphics.layeredEngines()) super.drawEngines(unit);
    }

    @Override public void drawItems(Unit unit) {
        // Cargo is visible on the hull instead of an oversized vanilla floating item icon.
        // Retain the original indicator for the controlled unit and for missing cargo artwork.
        if (graphics == null || !graphics.layeredCargo() || unit.isLocal()) super.drawItems(unit);
    }
}
