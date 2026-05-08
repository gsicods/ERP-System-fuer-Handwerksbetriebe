-- Stundenlohn-Verlauf je Mitarbeiter (1:N).
--
-- Bisher gab es nur ein Feld mitarbeiter.stundenlohn fuer den "aktuellen" Wert.
-- Das reicht nicht, weil:
--   * Stundenloehne sich aendern (Tariferhoehung, Befoerderung, GF-Bezuege).
--   * Vergangene Zeitbuchungen mit dem damals gueltigen Lohn bewertet werden
--     muessen (rueckwirkende Korrektheit).
--
-- Strategie: Pro Mitarbeiter beliebig viele Eintraege mit gueltig_ab. Der zum
-- Stichtag passende Eintrag ist der juengste mit gueltig_ab <= stichtag.
-- mitarbeiter.stundenlohn wird im Service stets auf den aktuell gueltigen
-- Eintrag gespiegelt (= bestehender Code aendert sich nicht).

CREATE TABLE IF NOT EXISTS mitarbeiter_stundenlohn (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    mitarbeiter_id  BIGINT        NOT NULL,
    stundenlohn     DECIMAL(10,2) NOT NULL,
    gueltig_ab      DATE          NOT NULL,
    bemerkung       VARCHAR(500)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stundenlohn_mitarbeiter
        FOREIGN KEY (mitarbeiter_id) REFERENCES mitarbeiter (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_stundenlohn_mitarbeiter_datum UNIQUE (mitarbeiter_id, gueltig_ab),
    INDEX idx_stundenlohn_mitarbeiter_datum (mitarbeiter_id, gueltig_ab)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bestehende Stundenloehne in den Verlauf uebernehmen, damit alte Zeitbuchungen
-- weiterhin korrekt bewertbar sind. gueltig_ab = Eintrittsdatum, sonst Fallback.
-- Mitarbeiter ohne Eintrittsdatum bekommen einen fiktiven Stichtag mit
-- entsprechender Bemerkung - der User kann diesen im UI korrigieren.
INSERT INTO mitarbeiter_stundenlohn (mitarbeiter_id, stundenlohn, gueltig_ab, bemerkung)
SELECT
    m.id,
    m.stundenlohn,
    COALESCE(m.eintrittsdatum, '2020-01-01'),
    CASE
        WHEN m.eintrittsdatum IS NULL
            THEN 'Initialer Eintrag aus Migration V299 - kein Eintrittsdatum hinterlegt, fiktives Startdatum 2020-01-01 (bitte pruefen)'
        ELSE 'Initialer Eintrag aus Migration V299 (Bestandsdaten)'
    END
FROM mitarbeiter m
WHERE m.stundenlohn IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM mitarbeiter_stundenlohn h WHERE h.mitarbeiter_id = m.id
  );
