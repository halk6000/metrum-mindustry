package metrum.graphics;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import arc.struct.ObjectMap;
import mindustry.entities.units.WeaponMount;
import metrum.Balance;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import mindustry.type.UnitType;

/**
 * Purely visual unit model. Coordinates and sizes are in world units or normalized
 * hull coordinates, never determined by the HD texture's pixel count.
 * No simulation state, network state or save data is added by this renderer.
 */
public final class UnitGraphics {
    private static final String SHARED = "metrum-ship-hd-";
    private static final float T3_VISUAL_SCALE = 0.70f;
    private static final int[] PULSE = {0, 1, 2, 3, 4, 5, 6, 2, 1};

    public final String key;
    public final float width, height;
    private final int variant;
    private final float reactorY, reactorSize, cargoFront, cargoRear;
    private TextureRegion hull, cargo, miningLaser;
    private final TextureRegion[] reactor = new TextureRegion[7];
    private final TextureRegion[] exhaust = new TextureRegion[5];
    private boolean completeReactor, completeExhaust, frontCombatLaser;
    private final ObjectMap<Weapon, TextureRegion> weaponArt = new ObjectMap<>();
    private final ObjectMap<Weapon, TextureRegion> openHatches = new ObjectMap<>();
    private TextureRegion missingOutline;

    private UnitGraphics(String key, float width, float height, int variant,
                         float reactorY, float reactorSize, float cargoFront, float cargoRear) {
        this.key = key;
        float visualScale = variant >= 0 ? T3_VISUAL_SCALE : 1f;
        this.width = dimension("graphics." + key + ".width", width) * visualScale;
        this.height = dimension("graphics." + key + ".height", height) * visualScale;
        this.variant = variant;
        this.reactorY = reactorY;
        this.reactorSize = reactorSize;
        this.cargoFront = cargoFront;
        this.cargoRear = cargoRear;
    }

    private static float dimension(String key, float fallback) {
        float value = Balance.f(key, fallback);
        return Float.isNaN(value) || Float.isInfinite(value) || value <= 0f ? fallback : value;
    }

    /** Defaults reproduce the world footprint of the original sprites at 4 px/world unit. */
    public static UnitGraphics create(String name) {
        switch (name) {
            case "monomet-t1": return basic(name, 14.75f, 16f);
            case "monomet-t1b": case "monomet-t2": case "monomet-t2b":
                return basic(name, 16f, 16f);
            case "monomet-t1c": case "monomet-t2c": return basic(name, 20f, 20f);
            case "monomet-t1d": case "monomet-t2d": return basic(name, 25f, 25f);
            case "modular-light": return basic(name, 12f, 12f);
            case "modular-medium": return basic(name, 16f, 16f);
            case "modular-heavy": return basic(name, 22f, 22f);
            case "monomet-t3": return new UnitGraphics(name, 30f, 49.75f, 0, .347f, .37f, .59f, .70f);
            case "monomet-t3b": return new UnitGraphics(name, 35f, 59f, 1, .337f, .36f, .576f, .686f);
            case "monomet-t3c": return new UnitGraphics(name, 38.75f, 64.75f, 2, .330f, .36f, .558f, .661f);
            case "monomet-t3d": return new UnitGraphics(name, 45f, 74.25f, 3, .339f, .37f, .583f, .692f);
            default: return null;
        }
    }

    private static UnitGraphics basic(String name, float width, float height) {
        return new UnitGraphics(name, width, height, -1, 0f, 0f, 0f, 0f);
    }

    private static TextureRegion find(String name) {
        TextureRegion region = Core.atlas.find(name);
        return region != null && region.found() ? region : null;
    }

    /** Called after UnitType.load(), using the mod-prefixed content name. */
    public void load(String contentName) {
        weaponArt.clear();
        openHatches.clear();
        missingOutline = Core.atlas.find(SHARED + "unused-outline");
        hull = find(contentName + "-hd-hull");
        if (variant < 0 || hull == null) return;
        cargo = find(SHARED + "cargo");
        miningLaser = find(contentName + "-hd-minelaser");
        completeReactor = true;
        for (int i = 0; i < reactor.length; i++) {
            reactor[i] = find(contentName + "-hd-reactor-" + (i + 1));
            completeReactor &= reactor[i] != null;
        }
        completeExhaust = true;
        for (int i = 0; i < exhaust.length; i++) {
            exhaust[i] = find(SHARED + "exhaust-" + (i + 1));
            completeExhaust &= exhaust[i] != null;
        }
    }

