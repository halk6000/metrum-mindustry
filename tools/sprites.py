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

if __name__ == "__main__":
    collider(OUT / "production/hadron-collider.png")
    atomizer(OUT / "production/atomizer.png")
    print("sprites written")
