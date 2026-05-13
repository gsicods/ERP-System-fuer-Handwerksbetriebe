-- Kostenstellen-Splits am Kassen-Beleg.
--
-- Bisher kannte der Beleg genau EINE Kostenstelle (beleg.kostenstelle_id) — das
-- reicht fuer die einfache Tankquittung. Sobald aber ein Bar-Einkauf auf mehrere
-- Kostenstellen verteilt werden soll (z.B. 60% Werkstatt, 40% Gemeinkosten)
-- oder eine periodische Ausgabe ueber mehrere Jahre gestreckt werden soll
-- (z.B. Zertifizierung alle 4 Jahre), brauchen wir N:1 Splits.
--
-- Spiegelt das Pattern von lieferant_dokument_projekt_anteil, aber fokussiert
-- auf Belege + Kostenstellen — Projekte sind hier bewusst nicht enthalten,
-- weil Bar-Belege im Handwerker-Alltag fast nie projektbezogen sind.
--
-- Idempotent: jede Aenderung pruefbar, Mehrfach-Ausfuehrung unschaedlich.

SET @tbl_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg_kostenstellen_anteil'
);
SET @sql := IF(@tbl_exists = 0,
    'CREATE TABLE beleg_kostenstellen_anteil (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        beleg_id BIGINT NOT NULL,
        kostenstelle_id BIGINT NOT NULL,
        prozent INT NULL,
        absoluter_betrag DECIMAL(15,2) NULL,
        berechneter_betrag DECIMAL(15,2) NULL,
        beschreibung VARCHAR(255) NULL,
        zugeordnet_am DATETIME NULL,
        zugeordnet_von_user_id BIGINT NULL,
        streckung_jahre INT NOT NULL DEFAULT 1,
        streckung_start_jahr INT NULL,
        CONSTRAINT fk_bka_beleg FOREIGN KEY (beleg_id) REFERENCES beleg(id) ON DELETE CASCADE,
        CONSTRAINT fk_bka_kostenstelle FOREIGN KEY (kostenstelle_id) REFERENCES firma_kostenstelle(id),
        CONSTRAINT fk_bka_user FOREIGN KEY (zugeordnet_von_user_id) REFERENCES frontend_user_profile(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index fuer schnelles Laden aller Anteile pro Beleg (Editor zeigt sie an).
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg_kostenstellen_anteil'
      AND index_name = 'idx_bka_beleg'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_bka_beleg ON beleg_kostenstellen_anteil(beleg_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Index fuer die Auswertung "alle Belege auf Kostenstelle X im Jahr Y"
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'beleg_kostenstellen_anteil'
      AND index_name = 'idx_bka_kostenstelle_jahr'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_bka_kostenstelle_jahr ON beleg_kostenstellen_anteil(kostenstelle_id, streckung_start_jahr)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
