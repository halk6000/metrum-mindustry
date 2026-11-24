"""Rest pose of the code-drawn twin-roller salvager, for UI and build plans."""
from pathlib import Path
from PIL import Image, ImageDraw
import sys

overclocked = "--overclocked" in sys.argv
advanced = "--advanced" in sys.argv
half = 12 if advanced else 8

image = Image.new("RGBA", (half * 8, half * 8))
draw = ImageDraw.Draw(image)

def rect(x, y, w, h, color):
    draw.rectangle((round((x-w/2+half)*4), round((half-y-h/2)*4),
                    round((x+w/2+half)*4)-1, round((half-y+h/2)*4)-1), fill=color)

if advanced:
    rect(0, 0, 23.6, 23.6, "#30343c")
    for y in (-9, 9):
        rect(0, y, 21, 3, "#656b76")
    for x in (-9.5, 9.5):
        rect(x, 0, 3.5, 13, "#656b76")
        cx, cy = (x+half)*4, half*4
        draw.ellipse((cx-6, cy-6, cx+6, cy+6), fill="#a6adb8")
        draw.line((cx-6, cy, cx+6, cy), fill="#30343c", width=2)
        for y in (-4, 4):
            rect(x, y, 2, 1, "#82b9b0")
rect(0, 0, 15.6, 15.6, "#30343c")
for y in (-6, 6):
    rect(0, y, 13, 2, "#656b76")
for x in (-6, 6):
    rect(x, 0, 2, 10, "#656b76")
for x in (-2.7, 2.7):
    cx, cy = (x+half)*4, half*4
    draw.ellipse((cx-10, cy-10, cx+10, cy+10), fill="#656b76")
    draw.line((cx-9, cy, cx+9, cy), fill="#a6adb8", width=2)
    draw.line((cx, cy-9, cx, cy+9), fill="#a6adb8", width=2)
    draw.ellipse((cx-3, cy-3, cx+3, cy+3), fill="#c5a66b")
if overclocked:
    rect(-6, 0, 2, 10, "#c5a66b")
    rect(6, 0, 2, 10, "#c5a66b")
    rect(0, 6, 11, 2, "#c5a66b")
    for x in (-3, 0, 3):
        draw.line(((x-0.6+half)*4, (half-5.2)*4,
                   (x+0.6+half)*4, (half-6.8)*4), fill="#30343c", width=3)
    rect(0, -6, 6, 2.5, "#30343c")
    for x in (-1.5, 0, 1.5):
        rect(x, -6, 0.5, 2, "#a6adb8")
name = "salvager-advanced" if advanced else "salvager-overclocked" if overclocked else "salvager"
image.save(Path(__file__).resolve().parents[1] / f"assets/sprites/blocks/production/{name}.png")
