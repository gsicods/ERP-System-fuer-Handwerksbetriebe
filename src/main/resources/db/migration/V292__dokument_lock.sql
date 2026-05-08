-- Soft-Lock fuer Geschaeftsdokumente im Dokumenten-Editor.
-- Verhindert, dass zwei Benutzer gleichzeitig dasselbe Dokument bearbeiten.
--
-- Strategie: Heartbeat-Lock.
--   * Beim Oeffnen wird ein Eintrag mit acquired_at + last_heartbeat_at = NOW() angelegt.
--   * Frontend pingt alle 30s einen Refresh-Endpoint, der last_heartbeat_at aktualisiert.
--   * Beim Schliessen des Tabs wird der Eintrag aktiv geloescht.
--   * Faellt das Schliessen aus (Browser-Crash, Netzabbruch), darf ein anderer
--     Benutzer das Lock uebernehmen, sobald last_heartbeat_at > 90s alt ist.
--
-- (dokument_typ, dokument_id) ist der zusammengesetzte Schluessel, weil IDs zwischen
-- AUSGANG (ausgangs_geschaeftsdokument) und EINGANG (lieferant_dokument) ueberlappen.

CREATE TABLE IF NOT EXISTS dokument_lock (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    dokument_typ       VARCHAR(16)  NOT NULL,
    dokument_id        BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    user_display_name  VARCHAR(255) NOT NULL,
    acquired_at        DATETIME     NOT NULL,
    last_heartbeat_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dokument_lock_target UNIQUE (dokument_typ, dokument_id),
    INDEX idx_dokument_lock_heartbeat (last_heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
