# Metrum → Java mod (Mindustry)

Źródło: `original/Metrum` (JSON/HJSON mod v1.0.3, minGameVersion 145).

## Wykonane
- [x] Rozpakowanie oryginału, repo git, roadmapa

- [x] Edytor balansu (suwaki z limitami) + eksport do jar: `Edytor balansu.bat`

## Pozostałe (lista z 23.11)
- [ ] Atom dla każdego pierwiastka + blok „Panel kontrolny reakcji” obok reaktora (cel fuzji, przechwytuje produkt)
- [ ] De-atomizer: atomy -> z powrotem surowiec
- [x] Maszyna parowa rozpędza ciepło 3 -> 6 -> 9 -> 12
- [x] Huta Infuzyjna bez wody jako produktu ubocznego
- [ ] Inteligentne mosty: kolory obiegów (most łączy się tylko z mostem tego samego koloru)
- [x] Kapsuły lądownicze (tylko posiłki do własnych rdzeni - gracz i wróg; wróg przy ataku na jego bazę dostaje posiłki z innych baz co 3 min) (badanie: atomizer + 2 atomy każdego rodzaju + 100 stali + 250 krzemu): zrzut jednostek w pobliże rdzenia na innej planecie, 1500 energii przez 15 s; boty w późnej fazie też ich używają (posiłki z innych sektorów)
- [ ] B/C/D: coraz lepsze kopanie (kolejne rudy); po wyprodukowaniu domyślnie „inteligentne wydobycie – najmniej w magazynie”
- [ ] Komenda „Opiekuj się”: odbudowa + naprawa struktur
- [ ] Podkręcony krzemowy piec łukowy 3x3: 1 grafit + 2 piasek, 450 prądu, 6 krzemu, 95% czasu; booster atomowy węgiel -> 12 krzemu
- [ ] Zaawansowany Salvager 3x3: żelazo 60% / piasek 20% / beryl 15% / wolfram 5%
- [ ] Podkręcony Salvager 2x2: 80% 2 żelaza / 20% 1 piasek
- [ ] Planer AI krok 6: mury z przejściem w miejscu największego ognia
- [x] Panel wydobycia (komenda „Inteligentne wydobycie”): komenda "wydobywaj" -> okno: najbliższa ruda / najmniej w magazynie / konkretna ruda
- [x] Drukarka kadłubów 3x3 (lekki / średni / ciężki, podgląd w środku, 20 s)
- [x] Drukarka części (silnik, skrzydła, nogi) + Magazyn części (tylko kadłuby i części)
- [x] Drukarka broni (laser, rakiety, ładunki, zbiornik paliwa, karabin) + Magazyn broni
- [x] Montownia dronów 4x4: wybór kadłuba, sloty-kwadraty (klik = moduł), 60 s, okno nazwy statku
- [x] Wieżyczki z kadłubów (karabin, rakiety; 1 skrzynka/strzał), drony C+ mogą je podnosić (PayloadUnit 2x2): drony C+ przenoszą kadłub i montują jako wieżyczkę; amunicja wg broni albo 1 skrzynka/strzał
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

## Planowane: symulacja rozwoju sektorów Ferrox (offline / podczas gry)
- [x] **Trudność planety = rozkład poziomów AI sektorów** (Ustawienia -> Metrum, SectorAi; główny sektor - do wskazania): sektor losuje poziom inteligencji zgodnie z trudnością planety. Główny sektor do przejęcia ma zawsze Taktyka bitewnego (BATTLE_TACTICIAN), niezależnie od losowania; liczyć go osobno od procentów zwykłych sektorów.
- [ ] **Rozkłady docelowe (zwykłe sektory)**: stałe udziały podane przez użytkownika są punktami zakotwiczenia, a pozostała pula jest **proporcjonalnie przeskalowywana (X)** na podstawie wag z poziomu Łatwy, po wykluczeniu kategorii z udziałem 0% i kategorii mających procent narzucony z góry.

| Trudność planety | DUMB (głupi) | EASY (łatwy) | HARD (trudny) | CHESS (szachista) | TACTICAL (taktyczny) | BATTLE_TACTICIAN (taktyk bitewny) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1. Swobodny | 40% | 60% | 0% | 0% | 0% | 0% |
| 2. Łatwy | 20% | 60% | 10% | 6% | 2% | 2% |
| 3. Normalny | 10% | 40% | 25% | 15% | 5% | 5% |
| 4. Trudny | 0% | 10% | 40% | 30% | 10% | 10% |
| 5. Eksterminacja | 0% | 0% | 50% | 30% | 10% | 10% |

