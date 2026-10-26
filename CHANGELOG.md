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

