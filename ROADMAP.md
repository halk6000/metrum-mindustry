# Metrum → Java mod (Mindustry)

Źródło: `original/Metrum` (JSON/HJSON mod v1.0.3, minGameVersion 145).

## Wdrożone lub testowane
- [x] Edytor balansu (suwaki z limitami) + eksport do jar: `Edytor balansu.bat`

## Funkcje w rozwoju i do potwierdzenia
- [ ] Atomy i Panel kontroli reakcji są w grze. Do potwierdzenia cały przebieg fuzji z panelem, w tym pełny bufor, zmiana celu i wznowienie po wczytaniu.
- [ ] De-atomizer zamienia atomy na surowce. Do potwierdzenia w grze.
- [ ] Fabryka atomizacji: cztery niezależne sloty produkcyjne korzystają z osobnych buforów dla każdego surowca. Dostawy i odbiór atomów można podłączyć z dowolnej strony. Maksymalny pobór to 510 jednostek energii/s, badanie po Atomizerze i karborundzie. Do potwierdzenia obsługa w grze.
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
- [x] Port: itemy (5) + płyn steam
- [x] Port: wiertła (5), produkcja (6)
- [x] Port: turbina zaawansowana
- [x] Port: jednostki MonoMet T1–T3 + warianty b
- [x] Port: doki (Air Miner Dock, Rebuilder) — cały oryginał przeniesiony
- [ ] Dźwięki po aktualizacji do sprawdzenia w grze.
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

## Planowane: nowe planety i mechaniki

### Metonia (dotychczasowa nazwa: Ferrox)
- [ ] **Zmiana nazwy planety Ferrox → Metonia** we wszystkich nazwach widocznych w grze, lokalizacjach PL/EN, opisach, drzewie technologii, mapie planet, dokumentacji i testach. Zachować dotychczasowy przemysłowy/rdzawy charakter planety, gospodarkę AI, rozwój sektorów i wsparcie międzysektorowe.
- [ ] **Migracja techniczna nazwy**: przed zmianą wewnętrznego identyfikatora planety sprawdzić zgodność istniejących zapisów kampanii, sektorów i powiązanych danych; nie zmieniać go bez strategii migracji. Wcześniejsze wpisy „Ferrox” w historii realizacji roadmapy opisują dawną nazwę i nie oznaczają cofnięcia wykonanej pracy.