- [ ] **Wzór „X” (proporcjonalna reszta)**: dla danego poziomu trudności obliczyć `R = 100% - suma udziałów stałych`; dla kategorii bez ustalonego udziału, które są dozwolone, wylosować docelowy udział proporcjonalnie do wagi bazowej `w_i` z Łatwego: `p_i = R × w_i / Σ(w_j pozostałych kategorii)`. Przykłady: Normalny ma 50% do rozdania wg wag HARD:CHESS:TACTICAL:BATTLE_TACTICIAN = 10:6:2:2 → 25/15/5/5. Trudny ma 50% wg 6:2:2 → 30/10/10. Eksterminacja ma 50% wg 6:2:2 → 30/10/10. Wykluczone kategorie pozostają zerowe. W razie niecałkowitych wyników zaokrąglać liczebności sektorów bez utraty sumy.
- [ ] **Stałe udziały nie podlegają skalowaniu**: Swobodny = 40% DUMB + 60% EASY; Łatwy = 60% EASY, 20% DUMB, 10% HARD, 6% CHESS, 2% TACTICAL, 2% BATTLE_TACTICIAN (dwa wskazane udziały po 1%); Normalny = 40% EASY i 10% DUMB; Trudny = 40% HARD i 10% EASY, bez DUMB; Eksterminacja = 50% HARD, bez EASY i DUMB. Główny sektor do przejęcia ma zawsze BATTLE_TACTICIAN, liczony osobno.
- [x] **Eksterminacja — minimum 7 Taktyków bitewnych na planecie**, wliczając główny sektor; po losowaniu skorygować przydziały, jeśli jest ich mniej niż 7 (jeżeli planeta ma mniej niż 7 sektorów, ograniczyć do liczby dostępnych). Dla każdego poziomu zaokrąglać przydziały do całkowitych liczebności sektorów z zachowaniem sumy.
- [ ] **Rozdzielić pojęcia**: pięć poziomów trudności planety i sześć poziomów AI w kodzie to różne skale. Poziom rozwoju bazy pozostaje od nich niezależny.
- [ ] **Oddzielić poziom AI od poziomu bazy**: poziom AI określa jakość decyzji, tempo i efektywność rozbudowy; poziom bazy określa rzeczywiście osiągnięty rozwój przemysłu, obrony i potencjału wojskowego. Łatwe AI może z czasem osiągnąć np. poziom bazy 10 — bez automatycznego zwiększania własnej inteligencji.
- [ ] **Rozwój sektora bez obecności gracza**: symulować upływ czasu, rozbudowę infrastruktury, produkcję, badania/odblokowania technologii oraz tworzenie i ulepszanie jednostek. Słabsze AI rozwija się wolniej i mniej efektywnie, ale nie ma sztucznego, niskiego limitu poziomu bazy.
- [ ] **Ciągłość stanu sektora**: zapisywać poziom bazy, budynki/infrastrukturę, zasoby, technologie, garnizon i postęp produkcji. Po wejściu gracza odtworzyć stan wypracowany przez symulację, bez tworzenia gotowej bazy „znikąd”.
- [ ] **Aktywny rozwój po wejściu gracza**: wróg nadal wydobywa surowce, buduje, produkuje i ulepsza jednostki oraz reaguje na atak zgodnie ze swoim poziomem AI. Po wyjściu gracza sektor wraca do symulacji uproszczonej.
- [ ] **Jednostki zależne od bazy**: dostępne typy, poziomy ulepszeń i tempo produkcji wynikają z infrastruktury i technologii sektora, a nie wyłącznie z poziomu AI.
- [ ] **Wsparcie międzysektorowe jako efekt rozwoju**: sektory wroga rozwijają własne zaplecze i wysyłają posiłki zależnie od swoich możliwości; gracz ma analogiczny mechanizm przygotowania i wysyłania jednostek. Zdobycie sektora osłabia sieć wsparcia przeciwnika.
- [ ] **Balans trudności**: Ferrox docelowo trudniejszy od Erekiru dzięki kumulującemu się rozwojowi wrogich sektorów i wsparciu międzysektorowemu, nie przez samo podbijanie HP lub obrażeń. Przetestować scenariusze: łatwe AI + rozwinięta baza, trudne AI + młoda baza.

