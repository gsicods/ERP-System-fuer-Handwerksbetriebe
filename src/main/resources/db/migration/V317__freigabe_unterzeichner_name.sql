-- Beweissicherung der digitalen Freigabe-Annahme: Vor- und Nachname der konkret
-- klickenden Person (bei Firmenkunden = vertretungsberechtigte Person).
--
-- Bewusst NULLABLE: Vor V317 akzeptierte Freigaben besitzen den Namen nicht.
-- Die Pflicht für neue Akzeptanzen wird im Service erzwungen
-- (DokumentFreigabeService.akzeptiere) und im Request-DTO per Bean-Validation
-- — nicht über die DB. Migration ist damit idempotent und ändert keine Altdaten.

-- Idempotent: jede Spalte einzeln nur hinzufügen, wenn sie nicht existiert.
SET @col_vorname := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dokument_freigabe'
      AND column_name = 'unterzeichner_vorname'
);
SET @sql := IF(@col_vorname = 0,
    'ALTER TABLE dokument_freigabe ADD COLUMN unterzeichner_vorname VARCHAR(80) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_nachname := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dokument_freigabe'
      AND column_name = 'unterzeichner_nachname'
);
SET @sql := IF(@col_nachname = 0,
    'ALTER TABLE dokument_freigabe ADD COLUMN unterzeichner_nachname VARCHAR(80) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_name := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dokument_freigabe'
      AND column_name = 'unterzeichner_name'
);
SET @sql := IF(@col_name = 0,
    'ALTER TABLE dokument_freigabe ADD COLUMN unterzeichner_name VARCHAR(160) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
