-- Beleg-Aufteilung: Belege koennen entweder VOLLSTAENDIG fuer die Firma sein
-- oder TEILWEISE (z.B. Supermarkt-Bon, wo nur der Kaffee fuer's Geschaeft ist).
-- Bei TEILWEISE extrahiert die KI alle Positionen, der Nutzer waehlt am Handy
-- per Checkbox welche zur Firma gehoeren. Die Summen fuer die Firma werden
-- automatisch inkl. MwSt-Aufteilung berechnet und unter betrag_firma_* gespeichert.
--
-- Alle Spalten sind idempotent angelegt — Mehrfach-Ausfuehrung schadet nicht.

-- 1) Spalten am Beleg ----------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'aufteilungs_modus'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE beleg ADD COLUMN aufteilungs_modus ENUM('VOLLSTAENDIG','TEILWEISE') NOT NULL DEFAULT 'VOLLSTAENDIG'",
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'betrag_firma_netto'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN betrag_firma_netto DECIMAL(15,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'betrag_firma_brutto'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN betrag_firma_brutto DECIMAL(15,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'betrag_firma_mwst'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN betrag_firma_mwst DECIMAL(15,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Tabelle beleg_position ----------------------------------------------------
-- Enthaelt einzelne Positionen einer aufgeteilten Rechnung (KI-extrahiert).
-- ist_fuer_firma=true heisst: User hat per Checkbox bestaetigt, dass diese
-- Position auf das Geschaeft gebucht werden soll. Initialer Default = false.
--
-- mwst_satz pro Position erlaubt gemischte MwSt-Saetze auf einem Bon
-- (z.B. Lebensmittel 7% + Kaffee 19% oder Werkzeug + Verbrauchsmaterial).
CREATE TABLE IF NOT EXISTS beleg_position (
    id BIGINT NOT NULL AUTO_INCREMENT,
    beleg_id BIGINT NOT NULL,
    sortierung INT NOT NULL DEFAULT 0,
    beschreibung VARCHAR(500) NOT NULL,
    menge DECIMAL(15,3) NULL,
    einheit VARCHAR(20) NULL,
    einzelpreis DECIMAL(15,4) NULL,
    betrag_netto DECIMAL(15,2) NULL,
    betrag_brutto DECIMAL(15,2) NULL,
    mwst_satz DECIMAL(5,2) NULL,
    ist_fuer_firma TINYINT(1) NOT NULL DEFAULT 0,
    erstellt_am DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_beleg_position_beleg FOREIGN KEY (beleg_id)
        REFERENCES beleg(id) ON DELETE CASCADE,
    KEY idx_beleg_position_beleg (beleg_id, sortierung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
