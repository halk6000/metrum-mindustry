"""Procedural block sprites in the 'Erekir dark' style; accent colour per department."""
from PIL import Image, ImageDraw
import pathlib
OUT = pathlib.Path(__file__).resolve().parent.parent / "assets/sprites/blocks"
BODY, EDGE, RING, DARK = (50, 52, 60), (32, 33, 40), (90, 95, 110), (20, 20, 26)
PHYS, FUEL, STEAM = (120, 200, 255), (255, 154, 46), (230, 235, 240)

def frame(size, accent):
    S = 32 * size
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([1, 1, S - 2, S - 2], fill=BODY + (255,), outline=EDGE + (255,), width=max(2, size))
    p = S // 7
    for x, y in ((3, 3), (S - p - 3, 3), (3, S - p - 3), (S - p - 3, S - p - 3)):
        d.rectangle([x, y, x + p, y + p], fill=EDGE + (255,))
        d.point((x + 2, y + 2), fill=accent + (255,))
    return im, d, S

def collider(path):
    im, d, S = frame(3, PHYS)
    d.ellipse([10, 10, S - 11, S - 11], outline=RING + (255,), width=8)
    d.ellipse([17, 17, S - 18, S - 18], outline=PHYS + (255,), width=2)
    d.ellipse([38, 38, S - 39, S - 39], fill=DARK + (255,), outline=PHYS + (255,), width=2)
    for x, y in ((S // 2 - 3, 3), (S // 2 - 3, S - 9), (3, S // 2 - 3), (S - 9, S // 2 - 3)):
        d.rectangle([x, y, x + 6, y + 6], fill=PHYS + (255,))
    im.save(path)

def atomizer(path):
    im, d, S = frame(2, PHYS)
    d.ellipse([12, 12, S - 13, S - 13], fill=DARK + (255,), outline=RING + (255,), width=4)
    d.ellipse([18, 26, S - 19, S - 27], outline=PHYS + (255,), width=2)
    d.ellipse([26, 18, S - 27, S - 19], outline=PHYS + (255,), width=2)
    d.ellipse([28, 28, S - 29, S - 29], fill=(220, 245, 255, 255))
    im.save(path)

def fuel_station(path):
    im, d, S = frame(2, FUEL)
    d.rectangle([8, 8, 30, S - 9], fill=DARK + (255,), outline=FUEL + (255,), width=2)
    d.rectangle([11, 30, 27, S - 12], fill=FUEL + (255,))
    d.rectangle([36, 10, S - 9, 22], fill=RING + (255,))
    d.line([(46, 22), (46, 42), (S - 10, 50)], fill=EDGE + (255,), width=3)
    d.ellipse([S - 14, 46, S - 6, 54], fill=FUEL + (255,))
    im.save(path)

def magma_forge(path):
    im, d, S = frame(2, FUEL)
    d.ellipse([10, 10, S - 11, S - 11], fill=DARK + (255,), outline=RING + (255,), width=4)
    d.ellipse([20, 20, S - 21, S - 21], fill=(255, 106, 46, 255))
    d.ellipse([27, 25, 35, 33], fill=(255, 210, 120, 255))
    im.save(path)

def plasma_ionizer(path):
    im, d, S = frame(3, (200, 150, 255))
    for r in (40, 30):
        d.ellipse([S // 2 - r, S // 2 - r, S // 2 + r, S // 2 + r], outline=RING + (255,) if r == 40 else (200, 150, 255, 255), width=4 if r == 40 else 2)
    d.ellipse([34, 34, S - 35, S - 35], fill=(200, 150, 255, 255))
    d.ellipse([42, 42, S - 43, S - 43], fill=(255, 240, 255, 255))
    im.save(path)

def heat_inverter(path):
    im, d, S = frame(2, STEAM)
    d.rectangle([7, 7, 29, S - 8], fill=DARK + (255,), outline=(255, 120, 60, 255), width=2)
    d.rectangle([35, 7, S - 8, S - 8], fill=DARK + (255,), outline=STEAM + (255,), width=2)
    for y in range(14, S - 12, 8):
        d.line([(12, y), (24, y)], fill=(255, 120, 60, 255), width=2)
        d.line([(40, y), (S - 13, y)], fill=STEAM + (255,), width=2)
    d.rectangle([29, 4, 34, S - 5], fill=EDGE + (255,))
    im.save(path)

ITEMS = pathlib.Path(__file__).resolve().parent.parent / "assets/sprites/items"
ATOMS = {"carbon": (110, 110, 125), "silicon": (140, 150, 175), "beryllium": (90, 210, 155),
         "iron": (200, 200, 200), "tungsten": (125, 145, 210), "thorium": (225, 110, 175)}
PRODUCTS = {"carborundum": (60, 150, 150), "tungsten-carbide": (95, 105, 125), "beryllium-bronze": (205, 160, 80),
            "ferrotungsten": (145, 145, 165), "quark-matter": (255, 80, 200)}

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

def atomic_item(name, c):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse([2, 10, 29, 21], outline=EDGE + (255,), width=4)
    d.ellipse([10, 2, 21, 29], outline=EDGE + (255,), width=4)
    d.ellipse([3, 11, 28, 20], outline=c + (255,), width=2)
    d.ellipse([11, 3, 20, 28], outline=c + (255,), width=2)
    d.ellipse([10, 10, 21, 21], fill=EDGE + (255,))
    d.ellipse([11, 11, 20, 20], fill=shade(c, 1.25) + (255,))
    d.ellipse([13, 12, 16, 15], fill=(255, 255, 255, 230))
    im.save(ITEMS / f"atomic-{name}.png")

def product_item(name, c):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    hexa = [(16, 1), (30, 9), (30, 23), (16, 31), (2, 23), (2, 9)]
    d.polygon(hexa, fill=EDGE + (255,))
    inner = [(16, 3), (28, 10), (28, 22), (16, 29), (4, 22), (4, 10)]
    d.polygon(inner, fill=c + (255,))
    d.polygon([(16, 3), (28, 10), (16, 16), (4, 10)], fill=shade(c, 1.35) + (255,))
    d.polygon([(16, 16), (28, 10), (28, 22), (16, 29)], fill=shade(c, 0.7) + (255,))
    d.line([(8, 11), (14, 8)], fill=(255, 255, 255, 200), width=1)
    im.save(ITEMS / f"{name}.png")

UNITS = pathlib.Path(__file__).resolve().parent.parent / "assets/sprites/units"

def upgraded_unit(src, dst, accent, pods):
    """bigger copy of the previous variant + armour pods + booster emitter ring in the accent colour"""
    base = Image.open(UNITS / src).convert("RGBA")
    im = base.resize((round(base.width * 1.25), round(base.height * 1.25)), Image.NEAREST)
    W, H = im.size
    # subtle accent tint on the hull so variants read apart at a glance
    px = im.load()
    for x in range(W):
        for y in range(H):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (int(r * 0.8 + accent[0] * 0.2), int(g * 0.8 + accent[1] * 0.2), int(b * 0.8 + accent[2] * 0.2), a)
    canvas = im
    cx, cy = W // 2, H // 2
    d = ImageDraw.Draw(canvas)
    r = max(3, W // 9)
    d.ellipse([cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1], outline=EDGE + (255,), width=2)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=accent + (255,), width=1)
    d.point((cx, cy), fill=(255, 255, 255, 255))
    canvas.save(UNITS / dst)

CARB = (60, 150, 150)

def carborundum_bore(base):
    im, d, S = frame(3, CARB)
    d.rectangle([14, 14, S - 15, S - 15], fill=DARK + (255,), outline=RING + (255,), width=3)
    for i in range(3):
        x = 22 + i * 18
        d.polygon([(x, 30), (x + 8, 22), (x + 16, 30), (x + 8, 38)], fill=CARB + (255,), outline=EDGE + (255,))
    im.save(base + ".png")
    top = Image.new("RGBA", (S, S), (0, 0, 0, 0)); t = ImageDraw.Draw(top)
    t.rectangle([S // 2 - 8, 4, S // 2 + 8, 22], fill=RING + (255,), outline=EDGE + (255,), width=2)
    t.ellipse([S // 2 - 14, S // 2 - 14, S // 2 + 14, S // 2 + 14], fill=BODY + (255,), outline=CARB + (255,), width=3)
    top.save(base + "-top.png")
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0)); g = ImageDraw.Draw(glow)
    g.ellipse([S // 2 - 8, S // 2 - 8, S // 2 + 8, S // 2 + 8], fill=(140, 255, 240, 200))
    glow.save(base + "-glow.png")

if __name__ == "__main__":
    carborundum_bore(str(OUT / "drills/carborundum-bore"))
    for t in (1, 2, 3):
        upgraded_unit(f"monomet-t{t}b.png", f"monomet-t{t}c.png", PHYS, 1)
        upgraded_unit(f"monomet-t{t}c.png", f"monomet-t{t}d.png", (200, 120, 255), 2)
    for n, c in ATOMS.items(): atomic_item(n, c)
    for n, c in PRODUCTS.items(): product_item(n, c)
    fuel_station(OUT / "units/fuel-station.png")
    magma_forge(OUT / "production/magma-forge.png")
    plasma_ionizer(OUT / "production/plasma-ionizer.png")
    heat_inverter(OUT / "production/heat-inverter.png")
    collider(OUT / "production/hadron-collider.png")
    atomizer(OUT / "production/atomizer.png")
    print("sprites written")
