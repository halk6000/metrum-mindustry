"""Generates the 4x4 open-pit mine sprite (128x128)."""
import pathlib
import random

from PIL import Image, ImageDraw

root = pathlib.Path(__file__).resolve().parent.parent
random.seed(7)
im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
d = ImageDraw.Draw(im)


def octo(m, c, col):  # inset m, corner cut c
    a, b = m, 127 - m
    d.polygon([(a + c, a), (b - c, a), (b, a + c), (b, b - c), (b - c, b), (a + c, b), (a, b - c), (a, a + c)], fill=col)


octo(0, 14, "#2e2f36"); octo(4, 13, "#6e7080"); octo(7, 12, "#989aa4"); octo(12, 11, "#6e7080"); octo(15, 10, "#3a3b44")
terraces = ((18, "#8a6a52"), (27, "#735541"), (36, "#5c4232"), (45, "#463024"), (53, "#33221a"))
for m, c in terraces:
    cut = 16 - (m - 18) // 4
    octo(m, cut, c)
    if m < 53:  # lit terrace edges
        d.line([(m + cut, m), (127 - m - cut, m)], fill="#a68468", width=2)
        d.line([(m, m + cut), (m, 127 - m - cut)], fill="#a68468", width=2)
for _ in range(60):  # rubble
    x, y = random.randint(28, 98), random.randint(28, 98)
    d.rectangle([x, y, x + 2, y + 2], fill=random.choice(["#a68468", "#3d2b21", "#7f8088", "#5fa36b"]))
d.polygon([(96, 18), (108, 30), (70, 68), (60, 58)], fill="#4d4e58")  # ramp
d.line([(97, 20), (62, 57)], fill="#989aa4", width=2)
for t in range(5):
    d.line([(93 - 7 * t, 24 + 7 * t), (101 - 7 * t, 32 + 7 * t)], fill="#2e2f36", width=2)
for x, y in ((14, 14), (102, 14), (14, 102), (102, 102)):  # steel corner posts
    d.rectangle([x, y, x + 11, y + 11], fill="#2e2f36")
    d.rectangle([x + 2, y + 2, x + 9, y + 9], fill="#b0b4c0")
    d.rectangle([x + 4, y + 4, x + 7, y + 7], fill="#ffd37f")
im.save(root / "assets/sprites/blocks/production/open-pit-mine.png")
im.resize((384, 384), 0).save(root / "build/open-pit-preview.png")
