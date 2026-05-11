-- Belege werden ab sofort auch einer Kostenstelle zugewiesen — die KI agiert
-- beim Belegscan als Agent: sie liest die aktiven Kostenstellen + Sachkonten
-- aus der DB und schlaegt automatisch die passende Kombination vor. Der
-- Stundenlohn-/Verrechnungslohn-Rechner zieht dann alle Belege mit einer
-- Fixkosten-Kostenstelle als zusaetzliche Gemeinkosten in die Kalkulation ein.
--
-- Alle Spalten sind idempotent angelegt — Mehrfach-Ausfuehrung schadet nicht.

-- 1) Echte Zuordnung Kostenstelle ----------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'kostenstelle_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN kostenstelle_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK auf firma_kostenstelle (nur einmal anlegen)
SET @fk_exists := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND constraint_name = 'fk_beleg_kostenstelle'
);
SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE beleg ADD CONSTRAINT fk_beleg_kostenstelle FOREIGN KEY (kostenstelle_id) REFERENCES firma_kostenstelle(id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index fuer die Gemeinkosten-Aggregation im Verrechnungslohn (filtert ueber
-- kostenstelle_id + beleg_datum), und fuer Listings "Belege ohne Kostenstelle"
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND index_name = 'idx_beleg_kostenstelle'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_beleg_kostenstelle ON beleg(kostenstelle_id, beleg_datum)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) KI-Vorschlagsfelder fuer Kostenstelle und Sachkonto -----------------------
-- Wir speichern absichtlich NUR IDs ohne FK — die Vorschlaege sind reine
-- Hilfsinformation; wenn der Buchhalter eine Kostenstelle spaeter loescht,
-- soll der Beleg-Datensatz nicht kaputtgehen und der alte KI-Vorschlag bleibt
-- als historische Notiz erhalten.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'ki_vorgeschlagener_kostenstelle_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN ki_vorgeschlagener_kostenstelle_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'ki_vorgeschlagener_sachkonto_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN ki_vorgeschlagener_sachkonto_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'ki_kostenkonto_confidence'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN ki_kostenkonto_confidence DECIMAL(3,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg'
      AND column_name = 'ki_kostenkonto_begruendung'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE beleg ADD COLUMN ki_kostenkonto_begruendung VARCHAR(500) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