## Repo
- GitHub (publiczne): https://github.com/halk6000/metrum-mindustry

## Blokady / uwagi
- Oryginalny mod.json ma błędy składni (brak przecinków) — działa tylko dzięki tolerancyjnemu HJSON.
- "dll": Mindustry to Java — mod będzie .jar (DLL nie dotyczy tej gry).
- Jar jest desktopowy (bez dexa na Androida) — do dodania później.
- Oryginał miał temperature 10.5 / heatCapacity 40 (skala gry 0–1) — znormalizowane do 0.9/0.4. Hardness 25 → 4 (25 blokowało wydobycie).
- Test w grze: last_log.txt nie odświeża się przy automatycznym uruchomieniu (Steam Cloud?) — potrzebny ręczny test użytkownika.
- Stary Metrum.zip przeniesiony z saves/mods do Metrum-backup-from-game.zip (konflikt nazw z jar).
- Przeglądarka modów w grze: topic mindustry-mod, release z jar i [v160] w nazwie; lista odświeża się co ~2 h. Przy nowym wydaniu podbić version w mod.hjson.

## Ustalenia fabularne i diagnostyka AI
- [x] Wskaźnik AI bazy: zielony Metrum po wykonaniu aktualizacji planera, pomarańczowy Mindustry według buildAi; osobno RTS Mindustry i licznik udanych kroków. Klient sieciowy pokazuje brak danych serwera.
- [ ] Mini scenka przed rozpoczęciem sektora Stary Wąwóz. Załogę zniosło zbyt daleko z Ekler; odkryła nowy typ drona. Dowództwo chce zdobyć jego plany, lecz posiadacze nie oddadzą ich bez walki. Cel pierwszej misji: zniszczyć rdzeń przeciwnika. Nazwę Ekler zachować według wypowiedzi użytkownika do doprecyzowania.

- [ ] Gospodarka AI (ustalenie użytkownika): buduje z posiadanych zasobów jak gracz; otrzymuje bonus 50% zasobów jako rekompensatę słabszych decyzji. Wdrażać jako +50% realnego wydobycia, bez nieskończonych magazynów; nie mnożyć istniejących zapasów ponownie przy wczytaniu.

- [ ] Optymalizacja ciasnej bazy (ustalenie użytkownika): gdy brakuje miejsca na budowę, sprawdzić faktyczną ciągłość transportu i mediów, naprawiać przerwy oraz skracać zbędne trasy; demontaż tylko po potwierdzeniu zachowania dostaw. Nie powielać rur przy niskim poziomie wody w odbiorniku.

- [ ] Rzeczywista budowa przez drony (korekta użytkownika): zastąpić natychmiastowe setNet kolejką BuildPlan i pobieraniem materiałów przez silnik. Pierwszy etap: mury z dedykowanym MetrumBuilderAI. Pozostałe: wiertnice, transport, prąd, rury, fabryki, wieżyczki, magazynki i rozbiórka. Licznik planera nie jest licznikiem ukończonych budów.

- [ ] Rozpoznanie i ekspansja gospodarcza (uwaga użytkownika 24.11): drony szukają brakujących zasobów poza bezpośrednim otoczeniem rdzenia, odkrywają teren i zakładają połączone placówki; używać maszyn Metrum, uwzględnić wolfram i unikać zależności wymagających wolframu do uruchomienia jego pierwszego źródła. Dalsza symulacja przy nieobecności gracza pozostaje osobnym zadaniem.

## Mapa użytkownika — aktualizacja 24.11
- [x] Zaimportowano Stary Wąwóz zapisany 24.11 o 08:34 z katalogu map Steam do obu nazw mapy w assets/maps; kolejny JAR i wydanie mają zawierać tę wersję.
- Złoża Starego Wąwozu są rozmieszczone ręcznie: mają być skąpe, lecz wystarczające na pierwsze walki. Nie dosypywać losowych rud ani zwiększać złóż jako obejścia problemów gospodarki AI.
