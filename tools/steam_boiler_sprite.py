"""Render the code-drawn boiler at rest for its inventory icon and build plan."""
from pathlib import Path
from PIL import Image, ImageDraw

image = Image.new("RGBA", (64, 64))
draw = ImageDraw.Draw(image)

def rect(x, y, width, height, color):
    left = round((x - width / 2 + 8) * 4)
    top = round((8 - y - height / 2) * 4)
    right = round((x + width / 2 + 8) * 4) - 1
    bottom = round((8 - y + height / 2) * 4) - 1
    draw.rectangle((left, top, right, bottom), fill=color)

rect(0, 0, 15.6, 15.6, "#30343c")
rect(0, 1, 10, 12, "#656b76")
for x in (-6, 6):
    rect(x, 0, 2, 11, "#656b76")
for y in (5, -3):
    rect(0, y, 8, 1, "#a6adb8")
rect(0, 0, 6, 5, "#30343c")
rect(6, 0, 2.5, 3, "#a6adb8")
image.save(Path(__file__).resolve().parents[1] / "assets/sprites/blocks/production/steam-furnace.png")
