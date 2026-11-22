"""Static consistency check for the Metrum mod: sprites + EN/PL bundle keys for every content name."""
import re, pathlib, sys
root = pathlib.Path(__file__).resolve().parent.parent
src = "\n".join(p.read_text(encoding="utf-8") for p in (root / "src").rglob("*.java"))
sprites = {p.stem for p in (root / "assets/sprites").rglob("*.png")}
bundles = {l: (root / f"assets/bundles/{f}").read_text(encoding="utf-8")
           for l, f in (("EN", "bundle.properties"), ("PL", "bundle_pl.properties"))}

kinds = {  # constructor / helper -> bundle prefix
    "Item": "item", "product": "item", "atomic": "item", "Liquid": "liquid", "StatusEffect": "status",
    "Planet": "planet", "ErekirUnitType": "unit", "miner": "unit", "booster": "unit",
}
found = []
for m in re.finditer(r'(?:new\s+)?(\w+)\("([a-z0-9-]+)"', src):
    cls, name = m.groups()
    if cls in ("Weapon",) or name.startswith(("-", "metrum-", "heal-")):
        continue
    if cls in ("product", "atomic", "miner", "booster", "streamDrill", "decompressor") or m.group(0).startswith("new"):
        prefix = kinds.get(cls, "block")
        found.append((prefix, name))
found = sorted(set(found))
# status effects built from a name array
for n in re.findall(r'"(fuel-(?:slag|magma|plasma))"', src):
    found.append(("status", n))

problems = 0
for prefix, name in found:
    for lang, text in bundles.items():
        if f"{prefix}.metrum-{name}.name" not in text:
            print(f"[{lang}] missing {prefix}.metrum-{name}.name"); problems += 1
    if prefix in ("item", "liquid", "unit", "block") and name not in sprites:
        print(f"[sprite] missing {name}.png ({prefix})"); problems += 1
print(f"checked {len(found)} content entries, {problems} problem(s)")
sys.exit(1 if problems else 0)