### Metonia — kopalnia odkrywkowa i złoża wyczerpywalne
- [ ] **Boostery kopalni**: sprawdzić w grze zasilony radar obok (−2 s na 1%, raz) oraz wodę 12/s (szansa odkrycia 20% → 24%).
- [ ] **Złoża**: w toku są warstwy, rozrost i zagęszczanie odkrytych skupisk. Test potwierdza zachowanie znalezisk przy trzech wolnych polach i limit trzech warstw. Sprawdzić w grze rozmieszczanie przy braku miejsca, zużycie podczas nieobecności, zapis i odczyt oraz synchronizację sieciową.
- [ ] **Balans kopalni**: brak gwarancji zwrotu kosztów, ryzyko niepożądanych surowców, odpowiednie rozmieszczanie odkryć i obsługa braku wolnych pól wokół kopalni.
### Venoms Issland — planeta bioewolucji i toksycznej chemii
- [ ] **Nowa planeta Venoms Issland**: mroczny klimat, dominujący fiolet, biologiczne środowisko i odrębny charakter od przemysłowej Metonii; przygotować świat, mapy, oprawę, drzewo technologii oraz unikalne surowce i płyny.
- [ ] **Naziemne drony pajęcze / skorpionopodobne**: jednostki nastawione na zatruwanie przeciwników, ataki kwasowe (plucie kwasem) i walkę z wykorzystaniem efektów toksycznych; zaprojektować ich role, rozwój, animacje i sposób przeciwdziałania.
- [ ] **Bioegzystencja i ewolucja budynków**: budynki biologiczne mogą ewoluować w nowe formy po spełnieniu określonych warunków (np. dostęp do substancji, środowisko, czas lub wykonane działania); zdefiniować warunki, etapy, koszty i konsekwencje ewolucji w rozgrywce.
- [ ] **Niebezpieczne związki toksyczne i wybuchowe**: stworzyć mechanikę substancji o szkodliwych właściwościach, ich produkcji, przechowywania i wykorzystania w walce; błędne połączenia lub niewłaściwe warunki mogą prowadzić do skażenia, uszkodzeń albo wybuchów. Dokładne receptury i reguły pozostają do zaprojektowania.
- [ ] **Badania autorskie: grzybowe feromony** — po pokonaniu wrogich jednostek gracz pozyskuje feromony jako zasób badań. Silniejsze jednostki dostarczają więcej feromonów; ustalić tabelę nagród, zbieranie, przechowywanie i wydawanie.
- [ ] **Drzewko bio-badań** — feromony odblokowują budynki ewolucyjne i mutacje ekonomiczne/bojowe; rozwój ma wynikać z walki oraz ewolucji, a nie wyłącznie standardowych kosztów surowcowych.
- [ ] **Mutacje — przykłady do wdrożenia i testów**: gospodarcza „Wiertnice +20% prędkości wydobycia” (dla wszystkich wiertnic objętych mutacją); bojowa „Jednostki z atakami obszarowymi emitują toksyczne zarodniki o obrażeniach równych 30% bazowych obrażeń ataku”. Doprecyzować naliczanie obrażeń w czasie, możliwość kumulacji i działanie na cele odporne na truciznę.
- [ ] **Balans i testy Venoms Issland**: zapewnić czytelność zagrożeń, możliwość obrony przed kwasem i truciznami oraz sensowną progresję ewolucji bez blokad rozwoju sektora.

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

- [ ] **Regresja rozpoznawania zasobów przez planer AI — węgiel i paliwa rdzenia (25.11)**: objaw analogiczny do wcześniejszego błędu z niewidocznym piaskiem (wspólna przyczyna do potwierdzenia w kodzie). Rdzeń spala węgiel, ale planer nie widzi go w danych dronów. Udostępnić planerowi węgiel oraz wszystkie pozostałe zasoby zużywane jako paliwo przez rdzeń; uwzględniać ich zapotrzebowanie i dostępność w danych dronów. Planer powinien następnie sprawdzać dostawy taśmociągami lub dronami i planować brakujące zaopatrzenie. Test regresji: piasek, węgiel i każde paliwo rdzenia, oba sposoby transportu oraz brak dostaw.
- [ ] **Inteligentne wydobycie**: test potwierdza zmianę celu pustego drona, gdy rdzeń przestaje przyjmować dotychczasową rudę. Sprawdzić zachowanie w kampanii.

## Stary Wąwóz
- Złoża Starego Wąwozu są rozmieszczone ręcznie: mają być skąpe, lecz wystarczające na pierwsze walki. Nie dosypywać losowych rud ani zwiększać złóż jako obejścia problemów gospodarki AI.

## Ustalenia użytkownika — maszyny parowe i stapiacz
- [x] Sąsiadujące maszyny parowe tworzą grupę ze wspólnym wejściem/wyjściem. Premia grupy: +10% generacji prądu i ciepła, -5% zużycia pary, naliczana raz. Dwie maszyny po 12 pary/s: 22,8 pary/s; po 12 ciepła: 26,4. Obsłużyć podział i łączenie grup bez duplikacji zasobów. Grupa oddaje łączne ciepło jak jedna maszyna, także gdy odbiornik dotyka tylko jednej z maszyn; premia stała.
- [x] Prymitywny stapiacz skał 2x2: 1 skała + 1 węgiel -> 12 żużlu, cykl 1 s, 300 prądu/s. Koszt: 60 odłamków skały, 40 żelaza, 30 krzemu. Badanie po kruszarce. (zatwierdzone przez użytkownika)
- [ ] W kolejnych etapach okresowo sprawdzać origin/main:ROADMAP.md przez git fetch; nie nadpisywać zmian użytkownika ani lokalnej pracy AI.

