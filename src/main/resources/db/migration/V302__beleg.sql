-- Belege & Kasse
--
-- Neues Buchhaltungs-Modul (QuickBooks-artig). Belege sind eigenstaendige
-- Dokumente, die im Gegensatz zu LieferantDokument auch OHNE Lieferanten
-- erfasst werden koennen (Kassenbeleg, Privatentnahme, Tankquittung etc.).
--
-- Workflow:
-- 1. Mobile: Mitarbeiter (Abteilung Buchhaltung) scannt Beleg -> Status=NEU,
--    KIAnalyse=PENDING, Datei zwingend. Mitarbeiter geht sofort weiter zum
--    naechsten Beleg, keine Validierung am Handy.
-- 2. Backend: Async KI-Analyse fuellt geschaeftsdaten + ki_extraktion_json.
--    Status bleibt NEU, ki_analyse_status=DONE.
-- 3. PC: Buchhalter sieht im Eingang alle NEU-Belege, korrigiert ggf. KI-
--    Vorschlaege, weist Lieferanten/Kategorie zu -> Status=VALIDIERT.
--
-- HARTE REGEL: gespeicherter_dateiname IST NICHT NULL. Es gibt keine Buchung
-- ohne Beleg-Anhang.

SET @tbl_beleg = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'beleg'
);

SET @sql_beleg = IF(@tbl_beleg = 0,
    'CREATE TABLE beleg (
        id BIGINT NOT NULL AUTO_INCREMENT,
        beleg_kategorie VARCHAR(40) NOT NULL DEFAULT ''UNZUGEORDNET'',
        status VARCHAR(20) NOT NULL DEFAULT ''NEU'',
        ki_analyse_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
        beleg_datum DATE NULL,
        beleg_nummer VARCHAR(100) NULL,
        beschreibung VARCHAR(500) NULL,
        betrag_netto DECIMAL(15,2) NULL,
        betrag_brutto DECIMAL(15,2) NULL,
        mwst_satz DECIMAL(5,2) NULL,
        zahlungsart VARCHAR(40) NULL,
        lieferant_id BIGINT NULL,
        ki_vorgeschlagener_lieferant VARCHAR(255) NULL,
        ki_confidence DECIMAL(3,2) NULL,
        ki_extraktion_json LONGTEXT NULL,
        ki_fehler_text VARCHAR(1000) NULL,
        original_dateiname VARCHAR(255) NULL,
        gespeicherter_dateiname VARCHAR(255) NOT NULL,
        mime_type VARCHAR(120) NULL,
        upload_datum DATETIME NOT NULL,
        uploaded_by_id BIGINT NULL,
        validiert_am DATETIME NULL,
        validiert_von_id BIGINT NULL,
        notiz VARCHAR(1000) NULL,
        PRIMARY KEY (id),
        CONSTRAINT fk_beleg_lieferant FOREIGN KEY (lieferant_id) REFERENCES lieferanten(id) ON DELETE SET NULL,
        CONSTRAINT fk_beleg_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES mitarbeiter(id) ON DELETE SET NULL,
        CONSTRAINT fk_beleg_validiert_von FOREIGN KEY (validiert_von_id) REFERENCES mitarbeiter(id) ON DELETE SET NULL
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT 1'
);
PREPARE stmt_beleg FROM @sql_beleg;
EXECUTE stmt_beleg;
DEALLOCATE PREPARE stmt_beleg;

-- Indizes (idempotent ueber INFORMATION_SCHEMA.STATISTICS)

SET @idx_status = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND INDEX_NAME = 'idx_beleg_status'
);
SET @sql_idx_status = IF(@idx_status = 0,
    'CREATE INDEX idx_beleg_status ON beleg(status)',
    'SELECT 1'
);
PREPARE stmt_idx_status FROM @sql_idx_status;
EXECUTE stmt_idx_status;
DEALLOCATE PREPARE stmt_idx_status;

SET @idx_kat = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND INDEX_NAME = 'idx_beleg_kategorie'
);
SET @sql_idx_kat = IF(@idx_kat = 0,
    'CREATE INDEX idx_beleg_kategorie ON beleg(beleg_kategorie)',
    'SELECT 1'
);
PREPARE stmt_idx_kat FROM @sql_idx_kat;
EXECUTE stmt_idx_kat;
DEALLOCATE PREPARE stmt_idx_kat;

SET @idx_datum = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND INDEX_NAME = 'idx_beleg_datum'
);
SET @sql_idx_datum = IF(@idx_datum = 0,
    'CREATE INDEX idx_beleg_datum ON beleg(beleg_datum)',
    'SELECT 1'
);
PREPARE stmt_idx_datum FROM @sql_idx_datum;
EXECUTE stmt_idx_datum;
DEALLOCATE PREPARE stmt_idx_datum;

-- Berechtigung wird ueber das bestehende Schema abteilung_dokument_berechtigung
-- abgewickelt: Neuer Eintrag dokument_typ='BELEG' mit darf_scannen / darf_sehen.
-- Der LieferantDokumentTyp-Konverter wird im Java-Layer um den Wert 'BELEG'
-- erweitert; die Admin-UI 'Lieferanten-Dokumentenrechte' iteriert das Enum und
-- zeigt 'BELEG' damit automatisch an.
--
-- Wir seeden hier einen Default-Berechtigungseintrag (darfSehen+darfScannen)
-- fuer jede Abteilung deren Name 'buchhaltung' enthaelt, damit Buchhalter sofort
-- scannen koennen, ohne dass der Admin den Eintrag manuell anlegen muss.
-- Existiert bereits ein Eintrag (egal mit welchen Flags) wird er nicht
-- ueberschrieben.

INSERT INTO abteilung_dokument_berechtigung (abteilung_id, dokument_typ, darf_sehen, darf_scannen)
SELECT a.id, 'BELEG', TRUE, TRUE
FROM abteilung a
WHERE LOWER(a.name) LIKE '%buchhaltung%'
  AND NOT EXISTS (
    SELECT 1 FROM abteilung_dokument_berechtigung b
    WHERE b.abteilung_id = a.id AND b.dokument_typ = 'BELEG'
  );
