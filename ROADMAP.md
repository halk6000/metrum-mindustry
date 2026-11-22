# Metrum → Java mod (Mindustry)

Źródło: `original/Metrum` (JSON/HJSON mod v1.0.3, minGameVersion 145).

## Wykonane
- [x] Rozpakowanie oryginału, repo git, roadmapa

## Pozostałe
- [x] Szkielet Java (Gradle 8.10 wrapper, Mindustry v146 compileOnly) — `gradlew jar` → build/libs/Metrum.jar
- [x] Port: itemy (5) + płyn steam
- [x] Port: wiertła (5), produkcja (6)
- [x] Port: turbina zaawansowana
- [x] Port: jednostki MonoMet T1–T3 + warianty b
- [x] Port: doki (Air Miner Dock, Rebuilder) — cały oryginał przeniesiony
- [x] Kompilacja pod grę 160.8 (desktop.jar ze Steam), dźwięki zaktualizowane
- [x] Turbina: bez pary 75% mocy; duży piec parowy 3x3
- [ ] Test w grze (wczytanie jar, brak błędów sprite/crash)
- [x] Falownik cieplny 2x2 (HeatCrafter: ciepło 6, woda 0.25 → para 0.5, przegrzanie do 300%)
- [x] Dron wzmacniający (T3, z Air Miner Dock): co 10 s +20% obrażeń i zasięgu w promieniu 10; koszt 20 berylu, -2 za każdy wyższy materiał (min 2); bierze najsłabszy materiał z rdzenia
- [x] Stacja paliw 2x2, zasięg 8, żużel/magma/plazma (+10/30/50%)
- [x] Kuźnia magmy (ciepło + odłamki) i jonizator plazmy (magma + atomowy węgiel + prąd)
- [x] Bak paliwa wg systemu Metrum: T1–T3 + warianty B/C/D; bak od wariantu C, pojemność T1=100 / T2=200 / T3=250; vanilla bez paliwa
- [x] Drzewko tech: cała zawartość podpięta pod drzewo Erekiru
- [x] Zderzacz cz.1: 6 atomowych itemów + Atomizer (2 minerały -> 1 atom)
- [x] Zderzacz cz.2: Hadron Collider 3x3, 5 receptur (karborund, węglik wolframu, brąz berylowy, żelazowolfram, materia kwarkowa) + drzewko
- [ ] Zastosowania produktów zderzacza (nowe bloki/jednostki)
- [x] Lokalizacja: EN domyślny + PL (assets/bundles)
- [x] Planeta Ferrox (orbita słońca, teren z generatora Erekiru, rdzeń Bastion, wspólne drzewko z Erekirem)
- [ ] Własny generator terenu Ferrox
- [ ] Warianty C i D dronów MonoMet (C = drony wzmacniające?) + rekonstruktory
- [x] AI wroga na Ferrox: vanilla buildAi + rtsAi + BaseExpander (co 20 s fortyfikacja przy rdzeniu z zasobów rdzenia, tier obrony rośnie co 10 fal)
- [ ] BaseExpander: zaopatrzenie wieżyczek (amunicja/prąd), test w kampanii

## Blokady / uwagi
- Oryginalny mod.json ma błędy składni (brak przecinków) — działa tylko dzięki tolerancyjnemu HJSON.
- "dll": Mindustry to Java — mod będzie .jar (DLL nie dotyczy tej gry).
- Jar jest desktopowy (bez dexa na Androida) — do dodania później.
- Oryginał miał temperature 10.5 / heatCapacity 40 (skala gry 0–1) — znormalizowane do 0.9/0.4. Hardness 25 → 4 (25 blokowało wydobycie).
- Test w grze: last_log.txt nie odświeża się przy automatycznym uruchomieniu (Steam Cloud?) — potrzebny ręczny test użytkownika.
- Stary Metrum.zip przeniesiony z saves/mods do Metrum-backup-from-game.zip (konflikt nazw z jar).
