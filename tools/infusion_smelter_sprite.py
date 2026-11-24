"""Cold rest pose of the infusion smelter for its icon and build preview."""
from pathlib import Path
from PIL import Image, ImageDraw

im = Image.new("RGBA", (64, 64))
d = ImageDraw.Draw(im)
d.rectangle((1, 1, 62, 62), fill="#30343c")
d.ellipse((10, 10, 54, 54), fill="#656b76")
for y in (4, 52):
    d.rectangle((10, y, 53, y+7), fill="#656b76")
d.ellipse((14, 14, 50, 50), outline="#a6adb8", width=3)
d.ellipse((17, 17, 47, 47), fill="#30343c")
for x in (4, 52):
    d.rectangle((x, 14, x+7, 49), fill="#656b76")
    d.rectangle((x-1, 27, x+8, 36), fill="#a6adb8")
im.save(Path(__file__).resolve().parents[1] / "assets/sprites/blocks/production/infusion-smelter.png")
