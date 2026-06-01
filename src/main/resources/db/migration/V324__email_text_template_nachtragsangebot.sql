-- Seed-Vorlage für das Nachtragsangebot in der E-Mail-Textvorlagen-Verwaltung.
--
-- Hintergrund:
--   V218 hat die Tabelle und Vorlagen für Rechnung, Teilrechnung, Schlussrechnung,
--   Abschlagsrechnung, Mahnung, Angebot, Auftragsbestätigung und Zeichnung angelegt.
--   NACHTRAGSANGEBOT existierte damals noch nicht und fehlt daher als Standard-
--   vorlage in der "E-MAIL-TEXTVORLAGEN"-Verwaltung (Kommunikation-Ribbon).
--   V306 backfillte die Kategorie-Spalte, listete NACHTRAGSANGEBOT jedoch nicht
--   auf — daher hier ergänzend ein Backfill für manuell angelegte Zeilen.
--
-- Idempotent: INSERT IGNORE überspringt eine bereits vorhandene Zeile (Unique-Key
-- uk_email_text_template_doktyp auf dokument_typ) — manuelle Edits des Betreibers
-- bleiben bei einem erneuten Migrations-Lauf unberührt.

-- 1) Kategorie für etwaige manuell angelegte NACHTRAGSANGEBOT-Zeilen setzen.
UPDATE email_text_template
   SET kategorie = 'DOKUMENT'
 WHERE dokument_typ = 'NACHTRAGSANGEBOT'
   AND kategorie IS NULL;

-- 2) Standard-Vorlage für Nachtragsangebote seeden.
INSERT IGNORE INTO email_text_template
    (dokument_typ, kategorie, name, subject_template, html_body, aktiv, created_at, updated_at)
VALUES (
    'NACHTRAGSANGEBOT',
    'DOKUMENT',
    'Nachtragsangebot',
    'Nachtragsangebot: (BV: {{BAUVORHABEN}}) Angebotsnummer: {{DOKUMENTNUMMER}}',
    CONCAT(
        '<p>{{ANREDE}} {{KUNDENNAME}},</p>',
        '<p>im Anhang finden Sie unser Nachtragsangebot zu dem laufenden Projekt.<br>',
        'Bei Rückfragen können Sie sich gerne telefonisch oder per E-Mail bei uns melden.</p>',
        '<p><strong>Bauvorhaben:</strong> <span style="color:#C00000">{{BAUVORHABEN}}</span><br>',
        '<strong>Angebotsnummer:</strong> <span style="color:#C00000">{{DOKUMENTNUMMER}}</span></p>'
    ),
    1, NOW(6), NOW(6)
);
