-- Steuert pro Abteilung, ob Mitarbeiter eine Push-Benachrichtigung auf dem
-- Handy-Sperrbildschirm bekommen, sobald eine neue Anfrage über das öffentliche
-- Webseiten-Funnel-Formular eingegangen ist.
-- Default true: Bei Bestandsinstallationen sollen alle bestehenden Abteilungen
-- den Push standardmäßig erhalten – Admin kann pro Abteilung deaktivieren.
-- Idempotent: nur hinzufügen wenn die Spalte fehlt.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'abteilung'
      AND column_name = 'darf_webseiten_anfragen_pushen'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE abteilung ADD COLUMN darf_webseiten_anfragen_pushen BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
