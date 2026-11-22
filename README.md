# Metrum Industry Drones (Mindustry Java mod)

Java port of the original JSON mod "Metrum" (AkinoxPl) plus new mechanics. Target: **Mindustry build 160.8**, desktop.

[Polski opis niżej](#polski)

## Build

```
gradlew jar          # -> build/libs/Metrum.jar
```
Compiles against the installed game (`.../Steam/steamapps/common/Mindustry/jre/desktop.jar`); override with `-PmindustryJar=path`.
`tools/check.py` runs before every build and fails it if any content lacks a sprite or an EN/PL name.
`tools/sprites.py` regenerates the procedural sprites ("Erekir dark" style).

Install: `gradlew deploy` (writes `saves/mods/Metrum.zip`). Keep only one Metrum file in `saves/mods` — two copies with the same mod name conflict and the old one may load.

## Content

| Area | What |
|---|---|
| Original port | 5 items, steam, 5 drills, 6 crafters, advanced turbine, MonoMet T1–T3 + B, dock & rebuilder |
| Steam | Steam furnace 2x2 / large 3x3, **heat inverter** (heat + water -> steam), turbine runs at 75% without steam |
| Ammo | Ammo factory 3x3: 2 iron + 1 steel + 3 beryllium + water -> 2 ammo crates (+ steam) |
| War drones | Tiers T1–T3, upgrades B -> C -> D. **War Reconstructor** (the final one: B->C and C->D; base 1 crate + 2 steel + 0.09 steam/s, cost x1.5 per level). Each upgrade: bigger, slower, tougher, +1 weapon |
| Boost | C/D variants boost allies: +20% damage and range. Cost: 20 of the weakest material, 2 less per more advanced one (min 2). Mined ore counts |
| Fuel | C/D variants have a tank (T1 100 / T2 200 / T3 250), burn 1 per 0.6 s, orange bar, -90% speed when empty |
| Fuel station | 2x2, refuels within 8 tiles. Slag +10% / magma +30% / plasma +50% speed. Magma forge, plasma ionizer |
| Physics | Atomizer (2 minerals -> 1 atom), **Hadron Collider** (atom pair decides product): carborundum, tungsten carbide, beryllium bronze, ferrotungsten, quark matter |
| Planet | **Ferrox**: rusty re-skin of Erekir terrain, extra steam vents, iron ore. Enemy: buildAi + rtsAi + BaseExpander (fortifies cores, restocks turrets), fields booster craft (T2C, T3C from wave 30) from wave 15 |

## Known limits
- Fuel tanks are saved (custom save chunk); units are re-matched by type + position on load.
- Range boost stretches bullet lifetime; AI targeting still uses base range.
- Not yet tested in a full campaign.

---

## Polski

Port oryginalnego moda Metrum (JSON) do Javy + nowe mechaniki. Wersja gry: **160.8**.

- **Budowanie:** `gradlew jar`, plik trafia do `build/libs/Metrum.jar` → skopiuj do `saves/mods/`.
- **Para:** piece parowe, falownik cieplny (ciepło + woda → para), turbina 75% bez pary.
- **Amunicja:** fabryka amunicji 3x3 → skrzynki amunicji potrzebne do budowy wariantów C i wyżej.
- **Drony:** T1–T3, ulepszenia B → C → D (Rekonstruktor Wojenny). Każda wersja większa, wolniejsza, z dodatkową bronią.
- **Wzmocnienie:** C i wyżej: +20% obrażeń i zasięgu dla sojuszników, koszt 20 najsłabszego materiału, −2 za każdy lepszy.
- **Paliwo:** bak od wariantu C (100/200/250), 1 na 0,6 s, przy zerze −90% prędkości; stacja paliw 2x2, zasięg 8, żużel/magma/plazma = +10/30/50%.
- **Zderzacz hadronów:** atomizer + zderzacz, para atomów decyduje o produkcie.
- **Planeta Ferrox:** rdzawy teren, więcej kominów parowych, ruda żelaza, mądrzejszy wróg rozbudowujący bazę.

Plan i postęp: `ROADMAP.md`.
