-- Composite-Index fuer die Mobile-Belegliste (GET /api/buchhaltung/mobile/belege).
-- Spring-Data-Query `findTop20ByUploadedByOrderByUploadDatumDesc` filtert auf
-- uploaded_by_id und sortiert nach upload_datum DESC — ohne diesen Index
-- waere das ein Full-Table-Scan auf der wachsenden beleg-Tabelle.
-- Idempotent ueber INFORMATION_SCHEMA.STATISTICS, gleiches Pattern wie V302.

SET @idx_uploaded_by_datum = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND INDEX_NAME = 'idx_beleg_uploaded_by_upload_datum'
);
SET @sql_idx_uploaded_by_datum = IF(@idx_uploaded_by_datum = 0,
    'CREATE INDEX idx_beleg_uploaded_by_upload_datum ON beleg(uploaded_by_id, upload_datum)',
    'SELECT 1'
);
PREPARE stmt_idx_uploaded_by_datum FROM @sql_idx_uploaded_by_datum;
EXECUTE stmt_idx_uploaded_by_datum;
DEALLOCATE PREPARE stmt_idx_uploaded_by_datum;
