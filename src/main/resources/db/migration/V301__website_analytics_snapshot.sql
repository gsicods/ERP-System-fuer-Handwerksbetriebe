-- Tagesschluss-Snapshot der Website-Analytics (bauschlosserei-kuhn.de).
--
-- Hintergrund:
-- Die Marketing-Website pusht jede Nacht (~02:00) einen Snapshot der KPIs an
-- den Endpoint /api/internal/analytics-snapshot. Spezifikation:
-- docs/erp/ANALYTICS_SNAPSHOT_API.md (vom Website-Team).
--
-- snapshotDate ist eindeutig pro Tag - ein erneuter Push fuer denselben Tag
-- ueberschreibt den bestehenden Datensatz (Upsert). Listen (funnel, topPages,
-- devices, browsers, cities) werden als JSON-Strings gespeichert, damit
-- additive Schema-Erweiterungen vom Website-Team keine Migration verlangen.
-- raw_payload enthaelt zusaetzlich das vollstaendige eingehende JSON, damit
-- spaetere Auswertungen unbekannte Felder nachreichen koennen.
--
-- Idempotent: Tabelle wird nur angelegt, wenn sie fehlt.

CREATE TABLE IF NOT EXISTS website_analytics_snapshot (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_date        DATE NOT NULL,
    schema_version       INT NOT NULL,
    generated_at         DATETIME(6) NOT NULL,
    received_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    totals_visitors      BIGINT NOT NULL DEFAULT 0,
    totals_pageviews     BIGINT NOT NULL DEFAULT 0,
    totals_leads_phone   BIGINT NOT NULL DEFAULT 0,
    totals_leads_mail    BIGINT NOT NULL DEFAULT 0,
    totals_submissions   BIGINT NOT NULL DEFAULT 0,

    visitors_today       BIGINT NOT NULL DEFAULT 0,
    visitors_yesterday   BIGINT NOT NULL DEFAULT 0,
    conversion           INT NOT NULL DEFAULT 0,

    funnel_json          LONGTEXT NULL,
    top_pages_json       LONGTEXT NULL,
    devices_json         LONGTEXT NULL,
    browsers_json        LONGTEXT NULL,
    cities_json          LONGTEXT NULL,

    raw_payload          LONGTEXT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_website_analytics_snapshot_date (snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
