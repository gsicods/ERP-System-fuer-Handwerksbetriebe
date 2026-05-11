-- Beleg-Erweiterung fuer Auto-Eingangsrechnung + belegfreie Umbuchung
--
-- Neue Spalten:
--   dokument_typ    -> KI-Klassifikation (RECHNUNG/GUTSCHRIFT/...); steuert die
--                      Auto-Erzeugung eines LieferantGeschaeftsdokument-Eintrags.
--                      Wert null = noch nicht analysiert oder kein Geschaeftsdokument.
--   ist_umbuchung   -> belegfreie Buchhaltungs-Bewegung (Privat->Firma, Kasse->Bank).
--                      Wenn TRUE darf gespeicherter_dateiname NULL sein.
--
-- Lockerung:
--   gespeicherter_dateiname wird nullable. Service erzwingt weiterhin: NULL nur
--   bei ist_umbuchung=TRUE, fuer alle anderen Belege bleibt die Datei pflicht.
--   Java-Layer + Tests sichern das ab (DSGVO/GoBD-konform: jede gewoehnliche
--   Buchung hat weiterhin einen Beleg-Anhang).

-- 1) dokument_typ hinzufuegen (idempotent)
SET @col_dt = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND COLUMN_NAME = 'dokument_typ'
);
SET @sql_dt = IF(@col_dt = 0,
    'ALTER TABLE beleg ADD COLUMN dokument_typ ENUM(''ANGEBOT'',''AUFTRAGSBESTAETIGUNG'',''LIEFERSCHEIN'',''RECHNUNG'',''GUTSCHRIFT'',''SONSTIG'',''BELEG'') NULL AFTER beleg_kategorie',
    'SELECT 1'
);
PREPARE stmt_dt FROM @sql_dt;
EXECUTE stmt_dt;
DEALLOCATE PREPARE stmt_dt;

-- 2) ist_umbuchung hinzufuegen (idempotent)
SET @col_iu = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND COLUMN_NAME = 'ist_umbuchung'
);
SET @sql_iu = IF(@col_iu = 0,
    'ALTER TABLE beleg ADD COLUMN ist_umbuchung TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_iu FROM @sql_iu;
EXECUTE stmt_iu;
DEALLOCATE PREPARE stmt_iu;

-- 3) gespeicherter_dateiname nullable machen (idempotent)
SET @nullable_dn = (
    SELECT IS_NULLABLE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND COLUMN_NAME = 'gespeicherter_dateiname'
);
SET @sql_dn = IF(@nullable_dn = 'NO',
    'ALTER TABLE beleg MODIFY COLUMN gespeicherter_dateiname VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_dn FROM @sql_dn;
EXECUTE stmt_dn;
DEALLOCATE PREPARE stmt_dn;

-- 4) GoBD-Invariante als DB-CHECK-Constraint:
-- Jede gewoehnliche Buchung MUSS eine Beleg-Datei haben. NULL nur erlaubt,
-- wenn ist_umbuchung=TRUE (Privatentnahme, Privat->Firma, Kasse->Bank).
-- Idempotent ueber INFORMATION_SCHEMA.CHECK_CONSTRAINTS (MySQL >= 8.0.16).
SET @chk = (
    SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'chk_beleg_datei_oder_umbuchung'
);
SET @sql_chk = IF(@chk = 0,
    'ALTER TABLE beleg ADD CONSTRAINT chk_beleg_datei_oder_umbuchung CHECK (ist_umbuchung = 1 OR gespeicherter_dateiname IS NOT NULL)',
    'SELECT 1'
);
PREPARE stmt_chk FROM @sql_chk;
EXECUTE stmt_chk;
DEALLOCATE PREPARE stmt_chk;
