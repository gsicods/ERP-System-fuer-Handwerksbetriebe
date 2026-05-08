# Verrechnungslohn-Rechner – Plan

Stand: 2026-05-08
Branch: main
Status: Konzept, Bauabschnitt 1 Backend bereits angefangen.

## Ziel

Auf der Seite **Arbeitsplanung > Arbeitsgänge** soll ein Knopf erscheinen:

> "Was muss meine Stunde kosten?"

Der öffnet ein Vorschau-Fenster, in dem der Chef sieht:

- was er an Löhnen zahlt
- was die Firma sonst noch kostet (Gemeinkosten)
- wie viele Stunden er im Jahr wirklich verkaufen kann
- daraus ergibt sich automatisch der Mindest-Verrechnungslohn (Selbstkosten)
- per Schieberegler kann er einen Gewinn-Aufschlag setzen
- pro Abteilung kann er den Wert nach oben/unten korrigieren
- ein Knopf überträgt den Satz auf alle Arbeitsgänge des aktuellen Jahres

Sprache: Handwerker-freundlich, kein Buchhalter-Jargon.

## Daten, die wir brauchen – und woher sie kommen

| Was | Quelle |
|-----|--------|
| Aktuelle Stundenlöhne (zeitlich gestaffelt) | `MitarbeiterStundenlohn` (gueltigAb) |
| AG-Anteile SV (KV/PV/RV/AV) | `SvSatz` + `Mitarbeiter.beschaeftigungsart` + `Mitarbeiter.kinderlos` |
| Zusatzbeitrag Krankenkasse | `Krankenkasse.zusatzbeitragProzent` |
| BG-Beitrag (Unfallversicherung) | `Gewerk.bgSatzProzent` |
| Kalkulatorischer Unternehmerlohn (GF) | `Mitarbeiter.kalkulatorischerLohnMonat` (NEU, V300) |
| Geldwerte Vorteile (Auto/Telefon GF) | `Mitarbeiter.geldwertVorteilMonat` (NEU, V300) |
| Soll-Stunden | `Zeitkonto` / `MonatsSaldo` |
| Urlaub | `Mitarbeiter.jahresUrlaub` + `Abwesenheit` (typ=URLAUB) |
| Krankheit | `Abwesenheit` (typ=KRANKHEIT), Default wenn keine Daten |
| Feiertage | `Feiertag` (mit Bundesland) |
| Interne / Gewährleistungs-Stunden | `Zeitbuchung` × `Projekt.projektArt` (INTERN, GARANTIE) |
| Gemeinkosten | `LieferantDokumentProjektAnteil` × `Kostenstelle.istFixkosten` |

## Wie der Rechner rechnet

### Lohn-Block (Jahres-Kosten)

Für jeden aktiven Mitarbeiter:

- **Normal** (kein GF): Brutto/Monat × 12 + AG-Anteile (KV/PV/RV/AV anteilig je nach `beschaeftigungsart`, plus KK-Zusatzbeitrag, plus BG)
- **Geschäftsführer** (`istGeschaeftsfuehrer = true`): `kalkulatorischerLohnMonat × 12 + geldwertVorteilMonat × 12` – KEINE Sozialversicherung, KEINE Lohnnebenkosten

→ Summe = "Was kosten mich meine Leute pro Jahr?"

### Verkäufliche-Stunden-Block

Für **jeden** Mitarbeiter (egal ob GF oder nicht) gilt dieselbe Logik:

```
Sollstunden (aus Zeitkonto)
  − Urlaubsstunden
  − Krankheitsstunden
  − Interne Stunden (Zeitbuchungen auf Projekte mit projektArt = INTERN oder GARANTIE)
= Verkäufliche Stunden des Mitarbeiters
```

Geschäftsführer: Wenn er auch produktiv arbeitet, bucht er produktive Zeit auf reguläre Projekte → fließt 1:1 in die verkäuflichen Stunden ein. Sein "Chef-Quatsch" wird über interne Projekte gebucht und fällt automatisch raus.

Mitarbeiter ohne Zeitbuchungen (z.B. reine Bürokraft, die nicht in der Zeiterfassung ist): liefert 0 verkäufliche Stunden – sein Lohn erhöht den Verrechnungssatz korrekt.

→ Summe = "Wie viele Stunden kann ich im Jahr verkaufen?"

### Gemeinkosten-Block

Aus `LieferantDokumentProjektAnteil`:

