CREATE TABLE IF NOT EXISTS zahlung (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    richtung VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ERFASST',
    zahlungsdatum DATE NOT NULL,
    betrag DECIMAL(15,2) NOT NULL DEFAULT 0,
    zahlungsart VARCHAR(80),
    verwendungszweck VARCHAR(500),
    ausgangs_dokument_id BIGINT,
    beleg_id BIGINT,
    erfasst_am DATETIME(6) NOT NULL,
    CONSTRAINT fk_zahlung_ausgangs_dokument FOREIGN KEY (ausgangs_dokument_id) REFERENCES ausgangs_geschaeftsdokument(id),
    CONSTRAINT fk_zahlung_beleg FOREIGN KEY (beleg_id) REFERENCES beleg(id)
);

CREATE INDEX IF NOT EXISTS idx_zahlung_richtung_datum ON zahlung(richtung, zahlungsdatum);
CREATE INDEX IF NOT EXISTS idx_zahlung_ausgangs_dokument ON zahlung(ausgangs_dokument_id);
CREATE INDEX IF NOT EXISTS idx_zahlung_beleg ON zahlung(beleg_id);
