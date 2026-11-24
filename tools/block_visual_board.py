"""Build a review board at a constant number of pixels per game tile."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
entries = [
    ("steam-furnace", "Maly piec parowy", 2),
    ("large-steam-furnace", "Duzy piec parowy", 3),
    ("infusion-smelter", "Huta infuzyjna", 2),
    ("salvager", "Odzyskiwacz", 2),
    ("salvager-overclocked", "Podkrecony odzyskiwacz", 2),
    ("salvager-advanced", "Zaawansowany odzyskiwacz", 3),
]
board = Image.new("RGB", (960, 640), "#181b22")
draw = ImageDraw.Draw(board)
font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 20)
draw.text((24, 15), "METRUM | obudowy w spoczynku | jednakowa skala", font=font, fill="#dce1e9")
for i, (name, label, size) in enumerate(entries):
    x, y = (i % 3) * 320, 60 + (i // 3) * 280
    draw.rounded_rectangle((x+8, y+4, x+312, y+272), radius=8, fill="#252a33")
    sprite = Image.open(root / f"assets/sprites/blocks/production/{name}.png").convert("RGBA")
    sprite = sprite.resize((size*64, size*64), Image.Resampling.NEAREST)
    board.paste(sprite, (x+160-sprite.width//2, y+112-sprite.height//2), sprite)
    draw.text((x+18, y+222), f"{i+1}. {label}", font=font, fill="#dce1e9")
draw.text((24, 622), "Bez animacji i zaru. Piec lukowy korzysta z atlasu gry i nie jest pokazany.",
          fill="#a6adb8")
out = root / "docs/block-visual-board.png"
out.parent.mkdir(exist_ok=True)
board.save(out)
print(out)