## Bezpieczny piec parowy — ustalenia użytkownika (27.11)
- [x] Duży piec parowy po 80 s pracy na ozonie zmienia się w Bezpieczny piec parowy: tylko ozon, prędkość 200%, wybuch 1% co 30 ticków, 5% obrażeń dla siebie i 20 dla sąsiadów, +50% życia, inny wygląd. Przetestowane w grze.
- [x] Wybuch odejmuje 10 s od licznika zdarzenia zamiast go zerować. Bez radaru przemiana wymaga szczęścia, z radarem trwa ok. 4 min — decyzja użytkownika: zostawić tak.

## Wzorzec układu bazy AI — na podstawie bazy użytkownika (27.11)
- [ ] Zwartość: maszyny jednego łańcucha stoją obok siebie, wyjście jednej wpada prosto do następnej; bez taśm tam, gdzie wystarczy sąsiedztwo.
- [ ] Jedna główna pętla taśm wokół rdzenia, do której dochodzą krótkie odgałęzienia z wiertnic; bez równoległych tras dla tego samego surowca.
- [ ] Prąd: nieliczne węzły na pełnym zasięgu promienia (wdrożone 27.11), bez ciągnięcia „kabla” po każdej kratce.
- [ ] Płyny: krótkie rury; maszyny pijące wodę stawiać blisko źródła wody, jedna linia na odbiorcę (wdrożone 27.11), bez urwanych końcówek (wdrożone 27.11).
- [ ] Rozmieszczenie od środka: produkcja przy rdzeniu, wiertnice na złożach z krótką taśmą, obrona na obrzeżu w stronę wroga.
- [ ] Kryterium gotowości: proces produkcyjny działa (towar dociera), a nie tylko budynek stoi.

## Następny krok AI — planer celów i graf zależności (25.11)
- [ ] Zastąpić indywidualne reguły dla surowców uniwersalnym grafem zależności opartym na danych bloków, recepturach, kosztach budowy, paliwach, amunicji, energii, płynach i produkcji jednostek. Niestandardowe mechaniki obsługiwać adapterami.
- [ ] Kolejka celów strategicznych generuje zadania podrzędne według brakujących zależności, dostępnych zapasów i przepustowości dostaw. Wybiera efektywne źródła oraz transport dronami, taśmociągami lub rurami; wykrywa cykle i niedostępne technologie. Weryfikuje faktyczne działanie budynków, a nie tylko ich postawienie.
- [ ] Początkowy scenariusz rozwoju: (1) wieżyczka obronna wraz z zaopatrzeniem; (2) fabryka dronów wydobywczych; (3) kolejna wieżyczka; (4) ulepszenie rdzenia do poziomu 2; (5) kolejna wieżyczka; (6) przestawienie produkcji dronów wydobywczych na bojowe, np. T2; (7) wyprodukowanie 2 T2 i atak na bazę przeciwnika; (8) wyprodukowanie 5 T2 i kolejny atak; (9) budowa 2 wieżyczek; (10) ulepszenie rdzenia do poziomu 3; dalszy stopniowy rozwój.
- [ ] Proponowane zabezpieczenia: priorytet awaryjny obrony i podtrzymania gospodarki, ponowne planowanie po stratach, zachowanie niezbędnych dronów wydobywczych przy zmianie produkcji. Poziom inteligencji wpływa na efektywność planowania, niezależnie od poziomu bazy.
- [ ] Testy: realizacja scenariusza od startu, brak paliwa rdzenia (węgiel i inne), piasek, brak energii, utrata dostaw, zależności cykliczne i odbudowa po ataku. Nie tworzyć osobnych ifów dla każdego surowca.

