-- Handwerks-Gewerk + zugeordnete Berufsgenossenschaft (BG) und deren
-- Beitragssatz fuer die gesetzliche Unfallversicherung.
--
-- Die BG-Beitraege sind in Deutschland nach Gefahrtarifstellen aufgeschluesselt
-- (sehr granular, pro Taetigkeit). Die hier gepflegten Werte sind realistische
-- Durchschnittswerte je Gewerk und werden im Frontend angepasst, wenn der
-- konkrete Beitragsbescheid der eigenen BG vorliegt. Im Firmen-Editor laesst
-- sich der Wert pro Firma noch ueberschreiben (siehe V298).

CREATE TABLE IF NOT EXISTS gewerk (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(255) NOT NULL,
    bg_name           VARCHAR(255) NOT NULL,
    bg_satz_prozent   DECIMAL(5,2) NOT NULL,
    aktiv             BOOLEAN      NOT NULL DEFAULT TRUE,
    bemerkung         VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_gewerk_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO gewerk (name, bg_name, bg_satz_prozent, aktiv, bemerkung) VALUES
    ('Bauhauptgewerbe (Hochbau, Maurer, Beton)', 'BG BAU',  5.46, TRUE, 'Richtwert - genauer Beitrag kommt aus dem Beitragsbescheid.'),
    ('Ausbau (Trockenbau, Putz, Stuck)',         'BG BAU',  3.30, TRUE, NULL),
    ('Dachdecker',                               'BG BAU',  7.70, TRUE, NULL),
    ('Maler und Lackierer',                      'BG BAU',  3.30, TRUE, NULL),
    ('Geruestbau',                               'BG BAU',  6.80, TRUE, NULL),
    ('Fliesen-, Platten- und Mosaikleger',       'BG BAU',  5.50, TRUE, NULL),
    ('Tischler / Schreiner',                     'BGHM',    1.13, TRUE, NULL),
    ('Metallbau / Schlosserei',                  'BGHM',    1.85, TRUE, NULL),
    ('Kfz-Werkstatt',                            'BGHM',    1.85, TRUE, NULL),
    ('Elektroinstallation',                      'BG ETEM', 1.10, TRUE, NULL),
    ('Sanitaer / Heizung / Klima (SHK)',         'BG ETEM', 2.10, TRUE, NULL),
    ('Garten- und Landschaftsbau',               'SVLFG',   3.40, TRUE, NULL),
    ('Gebaeudereinigung',                        'BG BAU',  5.46, TRUE, NULL),
    ('Andere / Sonstige',                        'Individuell', 3.00, TRUE, 'Platzhalter - bitte BG und Satz manuell eintragen.')
ON DUPLICATE KEY UPDATE bg_name = VALUES(bg_name);
