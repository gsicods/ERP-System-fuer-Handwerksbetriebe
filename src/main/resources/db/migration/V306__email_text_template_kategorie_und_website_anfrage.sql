-- Fügt eine fachliche Gruppierung (Kategorie) zu den E-Mail-Textvorlagen hinzu
-- und seedet die neue Vorlage für die Bestätigungsmail an Webseiten-Leads.
--
-- Hintergrund:
--  * V218 hat die Tabelle für alle Dokument-Mails angelegt, V250 hat die
--    Mahnstufen ergänzt. Bisher gab es keinerlei Gruppierung — alle Vorlagen
--    stehen in einer flachen Liste. Mit zunehmender Zahl (jetzt + Webseiten-
--    Bestätigung) wird das unübersichtlich.
--  * Hibernate-MySQL-Dialekt mappt @Enumerated(EnumType.STRING) auf eine
--    native ENUM-Spalte (siehe BACKEND_ARCH.md), daher hier ENUM(…) statt
--    VARCHAR — sonst kracht ddl-auto=validate beim Startup.
--
-- Idempotent: Spalten- und Index-Anlage über INFORMATION_SCHEMA-Check.

-- 1) Spalte kategorie idempotent anlegen ---------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'email_text_template'
      AND COLUMN_NAME = 'kategorie'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE email_text_template ADD COLUMN kategorie ENUM(''DOKUMENT'',''MAHNWESEN'',''WEBSITE'',''SYSTEM'') NULL AFTER dokument_typ',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Bestehende Zeilen backfillen ---------------------------------------------
UPDATE email_text_template
   SET kategorie = 'DOKUMENT'
 WHERE kategorie IS NULL
   AND dokument_typ IN (
       'ANGEBOT','AUFTRAGSBESTAETIGUNG','RECHNUNG','TEILRECHNUNG',
       'ABSCHLAGSRECHNUNG','SCHLUSSRECHNUNG','GUTSCHRIFT','STORNORECHNUNG',
       'ZEICHNUNG'
   );

UPDATE email_text_template
   SET kategorie = 'MAHNWESEN'
 WHERE kategorie IS NULL
   AND dokument_typ IN (
       'ZAHLUNGSERINNERUNG','ERSTE_MAHNUNG','ZWEITE_MAHNUNG','MAHNUNG'
   );

-- Fallback für ggf. später manuell angelegte Zeilen ohne bekannten Typ:
-- die landen vorerst in SYSTEM und können im UI umgehängt werden.
UPDATE email_text_template
   SET kategorie = 'SYSTEM'
 WHERE kategorie IS NULL;

-- 3) Neue Vorlage für die Webseiten-Bestätigung seeden ------------------------
-- Wird vom AnfrageBestaetigungVersandService aufgerufen, wenn ein Lead über
-- den öffentlichen Funnel eine Anfrage abschickt. INSERT IGNORE seedet die
-- Zeile nur, wenn es noch keinen Datensatz mit dokument_typ
-- 'WEBSITE_ANFRAGE_BESTAETIGUNG' gibt — manuelle Edits durch den Betreiber
-- in der UI bleiben bei Re-Run der Migration unangetastet (Body wird hier
-- NICHT zwangsweise auf den Default zurueckgesetzt). Platzhalter:
--   {{ANREDE}}      — "Hallo Max Mustermann" (für Leads ohne Anrede-Feld)
--   {{KUNDENNAME}}  — Vor- + Nachname des Leads
--   {{BAUVORHABEN}} — Service-Typ + Projektarten (z.B. "Neubau - Wohnhaus")
--   {{NACHRICHT}}   — Freitext-Nachricht aus dem Funnel (HTML-escaped)
--   {{ANFRAGE_DATUM}}    — Anlegedatum dd.MM.yyyy
--   {{ANFRAGENUMMER}}    — interne Anfrage-ID

INSERT IGNORE INTO email_text_template
    (dokument_typ, kategorie, name, subject_template, html_body, aktiv, created_at, updated_at)
VALUES (
    'WEBSITE_ANFRAGE_BESTAETIGUNG',
    'WEBSITE',
    'Webseite — Anfragebestätigung',
    'Wir haben Ihre Anfrage erhalten — BV: {{BAUVORHABEN}}',
    CONCAT(
        '<p>{{ANREDE}},</p>',
        '<p>vielen Dank für Ihre Anfrage über unsere Webseite! Wir haben Ihre Nachricht erhalten und melden uns innerhalb der nächsten 1–2 Werktage persönlich bei Ihnen.</p>',
        '<p><strong>Ihre Angaben:</strong><br>',
        'Bauvorhaben: <span style="color:#C00000">{{BAUVORHABEN}}</span><br>',
        'Anfrage-Datum: <span style="color:#C00000">{{ANFRAGE_DATUM}}</span><br>',
        'Anfrage-Nr.: <span style="color:#C00000">{{ANFRAGENUMMER}}</span></p>',
        '<p><strong>Ihre Nachricht an uns:</strong></p>',
        '<p style="white-space:pre-wrap;color:#475569;border-left:3px solid #e5e7eb;padding:6px 12px;">{{NACHRICHT}}</p>',
        '<p>Sollten sich Details an Ihrem Projekt geändert haben, antworten Sie einfach auf diese E-Mail — wir ergänzen Ihre Anfrage dann gerne.</p>'
    ),
    1, NOW(6), NOW(6)
);
