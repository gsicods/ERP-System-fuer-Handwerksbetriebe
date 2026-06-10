-- Kennzeichnet ob ein ProjektGeschaeftsdokument vom System automatisch erstellt wurde
-- (via AusgangsGeschaeftsDokument-Buchung) oder manuell im Offene-Posten-Editor erfasst wurde.
-- Nur systemgenerierte Einträge erhalten automatische Zahlungserinnerungen per E-Mail.

ALTER TABLE projekt_geschaeftsdokument
    ADD COLUMN system_generiert TINYINT(1) NOT NULL DEFAULT 0;

-- Backfill: Einträge mit dem synthetischen Dateinamen "ausgangs-dok-*.pdf" wurden
-- vom System via erstelleOffenenPostenEintrag() erzeugt.
-- projekt_geschaeftsdokument.id = projekt_dokument.id (JOINED inheritance)
UPDATE projekt_geschaeftsdokument pg
INNER JOIN projekt_dokument pd ON pd.id = pg.id
SET pg.system_generiert = 1
WHERE pg.system_generiert = 0
  AND pd.gespeicherter_dateiname REGEXP '^ausgangs-dok-[0-9]+\\.pdf$';
