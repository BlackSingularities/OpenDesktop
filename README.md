# OpenDesktop

Otwarty, rozbudowany launcher (ekran główny) na Androida, napisany w Kotlinie i Jetpack Compose.

## Pulpit

- **Siatka do ustawienia** — od 3 do 10 kolumn i od 3 do 12 wierszy; zmiana układu przenosi elementy, które się nie mieszczą, na kolejne ekrany.
- **Ikony, foldery i widżety** — przytrzymaj i przeciągnij; upuść aplikację na aplikację, a powstanie folder.
- **Nakładanie się** — elementy mogą na siebie zachodzić albo trzymać się wolnych komórek (przełącznik w ustawieniach).
- **Zmiana rozmiaru** — uchwyt w rogu zaznaczonego elementu w trybie edycji (dotyczy widżetów, folderów i ikon).
- **Wiele ekranów** — dodawanie, usuwanie, zmiana kolejności i wybór ekranu głównego. Przytrzymanie elementu przy krawędzi przenosi go na sąsiedni ekran albo tworzy nowy.
- **12 przejść między ekranami** — przesunięcie, sześcian, sześcian od środka, głębia, oddalenie, przenikanie, wachlarz, odwracana karta, stos kart, karuzela 3D, akordeon, fala.
- **Widżety systemowe i wbudowane** — zegar z datą i baterią oraz pasek wyszukiwania.
- **Dok** — od 3 do 7 miejsc; przeciąganie między dokiem a pulpitem.
- **Gesty** — przesunięcie w górę, w dół i podwójne dotknięcie z akcjami do wyboru.
- **Strefy upuszczania** — usuń, informacje, odinstaluj.

## Szuflada aplikacji

11 motywów: siatka, lista, strony poziome, alfabet z szybkim przewijaniem, zakładki kategorii, kafelki, karuzela 3D, helisa 4D, kula 3D, koło do obsługi jedną ręką oraz bąbelki (plaster miodu z efektem soczewki).

- Sortowanie po nazwie, kategorii, częstości użycia lub dacie instalacji.
- Wyszukiwanie po nazwie, pakiecie i kategorii; Enter uruchamia pierwszy wynik; szukanie w sieci.
- Filtr kategorii, ukrywanie aplikacji, zmiana nazw, skróty aplikacji (gdy OpenDesktop jest domyślny).
- Kategorie: aplikacja może należeć do jednej albo wielu; tworzenie ręczne, automatyczne (bez AI) lub przez AI.

## AI (OpenRouter)

Wklej klucz z [openrouter.ai/keys](https://openrouter.ai/keys) i wybierz model z pełnej listy pobranej z API (wyszukiwanie, filtr darmowych modeli, ceny, długość kontekstu). AI potrafi:

- zbudować spersonalizowane kategorie z listy aplikacji według Twoich wskazówek,
- znaleźć aplikację po opisie („czym zapłacę za parking?”),
- zaproponować nazwę folderu.

Klucz zostaje na urządzeniu i nie trafia do kopii zapasowej.

## Aktualizacje

Każdy commit na `main` buduje podpisane APK i publikuje wydanie w GitHub Releases. Aplikacja sama sprawdza najnowsze wydanie wskazanego repozytorium (domyślnie `BlackSingularities/OpenDesktop`; w ustawieniach można je wyszukać na GitHubie), pobiera APK i przekazuje je do instalatora systemowego.

## Budowanie

```bash
gradle :app:assembleRelease
```

Wymagane: JDK 17, Android SDK 35, Gradle 8.11. Podpis wydań pochodzi z sekretów repozytorium `SIGNING_*`; bez nich APK jest podpisane kluczem debug.

---

**Projekt i wykonanie: [redzikowski.com](https://redzikowski.com)**

<a href="https://buycoffee.to/adamredzikowski" target="_blank"><img src="https://buycoffee.to/static/img/share/share-button-dark--pl.png" style="width: 351px; height: 92px" width="351" height="92" alt="Postaw kawę dla adamredzikowski na buycoffee.to"></a>