    public boolean detailed() { return hull != null; }
    public boolean layeredCargo() { return detailed() && cargo != null && variant >= 0; }
    public boolean layeredEngines() { return detailed() && completeExhaust && variant >= 0; }
    public TextureRegion body(TextureRegion original) { return detailed() ? hull : original; }

    /** Mount existing weapons in actual hardpoints, before vanilla mirrors weapon definitions. */
    public void mountWeapons(UnitType type) {
        if (variant < 0) return;
        int missiles = 0, lasers = 0;
        for (Weapon weapon : type.weapons) {
            String name = weapon.name;
            if (name == null) continue;
            // Scale barrel offsets and recoil with the resized hardpoints.
            weapon.shootX *= T3_VISUAL_SCALE;
            weapon.shootY *= T3_VISUAL_SCALE;
            weapon.recoil *= T3_VISUAL_SCALE;
            if (name.endsWith("c1-mass-weapon")) {
                mount(weapon, .25f, .43f);
            } else if (name.endsWith("missile-put")) {
                if (missiles++ == 0) mount(weapon, .11f, .64f);
                else mount(weapon, .20f, .25f);
            } else if (name.endsWith("missile-pod-heavy")) {
                mount(weapon, .11f, .64f);
            } else if (name.endsWith("torpedo-launcher")) {
                // D has two forward bays flanking the mining head. C retains its existing torpedoes.
                mount(weapon, variant == 3 ? .28f : .20f, variant == 3 ? .18f : .25f);
            } else if (name.endsWith("fire-laser")) {
                mount(weapon, .5f, lasers++ == 0 ? .11f : .465f);
            }
        }
        frontCombatLaser = lasers > 0;
    }

    private void mount(Weapon weapon, float leftU, float v) {
        float sign = weapon.x < 0f ? -1f : 1f;
        weapon.x = sign * (.5f - leftU) * width;
        weapon.y = (.5f - v) * height;
    }

    /** Mindustry still renders the actual weapons, including rotation, mirror and recoil. */
    public void loadWeapon(Weapon weapon) {
        if (variant < 0 || !detailed() || weapon.name == null || weapon.region == null || !weapon.region.found()) return;
        TextureRegion fallback = new TextureRegion(weapon.region);
        fallback.scale *= T3_VISUAL_SCALE;
        weaponArt.put(weapon, fallback);
        String sprite = null;
        float worldHeight = 0f;
        if (weapon.name.endsWith("c1-mass-weapon")) {
            sprite = "gun"; worldHeight = height * .125f;
        } else if (weapon.name.endsWith("missile-put")) {
            sprite = "rocket-b"; worldHeight = height * .145f;
        } else if (weapon.name.endsWith("missile-pod-heavy")) {
            sprite = variant == 3 ? "rocket-d" : "rocket-c"; worldHeight = height * .17f;
        } else if (weapon.name.endsWith("fire-laser")) {
            if (miningLaser != null) replaceWeaponArt(weapon, miningLaser,
                height * (weapon.y > height * .2f ? .155f : .10f));
            return;
        } else if (variant == 3 && weapon.name.endsWith("torpedo-launcher")) {
            TextureRegion closed = find(SHARED + "torpedo-closed");
            TextureRegion open = find(SHARED + "torpedo-open");
            if (closed != null && open != null) {
                float size = height * .09f;
                replaceWeaponArt(weapon, closed, size);
                openHatches.put(weapon, scaled(open, size));
            }
            return;
        }
        if (sprite != null) {
            TextureRegion replacement = find(SHARED + sprite);
            if (replacement != null) replaceWeaponArt(weapon, replacement, worldHeight);
        }
    }

    private static TextureRegion scaled(TextureRegion source, float worldHeight) {
        // Private region: never change the shared atlas region's scale. Mindustry's
        // standard world sprite conversion is 4 pixels to one world unit.
        TextureRegion copy = new TextureRegion(source);
        copy.scale = worldHeight * 4f / Math.max(1, source.height);
        return copy;
    }