- nur Anteile mit `kostenstelle != null` (Projekt-Anteile sind schon im Projekt verbucht)
- Kostenstelle muss `istFixkosten = true` haben
- `isStreckungAktivFuerJahr(zielJahr)` muss true sein
- Summe der `getJahresanteil()`

→ Summe = "Was kostet die Firma sonst noch im Jahr?"

### Verrechnungslohn

```
Selbstkosten   = (Lohn + Gemeinkosten) ÷ Verkäufliche Stunden
Verkaufspreis  = Selbstkosten × (1 + Gewinn-%)
```

Pro Abteilung kann der User dann noch +/− € draufschlagen (z.B. Schweißerei teurer, Schlosserei günstiger).

## Zwei Modi: Rückwirkend (genau) vs. Laufendes Jahr (Schätzung)

Beim Klick auf den Button wählt der User zuerst **welches Jahr** er rechnen will. Daraus ergibt sich der Modus:

### Modus A – Rückwirkend (abgeschlossenes Jahr, z.B. 2025)

Hier wird mit **echten Ist-Daten** gerechnet, nichts geschätzt:

- **Lohnsumme**: aus den Lohnabrechnungen des Jahres (`Lohnabrechnung.bruttolohn`), GF mit kalkulatorischem Wert × 12.
- **Verkäufliche Stunden** pro Mitarbeiter:
  - SUMME aller Zeitbuchungen, deren Projekt **produktiv** ist (`projektArt = PAUSCHAL` oder `REGIE`).
  - Soll-Stunden interessieren hier NICHT – es zählt, was wirklich gearbeitet wurde.
- **Interne / unproduktive Stunden**: SUMME der Zeitbuchungen auf `INTERN` / `GARANTIE` (nur zur Anzeige, nicht zur Rechnung).
- **Gemeinkosten**: aus `LieferantDokumentProjektAnteil` für genau dieses Jahr.

**Daten-Check vor Berechnung:** Bevor der Rechner Werte zeigt, prüft er pro Mitarbeiter:
- Hat der MA in diesem Jahr Zeitbuchungen über mind. 90 % der Soll-Tage?
- Sind Lohnabrechnungen für alle 12 Monate vorhanden?

Fehlen Daten, zeigt der Rechner einen klaren Hinweis:

> ⚠️ Für 2025 fehlen Daten:
> – Hans Müller: Zeitbuchungen erst ab 03.11.2025 (vorher keine Zeiterfassung)
> – Petra Schmidt: Lohnabrechnungen Jan–Okt fehlen
> Die Berechnung wäre nicht aussagekräftig. Lieber das laufende Jahr hochrechnen?

### Modus B – Laufendes Jahr (Hochrechnung, z.B. 2026)

Hier rechnen wir das aktuelle Jahr **hoch** auf Basis von dem, was bisher ist + Schätzungen vom User:

- **Lohnsumme**: aktuelle Stundenlöhne × Sollstunden + AG-SV (12-Monats-Vollwert).
- **Verkäufliche Stunden** pro Mitarbeiter: berechnet aus zwei User-Eingaben am Anfang des Dialogs:
  - "Wie viel Prozent eurer Arbeitszeit verkauft ihr im Schnitt an Kunden? (z.B. 75 %)"
  - "Wie viel Prozent geht für interne Sachen drauf? (z.B. 20 %)"
  - Rest = Krankheit/Urlaub.
  - Diese Prozente werden **auf alle Mitarbeiter angewendet** (außer GF, die haben oft eigene Prozente – ggf. zweites Eingabefeld).
- **Gemeinkosten**: aus `LieferantDokumentProjektAnteil` Year-To-Date × Faktor (12 ÷ laufende Monate). Streckungs-Anteile bleiben unverändert.

**Hinweis im Dialog (oben):**

> 📊 Du rechnest ein laufendes Jahr (2026). Manche Werte werden hochgerechnet.
> Sag mir bitte erst, wie viel Prozent eurer Stunden ihr normalerweise verkauft.

### Daten-Voraussetzung für die Auswahl

Im Jahresauswahl-Dropdown nur Jahre anbieten, in denen es überhaupt Zeitbuchungen gab. Erstes mögliches Jahr: 2025 (mit Daten-Lücken-Warnung), normales Arbeitsjahr ab 2026.

## Bauabschnitte

### 1. Mitarbeiter-Editor (Geschäftsführer-Felder)