## Planowane: wskaźniki AI i rozwoju gospodarczego sektorów (25.11)
- [ ] Po rozwiązaniu debugowania myśli AI wyświetlać pod standardowym zagrożeniem sektora kolorowy pasek z nazwą faktycznego poziomu AI, a pod nim poziom gospodarczy i punkty. Rozdzielić inteligencję AI od rozwoju bazy.
- [ ] Punktacja: pozyskiwanie zasobów 2 pkt; podłączenie dostaw do rdzenia 3 pkt; aktywny dron 15 pkt, bezczynny 5 pkt; wieżyczka obronna 20 pkt; mały mur 2 pkt, duży 8 pkt; ulepszenie rdzenia 200 pkt; działające przetwórstwo 30 pkt × 1,5; przebudowy dronów 50 pkt × 1,5 za każdy kolejny wyższy poziom; kapsuła desantowa +600 pkt. Uwzględnić każdy wychodzący dron przetworzony w kolejny poziom; dokładną interpretację naliczania i mnożników przetwórstwa doprecyzować przed implementacją.
- [ ] Progi: 0–249 kolonizacja, 250–399 presyjny, 400–999 konfliktowy, 1000–1999 wojna gospodarcza, 2000+ wojna sektorowa (najwyższa kategoria). Powyżej 5000 nie dodawać kolejnych kategorii; punktacja nadal może rosnąć. Granice rozłączne.
- [ ] Propozycja: dynamicznie przeliczać stan gospodarki po budowie, zniszczeniu, zmianie aktywności dronów i dostaw. Doprecyzować, które punkty są stanem aktualnym, a które nagrodą za zdarzenie, aby uniknąć sztucznego farmienia punktów. Zapewnić zgodność z symulacją sektorów offline.

- [ ] Doprecyzowanie punktacji kumulacyjnej (25.11): dron T1 = 15 pkt, T2 = 15 + 50 = 65 pkt, T3 = 15 + 50 + ok. 97 = ok. 162 pkt. Przetwórstwo: T1 = 30 pkt, T2 = 30 + 45 = 75 pkt, T3 = 30 + 45 + 60 = 135 pkt; kolejne składniki rosną o 15 pkt. Nie mnożyć całej dotychczasowej sumy przez 1,5. Wzór kolejnych poziomów przebudowy dronów pozostaje do doprecyzowania.

- [ ] Blokada technologiczna AI: poziom rdzenia warunkuje dostęp do zaawansowanych dronów i wieżyczek. Początkowo AI buduje podstawowe jednostki i obronę. Wymagany poziom rdzenia stanowi zależność w uniwersalnym grafie celów; przed produkcją lub budową wyższego poziomu planer zabezpiecza materiały i ulepsza rdzeń. Dokładne wymagania poszczególnych obiektów wynikają z danych technologicznych, nie z osobnych reguł AI.

- [ ] Posiłki międzysektorowe: dostępny poziom technologiczny jednostek zależy od rdzenia sektora wysyłającego, nie odbierającego. Stan gospodarki określa możliwości produkcji i liczebność posiłków, ale sam nie odblokowuje technologii. Przykładowo sektor ok. 500 pkt może wysyłać T1B, T2B lub T3B zależnie od odblokowań, a sektor ok. 5000 pkt może dysponować jednostkami czwartego poziomu technologicznego, jeśli ulepszył rdzeń. Propozycja: posiłki pobierać z faktycznie wyprodukowanych jednostek lub ich odpowiednika w symulacji offline, bez generowania jednostek ponad dostępne technologie. Dokładne limity liczebności do ustalenia.

