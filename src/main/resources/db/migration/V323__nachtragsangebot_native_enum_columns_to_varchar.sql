-- Nachtragsangebot: repariert Bestandsdatenbanken mit nativen MySQL/MariaDB-ENUM-Spalten.
--
-- V322 entfernt Enum-CHECK-Constraints, deckt aber keine Tabellen ab, die frueher
-- von Hibernate als native ENUM-Spalten angelegt wurden. In diesen Datenbanken
-- fuehrt NACHTRAGSANGEBOT zu "Data truncated for column ...", weil der native
-- ENUM-Wertebereich den neuen Wert nicht kennt. Die Entities deklarieren diese
-- Felder als @Enumerated(STRING) + VARCHAR-Laenge; deshalb normalisieren wir die
-- Spalten hier idempotent auf VARCHAR.

SET @agd_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ausgangs_geschaeftsdokument'
      AND COLUMN_NAME = 'typ'
);
SET @sql_agd := IF(@agd_exists = 0, 'SELECT 1',
    'ALTER TABLE `ausgangs_geschaeftsdokument` MODIFY COLUMN `typ` VARCHAR(30) NOT NULL');
PREPARE stmt_agd FROM @sql_agd;
EXECUTE stmt_agd;
DEALLOCATE PREPARE stmt_agd;

SET @fta_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'formular_template_assignment'
      AND COLUMN_NAME = 'dokumenttyp_enum'
);
SET @sql_fta := IF(@fta_exists = 0, 'SELECT 1',
    'ALTER TABLE `formular_template_assignment` MODIFY COLUMN `dokumenttyp_enum` VARCHAR(30) NOT NULL');
PREPARE stmt_fta FROM @sql_fta;
EXECUTE stmt_fta;
DEALLOCATE PREPARE stmt_fta;

SET @tbe_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'textbaustein_dokumenttyp_enum'
      AND COLUMN_NAME = 'dokumenttyp'
);
SET @sql_tbe := IF(@tbe_exists = 0, 'SELECT 1',
    'ALTER TABLE `textbaustein_dokumenttyp_enum` MODIFY COLUMN `dokumenttyp` VARCHAR(30) NULL');
PREPARE stmt_tbe FROM @sql_tbe;
EXECUTE stmt_tbe;
DEALLOCATE PREPARE stmt_tbe;
