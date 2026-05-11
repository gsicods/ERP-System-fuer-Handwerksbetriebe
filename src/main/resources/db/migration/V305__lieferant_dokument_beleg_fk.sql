-- Verknuepfung LieferantDokument -> Beleg
--
-- Wenn der mobile Beleg-Scanner einen Beleg erfasst und die KI ihn als
-- RECHNUNG oder GUTSCHRIFT mit Lieferant klassifiziert, wird automatisch ein
-- LieferantDokument + LieferantGeschaeftsdokument angelegt. Damit der Datei-
-- Zugriff in der Rechnungsuebersicht funktioniert, zeigt das LieferantDokument
-- per FK auf den urspruenglichen Beleg-Datensatz (der die hochgeladene Datei
-- haelt).
--
-- ON DELETE SET NULL: Wird der Beleg verworfen/geloescht (soft), bleibt das
-- LieferantDokument bestehen, verliert nur den Datei-Verweis (Buchhalter
-- entscheidet ueber das Geschaeftsdokument separat).

-- Spalte hinzufuegen (idempotent)
SET @col_bid = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lieferant_dokument'
      AND COLUMN_NAME = 'beleg_id'
);
SET @sql_bid = IF(@col_bid = 0,
    'ALTER TABLE lieferant_dokument ADD COLUMN beleg_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_bid FROM @sql_bid;
EXECUTE stmt_bid;
DEALLOCATE PREPARE stmt_bid;

-- Foreign Key (idempotent)
SET @fk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lieferant_dokument'
      AND CONSTRAINT_NAME = 'fk_lieferant_dokument_beleg'
);
SET @sql_fk = IF(@fk = 0,
    'ALTER TABLE lieferant_dokument ADD CONSTRAINT fk_lieferant_dokument_beleg FOREIGN KEY (beleg_id) REFERENCES beleg(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- Index (idempotent)
SET @idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lieferant_dokument'
      AND INDEX_NAME = 'idx_lieferant_dokument_beleg'
);
SET @sql_idx = IF(@idx = 0,
    'CREATE INDEX idx_lieferant_dokument_beleg ON lieferant_dokument(beleg_id)',
    'SELECT 1'
);
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
