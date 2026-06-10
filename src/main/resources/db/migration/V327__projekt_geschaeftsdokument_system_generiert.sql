-- Kennzeichnet ob ein ProjektGeschaeftsdokument vom System automatisch erstellt wurde
-- (via AusgangsGeschaeftsDokument-Buchung) oder manuell im Offene-Posten-Editor erfasst wurde.
-- Nur systemgenerierte Einträge erhalten automatische Zahlungserinnerungen per E-Mail.

ALTER TABLE projekt_dokument
    ADD COLUMN system_generiert TINYINT(1) NOT NULL DEFAULT 0;

-- Backfill: Alle ProjektGeschaeftsdokument-Zeilen, deren dokumentid einer
-- Dokumentnummer in ausgangs_geschaeftsdokument entspricht, wurden vom System
-- via erstelleOffenenPostenEintrag() erzeugt.
-- Dieser JOIN ist robust gegenüber nachträglichen Dateinamen-Änderungen durch
-- speicherePdfFuerDokument(), die den ursprünglichen "ausgangs-dok-*"-Dateinamen
-- durch eine UUID ersetzen.
UPDATE projekt_dokument pd
INNER JOIN ausgangs_geschaeftsdokument agd
    ON pd.dokumentid = agd.dokument_nummer
SET pd.system_generiert = 1
WHERE pd.system_generiert = 0;

-- Ergänzend: Einträge, die noch den synthetischen Dateinamen tragen und noch nicht
-- über den JOIN erfasst wurden (sollte nicht vorkommen, aber als Sicherheitsnetz).
UPDATE projekt_dokument
SET system_generiert = 1
WHERE system_generiert = 0
  AND gespeicherter_dateiname REGEXP '^ausgangs-dok-[0-9]+\\.pdf$';