- [ ] Limit posiłków międzysektorowych: maksymalnie 2 × poziom rdzenia sektora wysyłającego × liczba posiadanych przez niego kapsuł desantowych. Twardy limit 30 jednostek posiłkowych jednocześnie w sektorze odbierającym, łącznie ze wszystkich sektorów wysyłających; nie wliczać miejscowej armii. Przy wysyłce uwzględniać wolne miejsca w limicie odbiorcy oraz wcześniej ustalone ograniczenia technologiczne i produkcyjne. Częstotliwość wysyłki pozostaje do ustalenia.

## Planowane: chemia, jonizacja i przemysł kwarkowy (26.11)

- [ ] **Rozbijacz chemiczny**: rozkłada związki na pierwiastki składowe, nie na atomowe itemy. Przykładowe surowce: woda (H₂O → wodór i tlen), ozon (O₃ → tlen), cyjanogen (C₂N₂ → węgiel i azot), piasek traktowany jako SiO₂ (krzem i tlen). Receptury dla tlenku berylu, węglików i ropy doprecyzować na podstawie składu przyjętego w grze; nie przypisywać fikcyjnym mieszankom rzeczywistych wzorów bez ustalenia.
- [ ] **Atomizer i deatomizer**: zachować dotychczasowe role dla minerałów i stałych surowców (minerał → postać atomowa oraz odwrotnie). Rozbijacz chemiczny nie zastępuje atomizera.
- [ ] **Jonizator gazów**: przetwarza gazy pierwiastkowe w odpowiednie atomowe zasoby dla przemysłu jądrowego. Rozszerzyć zestaw atomów m.in. o wodór, tlen, azot i hel; kolejne pierwiastki według potrzeb receptur. Nie mylić z istniejącym jonizatorem plazmy (paliwo).
- [ ] **Dejonizator**: odtwarza gazy i syntetyzuje związki z odpowiednich składników (również ciecze). Kluczowa receptura użytkownika: wodór + ozon → woda (bilans chemiczny 3H₂ + O₃ → 3H₂O), jako alternatywne źródło wody w sektorach z jej niedoborem. Dalsze propozycje: tlen → ozon, węgiel + azot → cyjanogen; dokładne jednostki produkcji i receptury do ustalenia. Nazwa obejmuje umownie także syntezę chemiczną.
- [ ] **Zderzacz hadronów**: zachować istniejące receptury materiałowe, dodać syntezę wybranych pierwiastków z atomowych zasobów gazowych i mineralnych. Sumowanie liczb protonów może służyć jako uproszczenie gry, nie jako pełny fizyczny model reakcji.
- [ ] **Reaktor fuzji**: rozszerzyć istniejący łańcuch transmutacji oraz Panel kontrolny reakcji o nowe lekkie pierwiastki. Rozważyć osobne tryby energetyczny, syntezy i produkcji plazmy; ostateczne receptury i koszty pozostają do ustalenia.
- [ ] **Booster z materii kwarkowej (zatwierdzone przez użytkownika)**: normalny uzysk = 1×, aktywny booster = 2,5× ilości produktów przy tych samych podstawowych wejściach i czasie cyklu; nie jest to mnożnik prędkości. Materia kwarkowa (istniejący quarkMatter, nazywana również materią kwantową w rozmowie) zużywana proporcjonalnie do faktycznie przetworzonych zasobów, bez zużycia podczas przestoju. Dla przedmiotów rozliczać ułamkowy uzysk akumulatorem (np. naprzemiennie 2 i 3 przy bazowym uzysku 1), bez utraty lub duplikacji po zapisie/wczytaniu. Dokładna dawka boostera oraz lista maszyn objętych premią do ustalenia.
- [ ] **Balans i integracja**: zwykła synteza nie może tworzyć darmowych zasobów w zamkniętym obiegu; uwzględniać energię, rzeczywiste zużycie boostera i przepustowość. Produkcja syntetyczna ma być alternatywą dla wydobycia, nie jego bezkosztowym zamiennikiem. Dodać receptury do uniwersalnego grafu zależności planera AI, edytora balansu i drzewa technologii; sprawdzić cykle, blokady wyjść i zapis stanu.

