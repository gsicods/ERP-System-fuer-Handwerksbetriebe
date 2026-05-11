-- Rückbau der in V253 angelegten Zwei-Faktor-Spalten.
-- Entscheidung: SMS/PIN-Verifizierung ist für einen 5-Mann-Handwerksbetrieb Overkill —
-- der bestehende Workflow (Token-Link in der Mail + Checkbox-Annahme + sofortige
-- Auto-Auftragsbestätigung als PDF) deckt die Beweislage gerichtsfest ab.
--
-- Idempotent: prüft jede Spalte einzeln per INFORMATION_SCHEMA, damit
-- die Migration auch dann sauber läuft, wenn V253 in einer Umgebung
-- nie ausgeführt wurde oder die Spalten manuell schon entfernt wurden.

-- dokument_freigabe.verifizierungs_modus
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'verifizierungs_modus');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN verifizierungs_modus',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- dokument_freigabe.code_hash
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'code_hash');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN code_hash',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- dokument_freigabe.code_versuche
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'code_versuche');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN code_versuche',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- dokument_freigabe.code_gueltig_bis
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'code_gueltig_bis');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN code_gueltig_bis',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- dokument_freigabe.code_letzter_versand
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'code_letzter_versand');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN code_letzter_versand',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- dokument_freigabe.handynummer_snapshot
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dokument_freigabe'
      AND COLUMN_NAME  = 'handynummer_snapshot');
SET @sql = IF(@col = 1,
    'ALTER TABLE dokument_freigabe DROP COLUMN handynummer_snapshot',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- kunde.freigabe_pin
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'kunde'
      AND COLUMN_NAME  = 'freigabe_pin');
SET @sql = IF(@col = 1,
    'ALTER TABLE kunde DROP COLUMN freigabe_pin',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
