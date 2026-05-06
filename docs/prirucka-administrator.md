# FlightLog — Příručka administrátora

**Pro koho je tato příručka:** Pro osobu odpovědnou za správnost letových záznamů, export a import dat a dohled nad systémem FlightLog.  
**Předpoklady:** Základní orientace v tabulkových datech a práce se soubory CSV (Excel, LibreOffice Calc nebo podobný nástroj).

---

## Obsah

1. [Přihlášení a orientace v rozhraní](#1-přihlášení-a-orientace-v-rozhraní)
2. [Přehled letů a filtrování](#2-přehled-letů-a-filtrování)
3. [Ruční zadání letu](#3-ruční-zadání-letu)
4. [Úprava záznamu letu](#4-úprava-záznamu-letu)
5. [Smazání záznamu letu](#5-smazání-záznamu-letu)
6. [Export letů do CSV](#6-export-letů-do-csv)
7. [Import letů z CSV](#7-import-letů-z-csv)
8. [Správa uživatelů](#8-správa-uživatelů)
9. [Řešení problémů](#9-řešení-problémů)

---

## 1 Přihlášení a orientace v rozhraní

### Přihlášení

1. Otevřete prohlížeč a přejděte na adresu aplikace.
2. Zadejte svůj **e-mail** a **heslo** a klikněte na **Přihlásit se**.
3. Po úspěšném přihlášení se zobrazí stránka **Přehled letů**.

Pokud zapomenete heslo, klikněte na odkaz **Zapomenuté heslo?** a následujte pokyny zaslané e-mailem. Token pro reset hesla platí 24 hodin.

### Přehled obrazovky

Po přihlášení vidíte:

- **Horní lišta** — název aplikace a tlačítko **Odhlásit se**.
- **Panel filtrů** — sada polí pro zúžení zobrazených záznamů.
- **Tabulka letů** — výpis záznamů dle aktivních filtrů.
- **Stránkování** — tlačítka Předchozí / Další a informace o aktuální straně.
- **Tlačítka akcí** — Export CSV, Import CSV.

### Automatické odhlášení

Po 30 minutách nečinnosti vás systém automaticky odhlásí. Při návratu budete potřebovat znovu zadat přihlašovací údaje.

---

## 2 Přehled letů a filtrování

### Tabulka letů

Tabulka zobrazuje záznamy se sloupci:

| Sloupec | Popis |
|---|---|
| Datum | Datum letu ve formátu DD. MM. YYYY |
| Druh | Kluzák nebo Motorové letadlo |
| Typ | Konkrétní typ letadla (např. WT9, L-13 Blaník) |
| Imatrikulace | Identifikační označení letadla |
| Pilot | Příjmení pilota |
| Úkol | Účel letu |
| Start | Čas vzletu (HH:MM) |
| Přistání | Čas přistání (HH:MM), prázdné u letů, které jsou ještě ve vzduchu |
| Doba | Délka letu v hodinách (2 desetinná místa) |
| Akce | Tlačítka pro úpravu a smazání záznamu |

Řádky zvýrazněné žluty jsou lety, které jsou v danou chvíli **stále ve vzduchu** (dosud nebylo zadáno přistání).

### Filtry

Dostupné filtry:

| Filtr | Popis |
|---|---|
| Od / Do | Rozsah dat (formát YYYY-MM-DD; vyberte pomocí kalendáře) |
| Kategorie | Vše, Kluzák, nebo Motorové letadlo |
| Imatrikulace | Zadejte část nebo celou imatrikulaci |
| Stav | Vše / Ve vzduchu / Přistálé |
| Na stránku | Počet záznamů na stránku: 10, 25 nebo 50 |

### Jak filtrovat

1. Vyplňte požadovaná pole v panelu filtrů.
2. Klikněte na tlačítko **Filtrovat**.
3. Tabulka se aktualizuje dle zadaných podmínek.
4. Pro zrušení všech filtrů klikněte na **Zrušit filtr** — zobrazí se všechny záznamy.

### Stránkování

Pokud záznamy přesahují vybraný počet na stránku, přecházejte mezi stránkami tlačítky **Předchozí** a **Další**. Informace o aktuální straně a celkovém počtu stran je zobrazena mezi těmito tlačítky.

---

## 3 Ruční zadání letu

Pokud je potřeba doplnit let zpětně (například z papírové evidence), administrátor může let zadat ručně.

> Tato funkce má stejné formulářové pole a validace jako formulář pro zapisovatele. Podrobný popis polí najdete v Příručce zapisovatele.

Ruční zadání letu momentálně probíhá přes rozhraní pro zapisovatele (záložka **Nový let**). Administrátor má k tomuto rozhraní přístup a může ho použít pro zpětné záznamy.

---

## 4 Úprava záznamu letu

### Kdy úpravu použít

- Zapisovatel zadal špatný čas, imatrikulaci nebo úkol.
- Je potřeba doplnit čas přistání u záznamu, který byl zadán bez něj.
- Je nutné opravit chybu v historickém záznamu.

### Postup

1. Najděte záznam v tabulce (případně použijte filtry).
2. V sloupci **Akce** klikněte na tlačítko **Upravit** u příslušného řádku.
3. Otevře se dialogové okno **Upravit let** s předvyplněnými hodnotami.
4. Upravte potřebná pole:

| Pole | Popis |
|---|---|
| Datum | Datum letu |
| Pilot | Příjmení pilota (lze vybrat z nápovědy) |
| Imatrikulace | Označení letadla — velká písmena |
| Typ letadla | Vyplňte pouze při zadávání hostujícího letadla, které systém nezná |
| Úkol | Účel letu |
| Start | Čas vzletu ve formátu HH:MM |
| Přistání | Čas přistání ve formátu HH:MM (lze ponechat prázdné u letu ve vzduchu) |
| Typ startu | Naviák nebo Motorové letadlo (vlek) |

5. Klikněte na **Uložit**.

Systém při uložení automaticky:
- Ověří, že čas přistání je pozdější než čas vzletu.
- Přepočítá dobu letu.
- Zaznamená do auditního logu, kdo a kdy úpravu provedl.

Pokud je zadaná hodnota chybná, zobrazí se červená chybová zpráva přímo u příslušného pole.

---

## 5 Smazání záznamu letu

### Kdy mazat

Smazání je nevratné. Smažte záznam pouze tehdy, pokud byl chybně zadán a nemá být v systému evidován vůbec.

### Postup

1. Najděte záznam v tabulce.
2. V sloupci **Akce** klikněte na tlačítko **Smazat**.
3. Zobrazí se potvrzovací dialog se základními údaji o záznamu.

### Provázané záznamy (vlek)

Pokud je mazaný záznam provázán s jiným letem (kluzák byl vlečen motorovým letadlem), systém na tuto vazbu upozorní a nabídne dvě možnosti:

- **Smazat i provázaný záznam** — odstraní oba lety (kluzák i vlečné letadlo).
- **Zrušit pouze vazbu** — zanechá provázaný záznam v systému, ale odpojí ho od mazaného letu.

4. Vyberte požadovanou akci a potvrďte.

Informace o smazání se uchová v auditním logu systému (kdo, kdy, jaký záznam).

---

## 6 Export letů do CSV

Export slouží k přenosu letových dat do externích nástrojů (například Excel pro účetní zpracování).

### Co se exportuje

Exportují se výhradně záznamy odpovídající **aktuálně nastaveným filtrům**. Pokud nejsou nastaveny žádné filtry, exportují se všechny záznamy.

### Postup

1. Nastavte filtry podle potřeby (datum, kategorie, imatrikulace…).
2. Klikněte na tlačítko **Export CSV** v panelu filtrů.
3. Soubor se automaticky stáhne do vašeho počítače.

Název souboru: `flightlog_export_DD-MM-YYYY.csv` (datum exportu).

### Formát exportního souboru

- Kódování: UTF-8 s BOM (Excel soubor otevře správně bez nutnosti konverze)
- Oddělovač: středník (`;`)
- První řádek: hlavička s názvy sloupců
- Řazení: od nejnovějšího letu dle času vzletu (sestupně)

Struktura sloupců:

| Sloupec | Datový typ | Příklad |
|---|---|---|
| `id` | číslo | `25` |
| `datum` | DD-MM-YYYY | `09-05-2025` |
| `typ` | text | `WT9` |
| `imatrikulace` | text | `PUS19` |
| `pilot` | příjmení | `Houdek` |
| `ukol` | text | `Prostor` |
| `start` | HH:MM | `16:57` |
| `pristani` | HH:MM | `17:12` |
| `doba` | hodiny (2 des. místa) | `0.25` |
| `pripojeno` | ID kluzáku u vleku, jinak prázdné | *(prázdné)* |

Příklad prvních dvou řádků souboru:

```
id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
25;09-05-2025;WT9;PUS19;Houdek;Prostor;16:57;17:12;0.25;
```

### Velký export (více než 1 000 záznamů)

Při exportu přesahujícím 1 000 záznamů systém zpracování spustí na pozadí a po dokončení automaticky nabídne soubor ke stažení. Na stránce se zobrazí průběžná informace o stavu.

---

## 7 Import letů z CSV

Import slouží pro hromadné vložení historických letových záznamů z papírové evidence. Funkce je primárně určena k jednorázové migraci dat.

### Požadavky na soubor

- Formát: CSV
- Kódování: UTF-8 (s nebo bez BOM)
- Oddělovač: středník (`;`)
- První řádek: hlavička — musí obsahovat přesně tyto sloupce ve stejném pořadí:

```
id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
```

- Formát dat: DD-MM-YYYY (například `09-05-2025`)
- Formát časů: HH:MM (například `16:57`)
- Desetinné číslo doby letu: desetinná **tečka** (například `0.25`, ne `0,25`)
- Sloupec `pripojeno`: ID kluzáku v rámci **tohoto souboru**, pokud je let vleku — jinak nechte prázdné

> **Poznámka k sloupci `id`:** Hodnoty v tomto sloupci slouží pouze k propojení záznamů v rámci importního souboru (vazba kluzák ↔ vlečné letadlo přes `pripojeno`). Po importu systém přidělí vlastní interní identifikátory — původní `id` ze souboru se nepoužívá.

### Postup importu

1. Připravte soubor CSV dle výše uvedených požadavků.
2. Klikněte na tlačítko **Import CSV** v panelu filtrů.
3. Vyberte soubor ze svého počítače.
4. Systém soubor nejprve **zvaliduje celý** bez uložení dat a zobrazí souhrn:
   - Počet záznamů připravených k importu.
   - Seznam nalezených chyb (číslo řádku a popis problému).
5. Pokud soubor **neobsahuje chyby**, zobrazí se tlačítko pro potvrzení importu.
6. Klikněte na **Potvrdit import**.
7. Systém uloží všechny záznamy najednou. Importované záznamy získají stav dokončený.

Pokud soubor **obsahuje chyby**, import není možný. Opravte soubor dle zobrazených chybových hlášek a nahrajte ho znovu.

### Nejčastější chyby při importu

| Chybová hláška | Příčina | Oprava |
|---|---|---|
| Hlavička musí být: id;datum;... | Špatné nebo chybějící názvy sloupců v prvním řádku | Zkontrolujte, zda první řádek přesně odpovídá požadovanému formátu |
| Neplatný formát data na řádku X | Datum není ve formátu DD-MM-YYYY | Například `09-05-2025`, nikoli `9.5.2025` ani `2025-05-09` |
| Čas přistání musí být pozdější | Přistání je zadáno dříve než vzlet | Zkontrolujte časy na daném řádku |
| Letadlo 'XY' není evidováno | Imatrikulace ve sloupci `imatrikulace` neodpovídá žádnému letadlu v systému | Ověřte imatrikulaci nebo kontaktujte administrátora DB |
| Očekáváno X sloupců, nalezeno Y | Řádek má jiný počet středníků než hlavička | Zkontrolujte, zda hodnota v některém poli neobsahuje neuvozený středník |

### Příprava souboru v Excelu

Pokud připravujete soubor v Excelu, při ukládání zvolte formát **CSV UTF-8 (oddělovač – středník)** nebo **CSV (s oddělovačem – středník)**. Standardní CSV z Excelu může používat čárku jako oddělovač — v takovém případě soubor před importem upravte.

---

## 8 Správa uživatelů

### Rozsah samostatné správy

Aplikace FlightLog **neobsahuje rozhraní pro správu uživatelských účtů**. Přidání, úprava ani smazání účtů nejsou dostupné přes webové rozhraní pro administrátora.

### Žádost o změnu účtů

Veškeré změny uživatelských účtů (vytvoření, úprava role, deaktivace, smazání) provádí **dodavatel** na základě písemné žádosti zadavatele. V rámci záruční podpory je tato služba dostupná nejvýše **5krát ročně**.

Pro podání žádosti kontaktujte dodavatele prostřednictvím dohodnutého kanálu (e-mail, helpdesk).

### Co uživatel může sám

Každý přihlášený uživatel (včetně administrátora) může samostatně:

- **Změnit vlastní heslo**: po přihlášení přejděte do nastavení účtu a použijte funkci změny hesla.
- **Obnovit zapomenuté heslo**: na přihlašovací stránce klikněte na odkaz **Zapomenuté heslo?** a postupujte dle e-mailu.

### Role uživatelů

Systém rozlišuje dvě role:

| Role | Přístup |
|---|---|
| **Zapisovatel** | Záznam vzletu, záznam přistání |
| **Administrátor** | Přehled a filtrování letů, úpravy, mazání, export a import CSV; přístup ke všem funkcím zapisovatele |

---

## 9 Řešení problémů

### Záznamy v tabulce chybí nebo neodpovídají očekávání

**Příčina:** Je aktivní filtr omezující zobrazené záznamy.

**Řešení:** Klikněte na tlačítko **Zrušit filtr** a ověřte, zda se záznamy zobrazí. Poté případně nastavte filtry znovu.

---

### Tlačítko Export CSV nic nestáhne

**Příčina A:** Aktivní filtry neodpovídají žádnému záznamu — výsledek je prázdný.

**Řešení:** Zrušte filtry a ověřte, zda tabulka zobrazuje data.

**Příčina B:** Export přesahuje 1 000 záznamů a zpracovává se na pozadí.

**Řešení:** Vyčkejte — stránka zobrazuje průběh. Soubor bude nabídnut ke stažení automaticky po dokončení.

**Příčina C:** Prohlížeč zablokoval automatické stahování.

**Řešení:** Povolte stahování souborů pro adresu aplikace v nastavení prohlížeče.

---

### Import hlásí chyby, ale soubor vypadá správně

**Příčina A:** Soubor je uložen v kódování Windows-1250 nebo jiném než UTF-8.

**Řešení:** Otevřete soubor v textovém editoru (Poznámkový blok, Notepad++, VS Code) a uložte ho znovu jako UTF-8. V Excelu při exportu zvolte možnost **CSV UTF-8**.

**Příčina B:** Soubor používá čárku místo středníku jako oddělovač.

**Řešení:** V textovém editoru nahraďte všechny čárky středníky (pozor na hodnoty, které čárku legitimně obsahují — ty je třeba uzavřít do uvozovek).

**Příčina C:** Desetinné číslo v sloupci `doba` používá čárku místo tečky.

**Řešení:** Nahraďte `0,25` za `0.25` ve všech dotčených řádcích.

---

### Nelze upravit ani smazat záznam — tlačítka Upravit / Smazat chybí

**Příčina:** Přihlášen je uživatel s rolí **Zapisovatel**, nikoliv Administrátor.

**Řešení:** Odhlaste se a přihlaste se znovu s administrátorskými přihlašovacími údaji.

---

### Pilot není nalezen při ručním zadání letu

**Příčina:** Pilot není evidován v databázi klubu (ClubDB), nebo je tato databáze dočasně nedostupná.

**Příznak nedostupnosti ClubDB:** Aplikace zobrazí upozornění „Data nemusí být aktuální". V tomto případě systém pracuje se zálohou dat z posledního úspěšného spojení s databází klubu.

**Řešení:**
- Pilota z klubu, který v seznamu chybí, zadejte jako **Hosta** a vepište jeho příjmení ručně.
- Pokud je pilot členem klubu a v seznamu se nikdy nezobrazuje, ověřte s provozovatelem ClubDB, zda má pilot přiřazenou roli `PILOT`.

---

### Stránka se nenačítá nebo zobrazuje chybu

**Řešení:**
1. Obnovte stránku (klávesa F5 nebo Ctrl+R).
2. Pokud chyba přetrvává, odhlaste se a přihlaste se znovu.
3. Pokud problém přetrvává i po přihlášení, kontaktujte dodavatele s popisem chyby a přesným časem výskytu.

---

### Při přihlášení se zobrazí výzva k CAPTCHA

**Příčina:** Bylo zadáno nesprávné heslo třikrát za sebou — systém aktivoval ochranu proti automatizovaným útokům.

**Řešení:** Splňte CAPTCHA výzvu a pokračujte s přihlášením. Pokud si heslem nejste jisti, použijte obnovu hesla přes odkaz **Zapomenuté heslo?**

---

*V případě dotazů nebo problémů, které tato příručka neřeší, kontaktujte dodavatele.*
