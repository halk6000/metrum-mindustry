"""Rest pose of the code-drawn twin-roller salvager, for UI and build plans."""
from pathlib import Path
from PIL import Image, ImageDraw
import sys

overclocked = "--overclocked" in sys.argv

image = Image.new("RGBA", (64, 64))
draw = ImageDraw.Draw(image)

def rect(x, y, w, h, color):
    draw.rectangle((round((x-w/2+8)*4), round((8-y-h/2)*4),
                    round((x+w/2+8)*4)-1, round((8-y+h/2)*4)-1), fill=color)

rect(0, 0, 15.6, 15.6, "#30343c")
for y in (-6, 6):
    rect(0, y, 13, 2, "#656b76")
for x in (-6, 6):
    rect(x, 0, 2, 10, "#656b76")
for x in (-2.7, 2.7):
    cx, cy = (x+8)*4, 32
    draw.ellipse((cx-10, cy-10, cx+10, cy+10), fill="#656b76")
    draw.line((cx-9, cy, cx+9, cy), fill="#a6adb8", width=2)
    draw.line((cx, cy-9, cx, cy+9), fill="#a6adb8", width=2)
    draw.ellipse((cx-3, cy-3, cx+3, cy+3), fill="#c5a66b")
if overclocked:
    rect(-6, 4, 2, 2, "#c5a66b")
    rect(6, -4, 2, 2, "#c5a66b")
    rect(0, 6, 7, 1, "#c5a66b")
    rect(0, -6, 6, 2.5, "#30343c")
    for x in (-1.5, 0, 1.5):
        rect(x, -6, 0.5, 2, "#a6adb8")
name = "salvager-overclocked" if overclocked else "salvager"
image.save(Path(__file__).resolve().parents[1] / f"assets/sprites/blocks/production/{name}.png")
