-- Schema-Validation-Fix fuer Beleg- und Sachkonto-Modul.
--
-- V302 und V303 haben die mit @Enumerated(EnumType.STRING) gemappten Spalten
-- als VARCHAR angelegt. Hibernate 6.x mit MySQL-Dialekt erwartet hier aber
-- native ENUM-Spalten (analog kunde.anrede, vgl. BACKEND_ARCH.md + V291).
-- Beim Startup schlaegt sonst ddl-auto=validate fehl mit
--   "wrong column type ... found [varchar], but expecting [enum (...)]".
--
-- Werte exakt wie die Java-Enum-Konstanten (UPPERCASE). MODIFY COLUMN ist
-- idempotent: wiederholtes Ausfuehren liefert denselben Spaltentyp.

ALTER TABLE beleg
    MODIFY COLUMN beleg_kategorie
        ENUM('UNZUGEORDNET','KASSE_EINNAHME','KASSE_AUSGABE','PRIVATENTNAHME','BANK','KREDITKARTE','SONSTIGER_BELEG')
        NOT NULL DEFAULT 'UNZUGEORDNET';

ALTER TABLE beleg
    MODIFY COLUMN status
        ENUM('NEU','VALIDIERT','VERWORFEN')
        NOT NULL DEFAULT 'NEU';

ALTER TABLE beleg
    MODIFY COLUMN ki_analyse_status
        ENUM('PENDING','LAEUFT','DONE','FAILED')
        NOT NULL DEFAULT 'PENDING';

ALTER TABLE sachkonto
    MODIFY COLUMN konto_typ
        ENUM('AUFWAND','ERTRAG','PRIVAT','NEUTRAL')
        NOT NULL;
