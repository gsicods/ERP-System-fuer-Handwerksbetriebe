-- Index fuer den Subject-basierten Threading-Fallback beim Live-Import
-- (EmailRepository.findRecentBefore: ORDER BY sent_at DESC, id DESC mit LIMIT).
-- Ohne diesen Index waere jeder Import eine Full-Table-Scan + Filesort auf
-- der wachsenden email-Tabelle. Wird ausserdem von diversen Listing-Endpoints
-- mitgenutzt, die nach sent_at sortieren.
-- Idempotent ueber INFORMATION_SCHEMA.STATISTICS, gleiches Pattern wie V315.

SET @idx_email_sent_at = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'email'
      AND INDEX_NAME = 'idx_email_sent_at'
);
SET @sql_idx_email_sent_at = IF(@idx_email_sent_at = 0,
    'CREATE INDEX idx_email_sent_at ON email(sent_at)',
    'SELECT 1'
);
PREPARE stmt_idx_email_sent_at FROM @sql_idx_email_sent_at;
EXECUTE stmt_idx_email_sent_at;
DEALLOCATE PREPARE stmt_idx_email_sent_at;
