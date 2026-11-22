# Metrum → Java mod (Mindustry)

Źródło: `original/Metrum` (JSON/HJSON mod v1.0.3, minGameVersion 145).

## Wykonane
- [x] Rozpakowanie oryginału, repo git, roadmapa

- [x] Edytor balansu (suwaki z limitami) + eksport do jar: `Edytor balansu.bat`

## Pozostałe
- [x] Szkielet Java (Gradle 8.10 wrapper, Mindustry v146 compileOnly) — `gradlew jar` → build/libs/Metrum.jar
- [x] Port: itemy (5) + płyn steam
- [x] Port: wiertła (5), produkcja (6)
- [x] Port: turbina zaawansowana
- [x] Port: jednostki MonoMet T1–T3 + warianty b
- [x] Port: doki (Air Miner Dock, Rebuilder) — cały oryginał przeniesiony
- [x] Kompilacja pod grę 160.8 (desktop.jar ze Steam), dźwięki zaktualizowane
- [x] Turbina: bez pary 75% mocy; duży piec parowy 3x3
- [x] Test w grze: wczytanie OK, 0 błędów, 0 brakujących grafik
- [x] Falownik cieplny 2x2 (HeatCrafter: ciepło 6, woda 0.25 → para 0.5, przegrzanie do 300%)
- [x] Wzmacniacze = warianty C (T1C/T2C/T3C, zasięg 8/10/12): +20% obrażeń i zasięgu, koszt 20 najsłabszego materiału (-2 za wyższy, min 2), bak 100/200/250; Booster Rebuilder (B -> C)
- [x] Stacja paliw 2x2, zasięg 8, żużel/magma/plazma (+10/30/50%)
- [x] Kuźnia magmy (ciepło + odłamki) i jonizator plazmy (magma + atomowy węgiel + prąd)
- [x] Bak paliwa wg systemu Metrum: T1–T3 + warianty B/C/D; bak od wariantu C, pojemność T1=100 / T2=200 / T3=250; vanilla bez paliwa
- [x] Drzewko tech: cała zawartość podpięta pod drzewo Erekiru
- [x] Zderzacz cz.1: 6 atomowych itemów + Atomizer (2 minerały -> 1 atom)
- [x] Zderzacz cz.2: Hadron Collider 3x3, 5 receptur (karborund, węglik wolframu, brąz berylowy, żelazowolfram, materia kwarkowa) + drzewko
- [x] Ściany z karborundu (odbijają pociski) i węglika wolframu (najcięższe), 1x1 i 2x2
- [x] Działo kwarkowe 3x3: amunicja brąz berylowy (szybkie, przebijające) i materia kwarkowa (wybuch 650, topienie)
- [x] Lokalizacja: EN domyślny + PL (assets/bundles)
- [x] Planeta Ferrox (orbita słońca, teren z generatora Erekiru, rdzeń Bastion, wspólne drzewko z Erekirem)
- [x] Generator Ferrox: układ Erekiru przemalowany na rdzawy (red stone / rhyolite, kolor globu)
- [x] Generator Ferrox: dodatkowe kominy parowe (~1 na 3000 kratek, z dala od rdzeni i spawnów)
- [x] Ruda żelaza (ore-iron) generowana w żyłach na Ferrox
- [x] Warianty D (zasięg 11/13/15, co 7.5 s), Advanced Booster Rebuilder 5x5 na plazmie (C -> D)
- [x] AI wroga na Ferrox: vanilla buildAi + rtsAi + BaseExpander (co 20 s fortyfikacja przy rdzeniu z zasobów rdzenia, tier obrony rośnie co 10 fal)
- [x] BaseExpander: tylko wieżyczki na amunicję (breach/diffuse/titan/disperse/scathe/smite), co 5 s dosypuje amunicję z rdzenia
- [x] tools/check.py (sprite + EN/PL dla każdej zawartości) wpięty w gradlew jar
- [x] Wróg na Ferrox: od fali 15 wystawia prawdziwe wzmacniacze (T2C, od fali 30 T3C), max 1 + (fala-15)/10, lecą za armią (do zestrzelenia)
- [x] Fabryka amunicji 3x3 (2 Fe + 1 stal + 3 Be + woda -> 2 skrzynki, para uboczna)
- [x] Rekonstruktor Wojenny = ostateczny (B->C i C->D), koszt bazowy 1 skrzynka + 2 stal + 0.09 pary/s, rośnie x1.5 na poziom (C->D: 2 skrzynki, 3 stal, 0.135 pary/s)
- [x] Mod potwierdzony w grze 160.8: 63 elementy, 0 błędów, 0 brakujących sprite, init OK (auto-test 22.11 11:42)
- [x] Ikony statusów (paliwo, wzmocnienie)
- [ ] Test kampanii Ferrox (TEST.md)

## Repo
- GitHub (prywatne): https://github.com/halk6000/metrum-mindustry

## Blokady / uwagi
- Oryginalny mod.json ma błędy składni (brak przecinków) — działa tylko dzięki tolerancyjnemu HJSON.
- "dll": Mindustry to Java — mod będzie .jar (DLL nie dotyczy tej gry).
- Jar jest desktopowy (bez dexa na Androida) — do dodania później.
- Oryginał miał temperature 10.5 / heatCapacity 40 (skala gry 0–1) — znormalizowane do 0.9/0.4. Hardness 25 → 4 (25 blokowało wydobycie).
- Test w grze: last_log.txt nie odświeża się przy automatycznym uruchomieniu (Steam Cloud?) — potrzebny ręczny test użytkownika.
- Stary Metrum.zip przeniesiony z saves/mods do Metrum-backup-from-game.zip (konflikt nazw z jar).