## Ustalenia: rozbijacz chemiczny i wieża amoniakowa (26.11)
- [ ] Rozbijacz chemiczny: maksymalnie dwa produkty na recepturę, sztywne porty A/B, bez automatycznego ponownego przetwarzania własnego produktu. Arkycyt (umownie C₂H₃N) rozkładać w dwóch osobnych maszynach: 2 arkycytu → 4 węgla + 2 amoniaku (NH₃); następnie 2 amoniaku → 6 wodoru + 2 azotu. Każdy cykl może niezależnie korzystać z boostera kwarkowego ×2,5. Poprawka przygotowana lokalnie, test w grze do wykonania.
- [ ] **Wieża znakująca teren amoniakiem** (pomysł użytkownika): po podłączeniu amoniaku znakuje sektor i co 5 minut wymusza falę wrogów do odparcia. Do ustalenia: koszt budowy, zużycie amoniaku, działanie na zdobytych sektorach, skład fal, zachowanie po odłączeniu paliwa i w trybie offline. Proponowane zabezpieczenie: wspólny licznik sektora, aby wiele wież nie mnożyło przypadkowo liczby fal.

## Aktualizacja 27.11.2026 — przemysł, interfejs i logistyka AI

Statusy poniżej opisują ustalenia rozmowy i poprawki lokalne, nie potwierdzają obecności całego kodu w repozytorium. Dotychczasowe wpisy pozostają bez zmian. Zadania z wcześniejszych sekcji należy uznać za zamknięte dopiero po integracji i właściwych testach.

### Przemysł i chemia
- [x] Rozbijacz chemiczny: użytkownik potwierdził działanie podstawowego cyklu, osobnych komór i pasków GUI; przygotowano sztywne porty A/B oraz dwuetapowe przetwarzanie arkycytu przez amoniak.
- [ ] Dokończyć testy wszystkich receptur, portów, pełnych komór, zapisu i synchronizacji boostera kwarkowego ×2,5. Nie przetwarzać automatycznie własnego produktu. Sprawdzić bilans masy według jednostek gry.
- [ ] Fabryka atomizerów: cztery niezależne komory wejście/wyjście, 3×3, HP 750, koszt 100 stali + 10 karborundu + 12 elektrum, 2 surowce → 1 atom na komorę co 0,5 s; 127,5 energii/s na aktywną komorę, 510/s przy czterech. Kod/GUI przygotowane lokalnie; sprawdzić konfigurację, równoległą pracę i blokadę pojedynczego wyjścia.
- [ ] Zagęszczacz związkowy: 2×2, 40 stali + 60 tlenku berylu + 30 krzemu, 90 energii/s, cykl 1 s; 6 jednostek tlenu → 2 ozonu (5 z boosterem kwarkowym). Sprawdzić dalsze receptury, skalę podglądu, kartę techniczną i działanie boostera.
- [x] Duży piec parowy: użytkownik potwierdził działanie poprawionego GUI i wykrywania radaru BHP. Przyjęte parametry: tlen +45% prędkości / +30% uzysku, 10% ryzyka zapłonu; ozon +60% / +50%, 20% zapłonu i 5% wybuchu, zasilony radar obniża ryzyko wybuchu do 2%; losowanie co 18 ticków. Zasięg radaru w poprawce lokalnej obejmuje również skos i jedno wolne pole.
- [ ] Zweryfikować faktyczny uzysk pieca, zużycie gazu, pożary i wybuchy na różnych planetach oraz radar BHP w kopalni odkrywkowej. Nie zmieniać zatwierdzonych parametrów bez uzgodnienia.
- [ ] Rozważyć CO₂ jako produkt uboczny spalania węgla i odzysk węgla/tlenu w rozbijaczu chemicznym. Pomysł omówiony, nie zatwierdzono kosztów ani receptur. Nie dopuścić do darmowego zamkniętego obiegu.
- [ ] Jonizator/dejonizator gazów, rozszerzenie atomów, synteza wody, dalsza fuzja i wieża amoniakowa: pozostają zadaniami z wcześniejszych sekcji.

