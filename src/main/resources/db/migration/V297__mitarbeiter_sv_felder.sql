-- Mitarbeiter um sozialversicherungs- und lohnberechnungs-relevante Felder
-- erweitern.
--
-- * beschaeftigungsart: Steuert, welche SV-Saetze fuer den Mitarbeiter gelten.
--   - REGULAER:           voller AG-/AN-Anteil KV/PV/RV/AV
--   - MINIJOB:            Pauschale AG-Abgaben (s. sv_satz Minijob-Saetze)
--   - GF_SV_PFLICHTIG:    Fremdgeschaeftsfuehrer, voll versichert
--   - GF_SV_FREI:         Beherrschender Gesellschafter-GF, keine SV-Pflicht
-- * krankenkasse_id: FK auf krankenkasse - bestimmt den Zusatzbeitrag.
-- * kinderlos: Aktiviert den PV-Kinderlos-Zuschlag (0,6 % nur AN).
--
-- Idempotent ueber INFORMATION_SCHEMA-Lookup, damit ein Re-Run nicht an
-- "Duplicate column"-Fehlern scheitert (analog V213/V214).

SET @col_beschaeftigungsart = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'beschaeftigungsart'
);
SET @sql_beschaeftigungsart = IF(@col_beschaeftigungsart = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN beschaeftigungsart ENUM(''REGULAER'',''MINIJOB'',''GF_SV_PFLICHTIG'',''GF_SV_FREI'') NOT NULL DEFAULT ''REGULAER''',
    'SELECT 1'
);
PREPARE stmt_b FROM @sql_beschaeftigungsart;
EXECUTE stmt_b;
DEALLOCATE PREPARE stmt_b;

SET @col_krankenkasse = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'krankenkasse_id'
);
SET @sql_krankenkasse = IF(@col_krankenkasse = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN krankenkasse_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_k FROM @sql_krankenkasse;
EXECUTE stmt_k;
DEALLOCATE PREPARE stmt_k;

SET @col_kinderlos = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'kinderlos'
);
SET @sql_kinderlos = IF(@col_kinderlos = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN kinderlos BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_l FROM @sql_kinderlos;
EXECUTE stmt_l;
DEALLOCATE PREPARE stmt_l;

SET @fk_krankenkasse = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = DATABASE()
      AND TABLE_NAME      = 'mitarbeiter'
      AND CONSTRAINT_NAME = 'fk_mitarbeiter_krankenkasse'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql_fk_kk = IF(@fk_krankenkasse = 0,
    'ALTER TABLE mitarbeiter ADD CONSTRAINT fk_mitarbeiter_krankenkasse FOREIGN KEY (krankenkasse_id) REFERENCES krankenkasse(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt_fk FROM @sql_fk_kk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;