    private void replaceWeaponArt(Weapon weapon, TextureRegion source, float worldHeight) {
        // Keep real atlas regions on the weapon during content/icon packing. The
        // private scaled region is installed only for the duration of a world draw.
        weaponArt.put(weapon, scaled(source, worldHeight));
    }

    public boolean hasWeaponArt(Weapon weapon) { return weaponArt.containsKey(weapon); }

    public void drawWeapon(Unit unit, WeaponMount mount) {
        Weapon weapon = mount.weapon;
        TextureRegion art = weaponArt.get(weapon);
        if (art == null) { weapon.draw(unit, mount); return; }
        TextureRegion open = openHatches.get(weapon);
        if (open != null && !unit.dead() && ((mount.shoot && mount.reload <= 12f)
            || (mount.recoil > .05f && mount.reload >= weapon.reload - 12f))) art = open;
        TextureRegion original = weapon.region, outline = weapon.outlineRegion;
        try {
            weapon.region = art;
            weapon.outlineRegion = missingOutline;
            weapon.draw(unit, mount);
        } finally {
            weapon.region = original;
            weapon.outlineRegion = outline;
        }
    }

    public static int cargoCount(int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) return 0;
        return (int)Math.min(4L, (4L * amount + capacity - 1L) / capacity);
    }

    /** Draw a complete body and optional visual layers. The caller owns draw color/reset. */
    public void draw(Unit unit, TextureRegion original) {
        boolean thrust = layeredEngines() && unit.isAdded() && !unit.dead()
            && unit.elevation > .05f && unit.vel().len2() > .015f;
        if (thrust) drawExhaust(unit);
        place(unit, body(original), .5f, .5f, width, height);
        if (!detailed() || variant < 0) return;

        if (completeReactor) {
            int tick = (int)(Time.time / 5f) + (unit.id & 7);
            int index = unit.dead() ? 0 : PULSE[Math.floorMod(tick, PULSE.length)];
            float size = width * reactorSize;
            place(unit, reactor[index], .5f, reactorY, size, size);
        } else {
            // Missing animation file does not disable the whole model.
            for (TextureRegion frame : reactor) if (frame != null) {
                float size = width * reactorSize;
                place(unit, frame, .5f, reactorY, size, size);
                break;
            }
        }
        if (layeredCargo()) {
            int count = cargoCount(unit.stack.amount, unit.type.itemCapacity);
            float size = width * .151f;
            for (int i = 0; i < count; i++) {
                float x = i % 2 == 0 ? .30f : (variant == 2 ? .69f : .70f);
                float y = i < 2 ? cargoFront : cargoRear;
                place(unit, cargo, x, y, size, size);
            }
        }
        // The mining head lights only during mining; no coloured wash on the hull.
        if (!frontCombatLaser && miningLaser != null && unit.isAdded() && !unit.dead() && unit.mining()) {
            float h = height * .155f;
            place(unit, miningLaser, .5f, .11f, h * miningLaser.width / miningLaser.height, h);
        }
        if (thrust) {
            // A small hot glow sits on each otherwise dark nozzle. Exhaust remains a separate layer.
            float w = width * .11f, h = height * .037f;
            place(unit, exhaust[0], .30f, .981f, w, h);
            place(unit, exhaust[0], .70f, .981f, w, h);
        }
    }

    private void drawExhaust(Unit unit) {
        float speed = Mathf.clamp(unit.vel().len() / Math.max(.01f, unit.type.speed));
        int frame = Math.min(4, Math.max(0, (int)(speed * 4f + Mathf.absin(Time.time, 3f, .7f))));
        float h = height * (.08f + .095f * speed);
        float w = h * 44f / 80f;
        float y = .984f + h / height * .28f;
        place(unit, exhaust[frame], .30f, y, w, h);
        place(unit, exhaust[frame], .70f, y, w, h);
    }

    /** Top-down normalized mount coordinates: (0,0) = upper left, (1,1) = lower right. */
    public void place(Unit unit, TextureRegion texture, float u, float v, float w, float h) {
        if (texture == null || !texture.found()) return;
        float sx = Draw.xscl, sy = Draw.yscl;
        float rotation = unit.rotation - 90f;
        float x = (u - .5f) * width * sx;
        float y = (.5f - v) * height * sy;
        Draw.rect(texture, unit.x + Angles.trnsx(rotation, x, y),
            unit.y + Angles.trnsy(rotation, x, y), w * sx, h * sy, rotation);
    }
}
