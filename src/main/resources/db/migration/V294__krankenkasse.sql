-- Krankenkassen-Stammdaten fuer Mitarbeiter-Zuordnung und Lohnberechnung.
--
-- Der allgemeine KV-Beitragssatz (14,6 %) ist gesetzlich einheitlich und wird
-- in sv_satz gepflegt. Hier wird nur der kassenindividuelle Zusatzbeitrag
-- gefuehrt, der ueber alle Kassen hinweg variiert (i. d. R. 1,5 % - 3,5 %).
--
-- Die Werte im Seed sind Stand Mai 2026 (Quelle: GKV-Spitzenverband). Sie
-- koennen im Frontend jederzeit aktualisiert werden, wenn eine Kasse ihren
-- Zusatzbeitrag aendert.

CREATE TABLE IF NOT EXISTS krankenkasse (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    name                     VARCHAR(255) NOT NULL,
    kuerzel                  VARCHAR(32)  NULL,
    zusatzbeitrag_prozent    DECIMAL(5,2) NOT NULL,
    aktiv                    BOOLEAN      NOT NULL DEFAULT TRUE,
    gueltig_ab               DATE         NULL,
    bemerkung                VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_krankenkasse_name UNIQUE (name),
    INDEX idx_krankenkasse_aktiv (aktiv)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO krankenkasse (name, kuerzel, zusatzbeitrag_prozent, aktiv, gueltig_ab) VALUES
    ('Techniker Krankenkasse',  'TK',           2.45, TRUE, '2026-01-01'),
    ('AOK Bayern',              'AOK-BY',       2.70, TRUE, '2026-01-01'),
    ('AOK NordWest',            'AOK-NW',       2.70, TRUE, '2026-01-01'),
    ('AOK Baden-Wuerttemberg',  'AOK-BW',       2.70, TRUE, '2026-01-01'),
    ('Barmer',                  'BARMER',       3.49, TRUE, '2026-01-01'),
    ('DAK-Gesundheit',          'DAK',          2.70, TRUE, '2026-01-01'),
    ('IKK classic',             'IKK',          2.70, TRUE, '2026-01-01'),
    ('Knappschaft',             'KBS',          2.70, TRUE, '2026-01-01'),
    ('BKK VBU',                 'BKK-VBU',      2.40, TRUE, '2026-01-01'),
    ('HEK - Hanseatische Krankenkasse', 'HEK',  2.70, TRUE, '2026-01-01'),
    ('hkk Krankenkasse',        'HKK',          1.84, TRUE, '2026-01-01'),
    ('mhplus Krankenkasse',     'MHPLUS',       2.40, TRUE, '2026-01-01'),
    ('BIG direkt gesund',       'BIG',          1.99, TRUE, '2026-01-01')
ON DUPLICATE KEY UPDATE name = VALUES(name);
