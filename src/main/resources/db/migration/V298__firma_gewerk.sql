-- Firma um Gewerk-Zuordnung und BG-Satz-Override erweitern.
--
-- gewerk_id schlaegt einen Default-BG-Satz vor (siehe gewerk.bg_satz_prozent).
-- bg_satz_override erlaubt, den tatsaechlichen Beitragssatz aus dem
-- Beitragsbescheid einzutragen, der den Gewerk-Default ueberschreibt. Wenn
-- override NULL ist, gilt der Gewerk-Satz.
--
-- Idempotent ueber INFORMATION_SCHEMA-Lookup (analog V214).

SET @col_gewerk_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'firmeninformation'
      AND COLUMN_NAME  = 'gewerk_id'
);
SET @sql_gewerk = IF(@col_gewerk_id = 0,
    'ALTER TABLE firmeninformation ADD COLUMN gewerk_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_g FROM @sql_gewerk;
EXECUTE stmt_g;
DEALLOCATE PREPARE stmt_g;

SET @col_bg_override = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'firmeninformation'
      AND COLUMN_NAME  = 'bg_satz_override'
);
SET @sql_bg = IF(@col_bg_override = 0,
    'ALTER TABLE firmeninformation ADD COLUMN bg_satz_override DECIMAL(5,2) NULL',
    'SELECT 1'
);
PREPARE stmt_bg FROM @sql_bg;
EXECUTE stmt_bg;
DEALLOCATE PREPARE stmt_bg;

SET @fk_gewerk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = DATABASE()
      AND TABLE_NAME      = 'firmeninformation'
      AND CONSTRAINT_NAME = 'fk_firmeninformation_gewerk'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql_fk = IF(@fk_gewerk = 0,
    'ALTER TABLE firmeninformation ADD CONSTRAINT fk_firmeninformation_gewerk FOREIGN KEY (gewerk_id) REFERENCES gewerk(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;
