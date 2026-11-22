# Metrum → Java mod (Mindustry)

Źródło: `original/Metrum` (JSON/HJSON mod v1.0.3, minGameVersion 145).

## Wykonane
- [x] Rozpakowanie oryginału, repo git, roadmapa

## Pozostałe
- [x] Szkielet Java (Gradle 8.10 wrapper, Mindustry v146 compileOnly) — `gradlew jar` → build/libs/Metrum.jar
- [ ] Port istniejącej zawartości (items, liquid stream, drills, production, turbine, MonoMet units, docks)
- [ ] Falownik cieplny (heat in → woda → para)
- [ ] Drony-wzmacniacze (+20% zasięg/obrażenia, koszt 20 mat. najsłabszego, -2 na kolejny tier)
- [ ] Stacja paliw 2x2, zasięg 8, paliwo żużel/magma/plazma (+10/30/50% prędkości)
- [ ] Bak paliwa jednostek ≥ tier C (100/200/250), zużycie 1/0.6 s, pomarańczowy pasek, -90% prędkości przy 0
- [ ] Zderzacz hadronów + drzewko tech
- [ ] Lokalizacja: EN domyślny, PL
- [ ] Nowa planeta + AI wroga z auto-rozbudową

## Blokady / uwagi
- Oryginalny mod.json ma błędy składni (brak przecinków) — działa tylko dzięki tolerancyjnemu HJSON.
- "dll": Mindustry to Java — mod będzie .jar (DLL nie dotyczy tej gry).
- Jar jest desktopowy (bez dexa na Androida) — do dodania później.
