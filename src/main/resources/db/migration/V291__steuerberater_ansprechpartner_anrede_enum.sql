-- Hibernate 6.x mit MySQL-Dialekt erwartet bei @Enumerated(EnumType.STRING)
-- eine native ENUM-Spalte (analog zu kunde.anrede). V289 hatte die Spalte
-- als VARCHAR(32) angelegt, was zu einem Schema-Validation-Fehler beim
-- Startup führt. Hier wird der Spaltentyp auf ENUM angeglichen.

ALTER TABLE steuerberater_ansprechpartner
    MODIFY COLUMN anrede ENUM('HERR','FRAU','FAMILIE','FIRMA','DAMEN_HERREN') NULL;
