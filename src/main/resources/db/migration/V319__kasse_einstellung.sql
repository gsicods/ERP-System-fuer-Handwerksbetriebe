-- Konfiguration des Kassenbuchs (Singleton-Tabelle, max. 1 Zeile).
--
-- Steuert zwei verbundene Mechanismen:
--   1. Kasse-darf-nicht-ins-Minus: BelegService validiert beim Speichern, dass
--      der Bar-Saldo nach der Buchung >= mindestbestand bleibt. Andernfalls
--      muss der User vorher eine Bank-Abhebung oder Privateinlage buchen.
--   2. Ehegattengehalt-Automatik: Monatlicher Scheduler bucht am Stichtag den
--      konfigurierten Betrag als Kassenausgabe (Sachkonto = Lohn-Aufwand). Wenn
--      die Kasse danach unter mindestbestand fiele, wird vor dem Lohn eine
--      Privateinlage in genau der noetigen Hoehe gebucht — Ziel: "am Monatsende
--      so wenig Geld in der Kasse wie noetig".
--
-- letzte_buchung_jahrmonat dient als Idempotenz-Sperre: pro YYYY-MM nur einmal
-- buchen, auch wenn der Scheduler mehrfach laeuft oder der Server neu startet.

SET @tbl_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'kasse_einstellung'
);
SET @sql := IF(@tbl_exists = 0,
    'CREATE TABLE kasse_einstellung (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        mindestbestand DECIMAL(10,2) NOT NULL DEFAULT 0.00,
        ehegattengehalt_aktiv BOOLEAN NOT NULL DEFAULT FALSE,
        ehegattengehalt_betrag DECIMAL(10,2) NULL,
        ehegattengehalt_tag INT NULL,
        ehegattengehalt_sachkonto_id BIGINT NULL,
        ehegattengehalt_kostenstelle_id BIGINT NULL,
        ehegattengehalt_empfaenger_name VARCHAR(120) NULL,
        privateinlage_sachkonto_id BIGINT NULL,
        letzte_buchung_jahrmonat VARCHAR(7) NULL,
        aktualisiert_am DATETIME NULL,
        CONSTRAINT fk_kasse_sachkonto FOREIGN KEY (ehegattengehalt_sachkonto_id) REFERENCES sachkonto(id),
        CONSTRAINT fk_kasse_kostenstelle FOREIGN KEY (ehegattengehalt_kostenstelle_id) REFERENCES firma_kostenstelle(id),
        CONSTRAINT fk_kasse_privateinlage_konto FOREIGN KEY (privateinlage_sachkonto_id) REFERENCES sachkonto(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Singleton-Default-Zeile anlegen (idempotent: nur wenn Tabelle leer)
SET @row_count := (SELECT COUNT(*) FROM kasse_einstellung);
SET @sql := IF(@row_count = 0,
    'INSERT INTO kasse_einstellung (mindestbestand, ehegattengehalt_aktiv, aktualisiert_am) VALUES (0.00, FALSE, NOW())',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
