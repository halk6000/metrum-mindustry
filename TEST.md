# Testy odzyskiwaczy i huty infuzyjnej

Uruchom z katalogu repozytorium: `./gradlew.bat salvagerTest`.
Test korzysta z silnika zainstalowanego Mindustry, bez otwierania zapisu gracza.
Inny plik gry można wskazać przez `-PmindustryJar="pełna ścieżka do desktop.jar"`.

Wprowadzone zmiany:
- Zaawansowany odzyskiwacz: koszt budowy 160 stali, 100 grafitu i 80 krzemu; bez wolframu. Nadal losuje wolfram z szansą 5%.
- Podkręcony odzyskiwacz: cykl 20 kroków (jak zwykły), 1 skała na cykl; wynik to 2 żelaza (80%) albo 1 piasek (20%). Rezerwuje miejsce na całą partię.

Potwierdzone automatycznie:
- Liczby produktów, zużycie jednej skały na zakończenie cyklu oraz oba wyniki losowania.
- Przekazywanie żelaza, piasku i stali do rzeczywistego obiektu taśmy Mindustry.
- Zachowanie produktów przy blokadzie wyjścia i wydawanie ich po odblokowaniu.
- Podkręcony odzyskiwacz podczas pracy: 9 pary/s i zapotrzebowanie 180 prądu/s; pełny magazyn zatrzymuje zużycie, a zwolnienie miejsca wznawia produkcję.

Granice weryfikacji: test ustawia świat i warunki programowo. Nie sprawdza interfejsu gry, istniejącego zapisu, zapisu/odczytu budynków ani symulacji nieaktywnego sektora. Odblokowanie taśmy jest symulowane; test sprawdza mechanizm wydawania, nie cały transport w czasie. Huta przechodzi test zakończenia receptury i przekazywania stali; jej kod nie został zmieniony.

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

## Nowe w 2.2.0
| # | Co zrobić | Oczekiwane | OK? |
|---|---|---|---|
| 15 | Zaznacz drony MonoMet, shift+klik w teren | Lecą tam (komendy RTS działają) | |
| 16 | Komenda „Inteligentne wydobycie” | Okno: najbliższa / najmniej w magazynie / konkretna ruda | |
| 17 | Drukarka kadłubów → wybierz średni | Kadłub widać w środku, po 20 s wychodzi | |
| 18 | Montownia: kadłub + kliknij sloty (karabin, silnik) | Kwadraty slotów, po 60 s dron + okno nazwy | |
| 19 | Dron modułowy | Nazwa nad dronem, strzela tylko z zamontowanej broni, ma pasek paliwa | |
| 20 | Wieżyczka kadłubowa + skrzynki amunicji | Strzela, 1 skrzynka na strzał | |
| 21 | Dron C podlatuje do wieżyczki kadłubowej | Może ją podnieść i postawić gdzie indziej | |

## Ustawienie linii dronów
```
[Dok 3x3] -> [Air Miner Rebuilder 3x3] -> [Rekonstruktor Wojenny 3x3] -> [Zaawansowany Rek. Wojenny 5x5]
   T1-T3          -> B (Fe 8, para)          -> C (skrzynka + 2 stal, para)    -> D (+ węglik W, żelazowolfram, plazma)
```
- Bloki muszą się **stykać**, a strzałka (obrót) każdego wskazuje na **następny** blok.
- Każdy rekonstruktor potrzebuje prądu i swoich surowców — bez nich dron wejdzie, ale nie wyjdzie.

Problemy: zrzut ekranu + `saves/last_log.txt` po zamknięciu gry.