**Backend** ✅ erledigt:
- Migration `V300__mitarbeiter_geschaeftsfuehrer.sql` (idempotent)
- `Mitarbeiter`-Domain um 3 Felder erweitert
- `MitarbeiterDto` + `MitarbeiterErstellenDto` erweitert
- `MitarbeiterService.save()` und `mapToDto()` durchgereicht

**Frontend** offen:
- Im `MitarbeiterEditor` neuer Abschnitt "Geschäftsführer" mit Checkbox
- Wenn Checkbox aktiv: zwei Eingabefelder
  - "Was möchtest du dir pro Monat als Lohn rechnen? (€)"
  - "Auto/Telefon/Privatanteile pro Monat (€)" – optional
- Mehrere GF möglich, einfach mehrere Mitarbeiter mit Haken
- Wording-Beispiel:
  > Bist du / ist diese Person Geschäftsführer/in?
  > [✓] Ja, dann nehmen wir statt einem echten Lohn deinen Wunschlohn für die Kalkulation.

### 2. Backend Rechner-Service

Neu:
- `service/VerrechnungslohnService.java`
  - `berechne(int jahr)` → liefert `VerrechnungslohnErgebnisDto`
  - intern: `berechneLohnsumme()`, `berechneVerkaeuflicheStunden()`, `berechneGemeinkosten()`
- `controller/VerrechnungslohnController.java`
  - `GET /api/verrechnungslohn?jahr=2026` → Vorschau-Daten
  - `POST /api/verrechnungslohn/uebernehmen` → setzt `ArbeitsgangStundensatz` für alle Arbeitsgänge des Jahres (mit per-Abteilung-Override)
- `dto/VerrechnungslohnErgebnisDto.java`
  - alle Detail-Werte (pro Mitarbeiter Brutto, AG-SV, Sollstunden, …)
  - Lohnsumme, Verkäufliche Stunden, Gemeinkosten gesamt
  - Default-Flags pro Wert (für UI-Anzeige "ist Default")

### 3. Frontend Rechner-Dialog

Auf `ArbeitsgangEditor` neuer Button "Was muss meine Stunde kosten?". Öffnet Dialog mit:

- **Block 1 – Meine Leute** (aufklappbar, default zugeklappt): Tabelle pro MA mit Brutto, AG-Anteile, Summe → Überschreibbar
- **Block 2 – Was kostet die Firma sonst noch**: Tabelle pro Kostenstelle mit Jahres-Wert → Überschreibbar
- **Block 3 – Verkäufliche Stunden**: Tabelle pro MA mit Soll/Urlaub/Krank/Intern → Überschreibbar, Default-Marker sichtbar
- **Block 4 – Was muss meine Stunde kosten?**: Selbstkosten, Schieberegler Gewinn, Verkaufspreis
- **Aufschlag/Abschlag pro Abteilung**: kleine Tabelle
- **Knopf** "Auf alle Arbeitsgänge für 2026 übernehmen"

## Status-Checkliste

- [x] Konzept festgehalten (dieses Dokument)
- [x] Bauabschnitt 1 Backend (Migration + Domain + DTO + Service)
- [ ] Bauabschnitt 1 Frontend (Mitarbeiter-Editor: Checkbox + 2 Felder)
- [ ] Backend-Boot-Test mit V300 gegen lokale DB
- [ ] Bauabschnitt 2 Backend (VerrechnungslohnService + Controller + DTO)
- [ ] Bauabschnitt 3 Frontend (Rechner-Dialog auf ArbeitsgangEditor)
- [ ] Globaler Übernehmen-Endpoint mit Per-Abteilung-Override
- [ ] Smoke-Test im Browser mit Test-Daten

## Offene Punkte (vor Bau klären)

- Krankheitstage-Default: 8 oder 10? → Vorschlag 8.
- Interne-Stunden-Default: 5 % oder höher? → Vorschlag 5 %, im Dialog leicht änderbar.
- Bundesland Feiertage: aus `Firmeninformation` ziehen, sonst BY-Default.
- Sollen die Pro-Abteilung-Aufschläge persistiert werden (für nächstes Jahr) oder jedes Mal frisch eingegeben? → Vorschlag: persistieren auf `Abteilung.verrechnungslohnAufschlag` (späterer Bauabschnitt).
- "Übernehmen" überschreibt bestehende Sätze für das Jahr – mit Bestätigungs-Dialog. Soll vorher ein Backup/Audit-Eintrag geschrieben werden?