### GUI, grafika i warstwy widoczności
- [ ] Wspólne modułowe GUI z rzeczywistymi danymi budynków: komory, receptury, postęp, zasilanie, booster i jednoznaczna diagnostyka (brak wejścia, energii lub miejsca na konkretnym wyjściu). Rozszerzenie przygotowane lokalnie dla kopalni odkrywkowej, atomizerów, zderzacza, reaktorów, maszyn parowych i wybranych fabryk. Użytkownik potwierdził pięć budynków; pozostałych nie testował. Proste bloki zachowują standardowe GUI.
- [ ] Sprawdzić PL/EN wszystkich nowych paneli i kart technicznych w grze, długość etykiet, brak zawijania przycisków, brakujące klucze i ikony. Sam komplet kluczy nie potwierdza czytelności.
- [ ] MicroPipe: transport cieczy i gazów nad budynkami, połączenia klikane jak słupy energetyczne, wolna przestrzeń pod przewodem. Pierwsza lokalna wersja: przyłącza 1×1, zasięg 12 pól, przepustowość 12/s, bufor 20, koszt 12 stali + 8 krzemu + 6 berylu (parametry robocze); bez rozgałęzień. Dokończyć rozgałęzienia, zapis, synchronizację, kierunki przepływu i blokady mieszania mediów.
- [ ] Warstwy widoczności: przycisk „Widoczność” / „Visibility” przy przedmiotach w rdzeniu; nie otwierać okna automatycznie po kliknięciu rdzenia ani blokować pobierania przedmiotów. Maskowanie dronów do 20% widoczności (również efektów wydobycia i napraw), niezależne tryby MicroPipe: 100%, 20%, ukryte. Ustawienia lokalne gracza, bez wpływu na symulację, kolizje czy AI. Poprawkę przycisku przygotowano po wykryciu złego zawijania; sprawdzić końcową wersję, pusty rdzeń, budowanie pod dronami i multiplayer.
- [ ] Grafiki HD jednostek T3–T3D: modułowe kadłuby, uzbrojenie, reaktory, silniki i 4 sloty ładunku (po jednym na kolejne 25% pojemności). Użytkownik potwierdził renderowanie i portret. Sprawdzić skalę 70% w świecie i 40% podczas produkcji względem pierwotnego podglądu, wszystkie warianty, rekonstruktory i wydajność; nie zmieniać hitboxów z powodu skali grafiki.
- [ ] Nowa grafika promocyjna oficjalnej wersji „Metrum Industry Drones” z planetą i dronami: koncepcja przygotowana, pozostaje włączenie do wydania oraz czytelność małej ikony.

### AI — delegowanie zamiast dublowania sterowników
- [ ] Planer wyznacza cele i priorytety, wykorzystuje istniejące komendy wydobycia, inteligentnego wydobycia, opieki, odbudowy i budowy. Lokalny refaktor usuwa osobny ResourceDroneAI na rzecz SmartMining i CaretakerAI; zachować obsługę ładunku, przypisania do rdzenia, przestojów, powrotu do wcześniejszych zadań i nie przerywać rozpoczętej budowy. Sprawdzić limit jednego delegowanego górnika i opiekuna na rdzeń jako rozwiązanie tymczasowe.
- [ ] **Nienaruszalna zasada logistyki:** drony wydobywcze oddają surowce wyłącznie do rdzenia. Nie delegować im bezpośrednich dostaw do pieców. Rdzeń spala/nie magazynuje m.in. węgla i piasku; dynamicznie wykrywać możliwość magazynowania, nie wpisywać wyjątku dla każdego materiału. Dla niemagazynowalnych surowców planować wiertnię/ekstraktor lub źródło produkcyjne oraz fizyczną trasę do odbiorcy. Surowce wytworzone przez fabryki mogą zasilać dalsze fabryki zgodnie z regułami gry.
- [ ] Test regresji: inteligentne wydobycie nie zapętla się na węglu/piasku, drony z istniejącym ładunkiem nie tracą go bez uzasadnienia, planer nie produkuje zbędnych górników. Oddzielić zapas rdzenia od rzeczywistych zapasów i dostaw każdej maszyny.
- [ ] Posiłki: lokalna poprawka tankowania po błędzie nieistniejącej metody FuelSystem.refillFull; zweryfikować działanie istniejącego API paliwowego, limit 30 żywych jednostek plus rezerwacje w drodze i brak posiłków z innych sektorów na pierwszej mapie.

