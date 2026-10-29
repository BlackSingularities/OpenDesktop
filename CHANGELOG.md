## 2.3.0 — Nowe okno „Dostosuj”, własna lokalizacja i bogatsza prognoza godzinowa

- Okno „Dostosuj” od nowa, na pełnym ekranie: pasek z zapisem i przywracaniem domyślnych, duży podgląd na jasnym, mieszanym lub ciemnym tle, karty z ikonami (Miejsce, Treść, Styl, Kolory, Tekst), ustawienia w sekcjach, przełączniki segmentowe zamiast przewijanych chipów, gotowe style jako miniatury, suwaki zmieniające podgląd na żywo.
- Pogoda dla bieżącego położenia telefonu („Moja lokalizacja”, zgrubna lokalizacja) albo wybranego miejsca z wyszukiwarki z podpowiedziami (zapisuje współrzędne).
- Mały przycisk odświeżania w rogu widżetów pogody (można ukryć) i „Odśwież teraz” w oknie.
- Prognoza godzinowa: odczuwalna, opady w mm, wiatr z kierunkiem i porywami, wilgotność, UV, zachmurzenie, ciśnienie; podsumowanie (kiedy zacznie/przestanie padać, najcieplej i najzimniej); nazwa dnia o północy.

## 2.2.0 — Rozbudowane widżety i pogoda wielodniowa

- Nowe widżety: prognoza godzinowa (wykres, kolumny, lista), pogoda – pełny panel, szczegóły pogody, słońce (łuk, wschód/zachód, długość dnia), faza Księżyca, rok w kropkach.
- Prognoza dzienna do 16 dni w czterech układach: kolumny, lista z paskami zakresu temperatur, wykres max/min, kompaktowy; nazwy dni, start od jutra, wyróżnienie dziś, opis warunków, opady, suma opadów, wiatr, UV.
- Pogoda teraz w trzech układach i z wybieranymi wartościami (odczuwalna, wiatr z kierunkiem, wilgotność, ciśnienie, UV, opady, zachmurzenie, słońce).
- Jednostki: °C/°F, km/h, m/s, mph, węzły, hPa/mmHg/inHg; miasto albo współrzędne, szybki wybór miast.
- Okno „Dostosuj” z kartami Treść / Wygląd / Kolory / Tekst i stałym podglądem: gradienty tła, grubość i kolor obrysu, 7 nowych gotowych stylów, kopiuj/wklej wygląd, własny kolor (HSV, hex), kroje pisma.
- Wygląd pogody: tło zależne od warunków i pory dnia, kolorowe temperatury, ikony kolorowe lub emoji, wielkość ikon, dziesiąte części stopnia, godzina aktualizacji, linie podziału.
- Jedno zapytanie do Open-Meteo na miasto (16 dni + 48 h), rozbiór poza wątkiem głównym; testy parsera, jednostek i fazy Księżyca.

## 2.1.1

- Nowa strona aplikacji z opisem funkcji.
- Jednolita informacja o autorze (Adam Rędzikowski · redzikowski.com) i przycisk „Postaw kawę”.
- Usunięte odnośniki do repozytorium z interfejsu.

## next CI release — Wyszukiwarka szuflady: kontakty, ustawienia i szybkie odpowiedzi

- Nad wynikami aplikacji kalkulator i przeliczanie jednostek (`QuickAnswers`); kliknięcie kopiuje wynik do schowka.
- Pod wynikami aplikacji kontakty z książki adresowej i ekrany ustawień systemowych (`UniversalSearch`), wyszukiwane poza wątkiem głównym.
- O `READ_CONTACTS` pytamy dopiero przy pierwszym wyszukiwaniu; bez zgody sekcja kontaktów jest pomijana, a wyszukiwanie aplikacji działa dalej.
- Nowe przełączniki w Ustawieniach → Szuflada aplikacji: „Kontakty w wynikach” i „Szybkie odpowiedzi” (domyślnie włączone).

## next CI release — Cofanie usunięcia

- Pigułka „Usunięto… · Cofnij” nad dokiem przez 5 sekund po usunięciu ikony, folderu, widżetu albo całego ekranu.
- Widżet systemowy znika z hosta dopiero po upływie tego czasu, więc cofnięcie przywraca go działającego, z tym samym identyfikatorem.
- Cofnięcie usunięcia ekranu przywraca też numer ekranu głównego.

## next CI release — Plakietki powiadomień

- Liczba oczekujących powiadomień w rogu ikony: pulpit, dok, foldery i wszystkie układy szuflady.
- Przełącznik „Plakietki powiadomień” w Ustawieniach → Wygląd → Ikony, z prośbą o systemowy dostęp do powiadomień.

## next CI release — Transparent application icon

- Use the transparent application mark in launcher, splash and existing branding surfaces.
- Preserve alpha in legacy and adaptive Android icons; refresh themed icon resources.

