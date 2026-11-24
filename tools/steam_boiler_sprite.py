"""Render the code-drawn boiler at rest for its inventory icon and build plan."""
from pathlib import Path
from PIL import Image, ImageDraw
import sys

large = "--large" in sys.argv
half = 12 if large else 8
image = Image.new("RGBA", (half * 8, half * 8))
draw = ImageDraw.Draw(image)

def rect(x, y, width, height, color):
    left = round((x - width / 2 + half) * 4)
    top = round((half - y - height / 2) * 4)
    right = round((x + width / 2 + half) * 4) - 1
    bottom = round((half - y + height / 2) * 4) - 1
    draw.rectangle((left, top, right, bottom), fill=color)

if large:
    rect(0, 0, 23.6, 23.6, "#30343c")
    for y in (-9, 9):
        rect(0, y, 20, 3, "#656b76")
    for x in (-10, 10):
        rect(x, 0, 2, 18, "#656b76")
        rect(x, 0, 3, 4, "#a6adb8")
rect(0, 0, 15.6, 15.6, "#30343c")
rect(0, 1, 10, 12, "#656b76")
for x in (-6, 6):
    rect(x, 0, 2, 11, "#656b76")
for y in (5, -3):
    rect(0, y, 8, 1, "#a6adb8")
rect(0, 0, 6, 5, "#30343c")
rect(6, 0, 2.5, 3, "#a6adb8")
name = "large-steam-furnace" if large else "steam-furnace"
image.save(Path(__file__).resolve().parents[1] / f"assets/sprites/blocks/production/{name}.png")