### AI — uniwersalny LogisticsPlanner (priorytet)
- [ ] Zintegrować przygotowany lokalnie moduł LogisticsPlanner.java z BasePlanner.java. Główny planer zleca połączenie źródła, odbiorcy i surowca; moduł odpowiada za analizę i budowę infrastruktury.
- [ ] Rozpoznawać faktyczne źródła, receptury, odbiorców, kierunki i rodzaje surowców na trasie, także gdy taśma/most chwilowo jest pusty. Nie uznawać samego istnienia wiertni ani fabryki za zapewnioną dostawę.
- [ ] Jeden surowiec i dwóch uprawnionych odbiorców: zwykłe rozgałęzienie. Jeżeli rdzeń nie magazynuje materiału, nie kierować go do rdzenia jako magazynu. Mieszana taśma: filtrowany rozdzielacz na surowiec zainteresowania oraz boczny transport pozostałych zasobów. Zachować poprawne kierunki wejść i wyjść.
- [ ] Planowanie tras podobne do natywnego układania dróg i rur Mindustry: dobierać taśmy/rury, łączniki, skrzyżowania, obejścia i mosty w zależności od kolizji oraz zgodności transportowanego medium. Pierwsza wersja obejmuje przedmioty i ograniczone wyszukiwanie mostów; płyny, gazy i MicroPipe oraz pełna obsługa kolizji pozostają do wykonania.
- [ ] Najpierw zbudować alternatywną trasę, potem modyfikować istniejące połączenie. Sprawdzać koszt całej trasy, dostępne materiały, kolejkę konstruktorów, wolne wyjścia fabryk i blokady przestrzeni. Przy niepowodzeniu ponownie planować zamiast powtarzać nieskuteczną operację.
- [ ] Krytyczny test na Starym Wąwozie: piec łukowy otrzymuje 4 piasku i 1 grafit na partię zgodnie z aktualną recepturą testowanego pieca; powstaje realne rozgałęzienie/filtr, a krzem faktycznie trafia do gospodarki. Nie mylić tego z wcześniej planowanym podkręconym piecem o innych recepturach. Testować również mieszany transport, pełne magazyny, brak miejsca, zniszczenie mostu oraz utratę źródła. Nie dosypywać rud na mapie jako obejścia błędu.
- [ ] Po stabilizacji transportu połączyć moduł z uniwersalnym grafem zależności celów, przepustowością i diagnostyką myśli AI. Dalsza rozbudowa bazy dopiero po potwierdzeniu działających dostaw.

### Kolejność dalszych prac
1. Regresja wydobycia i logistyki AI, rzeczywista produkcja krzemu, testy rozdzielaczy i mostów.
2. Uniwersalny graf zależności oraz transport płynów/gazów; dokończenie MicroPipe.
3. Testy pozostałych GUI, maskowania, chemii, kopalni i balansu; pełny build 160.8, zapis/wczytanie i multiplayer.
4. Rozwój gospodarczy AI, symulacja offline i wsparcie sektorowe; dokończenie Metonii.
5. Kolejne mechaniki chemiczne, wieża amoniakowa i Venoms Issland, zgodnie z wcześniejszymi sekcjami.
