-- Sozialversicherungs-Beitragssaetze (Bundeseinheitlich).
--
-- Die Saetze werden zentral hier gepflegt und im Frontend bearbeitet, wenn
-- der Gesetzgeber sie aendert (typisch zum Jahreswechsel). Die "Verteilung"
-- (AG-/AN-Anteil) wird im Berechnungs-Code festgelegt:
--   * KV/PV/RV/AV: Grundsatz halbe/halbe (Sonderfaelle: Sachsen, Kinderlose)
--   * Minijob-/Umlagen-Saetze: AG-Pauschale, AN traegt 0
--   * PV-Kinderlos-Zuschlag: nur AN
--
-- gueltig_ab erlaubt es, kuenftige Saetze schon einzupflegen, ohne den
-- aktuellen Wert zu ueberschreiben - das Tool waehlt zum Stichtag den
-- juengsten Eintrag <= heute.

CREATE TABLE IF NOT EXISTS sv_satz (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    satz_typ        ENUM(
        'KV_GESAMT',
        'PV_GESAMT',
        'PV_KINDERLOS_AN_ZUSCHLAG',
        'RV_GESAMT',
        'AV_GESAMT',
        'MINIJOB_AG_KV',
        'MINIJOB_AG_RV',
        'MINIJOB_AG_PAUSCHALSTEUER',
        'U1_UMLAGE',
        'U2_UMLAGE',
        'INSOLVENZGELDUMLAGE'
    )                            NOT NULL,
    prozent         DECIMAL(5,2) NOT NULL,
    gueltig_ab      DATE         NOT NULL,
    beschreibung    VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sv_satz_typ_ab UNIQUE (satz_typ, gueltig_ab),
    INDEX idx_sv_satz_typ (satz_typ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sv_satz (satz_typ, prozent, gueltig_ab, beschreibung) VALUES
    ('KV_GESAMT',                 14.60, '2026-01-01', 'Allgemeiner Beitragssatz Krankenversicherung (mit Krankengeldanspruch).'),
    ('PV_GESAMT',                  3.40, '2026-01-01', 'Pflegeversicherung. Wird i. d. R. halbe/halbe getragen (Sachsen-Sonderregel ausgenommen).'),
    ('PV_KINDERLOS_AN_ZUSCHLAG',   0.60, '2026-01-01', 'Zuschlag fuer kinderlose Arbeitnehmer ab 23 Jahren - traegt allein der Arbeitnehmer.'),
    ('RV_GESAMT',                 18.60, '2026-01-01', 'Rentenversicherung. Halbe/halbe.'),
    ('AV_GESAMT',                  2.60, '2026-01-01', 'Arbeitslosenversicherung. Halbe/halbe.'),
    ('MINIJOB_AG_KV',             13.00, '2026-01-01', 'Minijob-Pauschale Krankenversicherung (Arbeitgeber, gewerblich).'),
    ('MINIJOB_AG_RV',             15.00, '2026-01-01', 'Minijob-Pauschale Rentenversicherung (Arbeitgeber, gewerblich).'),
    ('MINIJOB_AG_PAUSCHALSTEUER',  2.00, '2026-01-01', 'Pauschalsteuer Minijob (Arbeitgeber, optional - alternativ individuelle Lohnsteuer).'),
    ('U1_UMLAGE',                  1.10, '2026-01-01', 'Umlage U1 (Lohnfortzahlung im Krankheitsfall) - kassenindividuell, hier Default-Wert.'),
    ('U2_UMLAGE',                  0.24, '2026-01-01', 'Umlage U2 (Mutterschaft) - kassenindividuell, hier Default-Wert.'),
    ('INSOLVENZGELDUMLAGE',        0.06, '2026-01-01', 'Insolvenzgeldumlage - traegt allein der Arbeitgeber.')
ON DUPLICATE KEY UPDATE prozent = VALUES(prozent);
