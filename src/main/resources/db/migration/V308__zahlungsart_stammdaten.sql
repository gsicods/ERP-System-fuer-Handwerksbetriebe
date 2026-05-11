-- Stammdaten-Tabelle fuer Zahlungsarten (Bar, EC, Ueberweisung, ...).
--
-- Hintergrund: Bisher war `beleg.zahlungsart` ein freier Textfeld. Der Buch-
-- halter musste bei jedem Beleg manuell "Bar" oder "EC" eintippen — fehler-
-- anfaellig (Tippfehler) und nicht auswertbar. Mit einer kleinen Stammdaten-
-- Tabelle bekommt die UI eine Auswahl-Liste, der String wird weiterhin
-- gespeichert (kein FK auf `beleg`), damit bestehende Belege unveraendert
-- bleiben.
--
-- Idempotent: Tabellen- und Index-Anlage ueber INFORMATION_SCHEMA. Seed via
-- INSERT IGNORE auf UNIQUE(bezeichnung).

SET @tbl_za = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'zahlungsart'
);

SET @sql_za = IF(@tbl_za = 0,
    'CREATE TABLE zahlungsart (
        id BIGINT NOT NULL AUTO_INCREMENT,
        bezeichnung VARCHAR(60) NOT NULL,
        aktiv BOOLEAN NOT NULL DEFAULT TRUE,
        sortierung INT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_zahlungsart_bezeichnung (bezeichnung)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT 1'
);
PREPARE stmt_za FROM @sql_za;
EXECUTE stmt_za;
DEALLOCATE PREPARE stmt_za;

-- Standard-Zahlungsarten (Handwerker-typisch). Bezeichnung wird 1:1 in
-- `beleg.zahlungsart` geschrieben.

-- Tabelle ist utf8mb4 (siehe CREATE TABLE) — Echte Umlaute sind erlaubt und
-- werden im UI als Auswahl-Label angezeigt; Handwerker-Sprache statt ae/oe/ue.

INSERT IGNORE INTO zahlungsart (bezeichnung, aktiv, sortierung) VALUES
  ('Bar',         TRUE, 10),
  ('EC-Karte',    TRUE, 20),
  ('Überweisung', TRUE, 30),
  ('Lastschrift', TRUE, 40),
  ('Kreditkarte', TRUE, 50),
  ('PayPal',      TRUE, 60),
  ('Scheck',      TRUE, 70),
  ('Rechnung',    TRUE, 80);
