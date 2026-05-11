-- Sachkonten fuer das Beleg-Modul.
--
-- Ein Sachkonto beschreibt das WOFUER eines Belegs (Aufwandsart, Erlosart,
-- Privatentnahme). Die bestehende BelegKategorie auf dem Beleg beschreibt
-- weiterhin das WO (Kasse/Bank/Privat/Sonstig). Beide zusammen ergeben eine
-- praktische QuickBooks-artige Zuordnung ohne vollen SKR.
--
-- Standardkonten werden geseedet (Handwerker-typisch). Buchhalter koennen
-- spaeter ueber die UI ergaenzen/anpassen.

SET @tbl_sk = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'sachkonto'
);

SET @sql_sk = IF(@tbl_sk = 0,
    'CREATE TABLE sachkonto (
        id BIGINT NOT NULL AUTO_INCREMENT,
        nummer VARCHAR(20) NULL,
        bezeichnung VARCHAR(120) NOT NULL,
        konto_typ VARCHAR(20) NOT NULL,
        beschreibung VARCHAR(500) NULL,
        aktiv BOOLEAN NOT NULL DEFAULT TRUE,
        sortierung INT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_sachkonto_bezeichnung (bezeichnung)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT 1'
);
PREPARE stmt_sk FROM @sql_sk;
EXECUTE stmt_sk;
DEALLOCATE PREPARE stmt_sk;

-- Spalte sachkonto_id im beleg ergaenzen (idempotent)

SET @col_skid = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'beleg'
      AND COLUMN_NAME  = 'sachkonto_id'
);
SET @sql_skid = IF(@col_skid = 0,
    'ALTER TABLE beleg ADD COLUMN sachkonto_id BIGINT NULL,
     ADD CONSTRAINT fk_beleg_sachkonto FOREIGN KEY (sachkonto_id) REFERENCES sachkonto(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt_skid FROM @sql_skid;
EXECUTE stmt_skid;
DEALLOCATE PREPARE stmt_skid;

SET @idx_skid = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'beleg'
      AND INDEX_NAME = 'idx_beleg_sachkonto'
);
SET @sql_idx_skid = IF(@idx_skid = 0,
    'CREATE INDEX idx_beleg_sachkonto ON beleg(sachkonto_id)',
    'SELECT 1'
);
PREPARE stmt_idx_skid FROM @sql_idx_skid;
EXECUTE stmt_idx_skid;
DEALLOCATE PREPARE stmt_idx_skid;

-- Standardkonten (Handwerker-typisch, Nummerierung an SKR03 angelehnt)
-- Idempotent ueber UNIQUE auf bezeichnung.

INSERT IGNORE INTO sachkonto (nummer, bezeichnung, konto_typ, beschreibung, aktiv, sortierung) VALUES
  -- AUFWAND
  ('3400', 'Materialeinkauf',          'AUFWAND', 'Material, Baustoffe, Rohstoffe',                       TRUE, 10),
  ('4400', 'Werkzeug & Kleingeraete',  'AUFWAND', 'Werkzeug, Kleingeraete (sofort abschreibbar)',         TRUE, 20),
  ('4530', 'Fahrzeugkosten',           'AUFWAND', 'Kraftstoff, Wartung, Reparaturen, KFZ-Versicherung',   TRUE, 30),
  ('4910', 'Telefon & Internet',       'AUFWAND', 'Mobilfunk, Festnetz, Internet',                        TRUE, 40),
  ('4930', 'Buerobedarf',              'AUFWAND', 'Papier, Toner, Stifte, Software',                      TRUE, 50),
  ('4940', 'Reinigung',                'AUFWAND', 'Putzmittel, Reinigungsdienst',                         TRUE, 60),
  ('4945', 'Verpflegung & Bewirtung',  'AUFWAND', 'Geschaeftsessen, Verpflegung Mitarbeiter',             TRUE, 70),
  ('4948', 'Reisekosten',              'AUFWAND', 'Hotel, Bahn, Spesen',                                   TRUE, 80),
  ('4380', 'Versicherungen',           'AUFWAND', 'Betriebsversicherungen (ohne KFZ)',                    TRUE, 90),
  ('4360', 'Werbung & Marketing',      'AUFWAND', 'Anzeigen, Website, Visitenkarten',                     TRUE, 100),
  ('4980', 'Sonstiger Aufwand',        'AUFWAND', 'Diverse betriebliche Aufwendungen',                    TRUE, 200),

  -- ERTRAG
  ('8400', 'Erloese 19%',              'ERTRAG', 'Steuerpflichtige Erloese 19% (Bar/Karte/Bank)',         TRUE, 300),
  ('8300', 'Erloese 7%',               'ERTRAG', 'Steuerpflichtige Erloese 7%',                           TRUE, 310),

  -- PRIVAT
  ('1800', 'Privatentnahme',           'PRIVAT', 'Bar-Entnahme durch Inhaber',                            TRUE, 400),
  ('1810', 'Privateinlage',            'PRIVAT', 'Einlage durch Inhaber',                                 TRUE, 410),

  -- NEUTRAL (keine GuV-Wirkung)
  ('1200', 'Bank-Kassen-Umbuchung',    'NEUTRAL', 'Bar abgehoben/eingezahlt; keine GuV-Wirkung',          TRUE, 500),
  ('1700', 'Durchlaufende Posten',     'NEUTRAL', 'Treuhand, Kautionen, durchlaufende Auslagen',          TRUE, 510);
