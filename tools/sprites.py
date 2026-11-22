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

if __name__ == "__main__":
    fuel_station(OUT / "units/fuel-station.png")
    magma_forge(OUT / "production/magma-forge.png")
    plasma_ionizer(OUT / "production/plasma-ionizer.png")
    heat_inverter(OUT / "production/heat-inverter.png")
    collider(OUT / "production/hadron-collider.png")
    atomizer(OUT / "production/atomizer.png")
    print("sprites written")
