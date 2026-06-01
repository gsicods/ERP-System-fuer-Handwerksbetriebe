-- Nachtragsangebot: neuer Dokumenttyp-Wert NACHTRAGSANGEBOT
--
-- Hintergrund:
--   @Enumerated(EnumType.STRING) wird in diesem Projekt global auf VARCHAR
--   gemappt (spring.jpa.properties.hibernate.type.preferred_enum_jdbc_type=VARCHAR).
--   Die betroffenen Spalten sind also VARCHAR und brauchen KEINEN Typ-Wechsel –
--   Hibernate `validate` prüft nur den Spaltentyp, nicht die zulässigen Werte,
--   und NACHTRAGSANGEBOT (16 Zeichen) passt in die vorhandenen Längen (30/40).
--
--   ABER: Tabellen, die ursprünglich von Hibernate (ddl-auto=update) angelegt
--   wurden, können eine wertbeschränkende CHECK-Constraint
--   `... CHECK (spalte IN ('ANGEBOT','AUFTRAGSBESTAETIGUNG', ...))` tragen.
--   Diese würde ein INSERT mit dem neuen Wert NACHTRAGSANGEBOT zur Laufzeit
--   ablehnen (MySQL >= 8.0.16 / MariaDB >= 10.2.1 erzwingen CHECKs).
--
--   Diese Migration entfernt solche Enum-CHECK-Constraints idempotent von den
--   Spalten, die den neuen Wert zur Laufzeit erhalten. Die Gültigkeit der Werte
--   sichert weiterhin das Java-Enum + die Service-Schicht ab (kein Datenverlust,
--   die Spalte bleibt VARCHAR). Existiert keine solche Constraint, ist der Block
--   ein No-Op. Idempotent über INFORMATION_SCHEMA.CHECK_CONSTRAINTS.
--
-- Engine-Kompatibilität: Das Projekt läuft auf MariaDB (Docker, 11.4) ODER MySQL 8.
--   MariaDB erwartet `ALTER TABLE ... DROP CONSTRAINT <name>`, MySQL `DROP CHECK <name>`.
--   Wir wählen das Schlüsselwort anhand von VERSION() dynamisch.

SET @dropkw := IF(VERSION() LIKE '%MariaDB%', 'DROP CONSTRAINT', 'DROP CHECK');

-- 1) ausgangs_geschaeftsdokument.typ (AusgangsGeschaeftsDokumentTyp)
SET @drops_agd := (
    SELECT GROUP_CONCAT(CONCAT(@dropkw, ' `', tc.CONSTRAINT_NAME, '`') SEPARATOR ', ')
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
     AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'ausgangs_geschaeftsdokument'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%AUFTRAGSBESTAETIGUNG%'
);
SET @sql_agd := IF(@drops_agd IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE `ausgangs_geschaeftsdokument` ', @drops_agd));
PREPARE stmt_agd FROM @sql_agd;
EXECUTE stmt_agd;
DEALLOCATE PREPARE stmt_agd;

-- 2) formular_template_assignment.dokumenttyp_enum (Dokumenttyp)
SET @drops_fta := (
    SELECT GROUP_CONCAT(CONCAT(@dropkw, ' `', tc.CONSTRAINT_NAME, '`') SEPARATOR ', ')
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
     AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'formular_template_assignment'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%AUFTRAGSBESTAETIGUNG%'
);
SET @sql_fta := IF(@drops_fta IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE `formular_template_assignment` ', @drops_fta));
PREPARE stmt_fta FROM @sql_fta;
EXECUTE stmt_fta;
DEALLOCATE PREPARE stmt_fta;

-- 3) textbaustein_dokumenttyp_enum.dokumenttyp (Dokumenttyp, ElementCollection)
SET @drops_tbe := (
    SELECT GROUP_CONCAT(CONCAT(@dropkw, ' `', tc.CONSTRAINT_NAME, '`') SEPARATOR ', ')
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
     AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'textbaustein_dokumenttyp_enum'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
      AND cc.CHECK_CLAUSE LIKE '%AUFTRAGSBESTAETIGUNG%'
);
SET @sql_tbe := IF(@drops_tbe IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE `textbaustein_dokumenttyp_enum` ', @drops_tbe));
PREPARE stmt_tbe FROM @sql_tbe;
EXECUTE stmt_tbe;
DEALLOCATE PREPARE stmt_tbe;
