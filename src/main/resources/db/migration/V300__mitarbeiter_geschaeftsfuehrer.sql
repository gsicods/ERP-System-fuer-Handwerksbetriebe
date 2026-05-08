-- Mitarbeiter um Geschaeftsfuehrer-Flag und kalkulatorische Lohn-Felder erweitern.
--
-- Hintergrund:
-- Fuer den Verrechnungslohn-Rechner brauchen wir, statt eines echten
-- SV-pflichtigen Bruttolohns, einen "kalkulatorischen Unternehmerlohn" je
-- Geschaeftsfuehrer (was er fuer sich pro Monat ansetzen moechte). Optional
-- koennen geldwerte Vorteile (Auto, Telefon o.ae.) als pauschaler Monatsbetrag
-- dazu kommen.
--
-- Felder:
-- * ist_geschaeftsfuehrer: Checkbox im MitarbeiterEditor. Mehrere GF moeglich.
-- * kalkulatorischer_lohn_monat: Was sich der GF pro Monat als Lohn rechnet.
-- * geldwert_vorteil_monat: Pauschal Privatanteile (z.B. PKW), die die Firma
--   traegt und die im Verrechnungslohn als Lohnkosten zaehlen sollen.
--
-- Idempotent ueber INFORMATION_SCHEMA-Lookup, damit ein Re-Run nicht an
-- "Duplicate column"-Fehlern scheitert (analog V297).

SET @col_gf = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'ist_geschaeftsfuehrer'
);
SET @sql_gf = IF(@col_gf = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN ist_geschaeftsfuehrer BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_gf FROM @sql_gf;
EXECUTE stmt_gf;
DEALLOCATE PREPARE stmt_gf;

SET @col_kalk = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'kalkulatorischer_lohn_monat'
);
SET @sql_kalk = IF(@col_kalk = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN kalkulatorischer_lohn_monat DECIMAL(12,2) NULL',
    'SELECT 1'
);
PREPARE stmt_kalk FROM @sql_kalk;
EXECUTE stmt_kalk;
DEALLOCATE PREPARE stmt_kalk;

SET @col_gw = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'mitarbeiter'
      AND COLUMN_NAME  = 'geldwert_vorteil_monat'
);
SET @sql_gw = IF(@col_gw = 0,
    'ALTER TABLE mitarbeiter ADD COLUMN geldwert_vorteil_monat DECIMAL(12,2) NULL',
    'SELECT 1'
);
PREPARE stmt_gw FROM @sql_gw;
EXECUTE stmt_gw;
DEALLOCATE PREPARE stmt_gw;
