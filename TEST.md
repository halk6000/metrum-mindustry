# Szybki test w grze (~10 min)

Przed testem: **zrestartuj Mindustry** (nowy `Metrum.jar` już leży w `saves/mods`).
Po starcie powstaje `saves/metrum-load.txt` — Claude go odczyta.

Najszybciej w **Edytorze / Sandbox (Erekir)** z nieskończonymi zasobami.

| # | Co zrobić | Oczekiwane | OK? |
|---|---|---|---|
| 1 | Menu → Mody | „Metrum Industry Drones 2.0.0”, bez czerwonych błędów | |
| 2 | Menu budowania (Erekir) | Nowe bloki mają grafiki (nie różowe „oh no”) i polskie nazwy | |
| 3 | Piec parowy: woda + węgiel | Produkuje parę | |
| 4 | Falownik cieplny + 2 grzałki elektryczne + woda | Para na wyjściu, przy większym cieple szybciej | |
| 5 | Zaawansowana turbina na kominie parowym, bez pary | Prąd ~75%; z parą 100% | |
| 6 | Fabryka amunicji: Fe + stal + beryl + woda | 2 skrzynki amunicji + para | |
| 7 | Dok dronów → MonoMet T1 → Air Miner Rebuilder → T1B → Rekonstruktor Wojenny → T1C | T1C większy, wolniejszy, z niebieskim pierścieniem | |
| 8 | Kliknij T1C | Pomarańczowy pasek paliwa pod jednostką i w panelu, spada | |
| 9 | Poczekaj aż bak = 0 | Status „Brak paliwa”, jednostka bardzo wolna | |
| 10 | Stacja paliw + żużel obok drona | Bak się napełnia, status „Paliwo z żużlu” (+10%) | |
| 11 | T1C obok rdzenia z berylem, obok inne jednostki | Co 10 s niebieska fala, sojusznicy „Wzmocniony” | |
| 12 | Atomizer: grafit → atomowy węgiel; Zderzacz: atomowy węgiel + krzem | Karborund | |
| 13 | Mapa planet → Ferrox | Rdzawa planeta, da się wylądować, teren czerwony z kominami i rudą żelaza | |
| 14 | Zapisz grę z T1C o niepełnym baku, wczytaj | Bak ma tyle samo co przed zapisem | |

Problemy: zrzut ekranu + `saves/last_log.txt` po zamknięciu gry.
