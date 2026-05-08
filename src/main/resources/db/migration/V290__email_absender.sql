-- Konfigurierbare Liste der Absender-E-Mail-Adressen.
-- Jeder FrontendUserProfile kann einer dieser Adressen zugewiesen werden.
-- Beim Versand einer E-Mail wird die dem eingeloggten Benutzer zugeordnete
-- Adresse als "From"-Header verwendet (statt der frueher hardgecodeten
-- bauschlosserei-kuhn@t-online.de).

SET @tbl_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'email_absender'
);

SET @create_tbl = IF(@tbl_exists = 0,
    'CREATE TABLE email_absender (
        id BIGINT NOT NULL AUTO_INCREMENT,
        email_adresse VARCHAR(255) NOT NULL,
        anzeigename VARCHAR(255) NULL,
        aktiv BOOLEAN NOT NULL DEFAULT TRUE,
        sortierung INT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        CONSTRAINT uk_email_absender_adresse UNIQUE (email_adresse)
    ) ENGINE=InnoDB',
    'SELECT 1'
);
PREPARE stmt FROM @create_tbl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Standard-Absender anlegen, falls noch keiner existiert. Damit bestehende
-- Installationen nach dem Update direkt eine funktionierende Default-Adresse
-- haben (entspricht dem bisherigen Hardcode-Wert).
INSERT INTO email_absender (email_adresse, anzeigename, aktiv, sortierung)
SELECT 'bauschlosserei-kuhn@t-online.de', 'Bauschlosserei Kuhn', TRUE, 0
WHERE NOT EXISTS (SELECT 1 FROM email_absender);

-- FK-Spalte am FrontendUserProfile.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'frontend_user_profile'
      AND COLUMN_NAME  = 'email_absender_id'
);

SET @add_col = IF(@col_exists = 0,
    'ALTER TABLE frontend_user_profile ADD COLUMN email_absender_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt2 FROM @add_col;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA   = DATABASE()
      AND TABLE_NAME     = 'frontend_user_profile'
      AND CONSTRAINT_NAME = 'fk_frontend_user_email_absender'
);

SET @add_fk = IF(@fk_exists = 0,
    'ALTER TABLE frontend_user_profile
        ADD CONSTRAINT fk_frontend_user_email_absender
        FOREIGN KEY (email_absender_id) REFERENCES email_absender (id)
        ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt3 FROM @add_fk;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Bestehende Profile, die noch keinen Absender zugewiesen haben, bekommen
-- den ersten aktiven Absender als Default - so funktioniert der Versand
-- direkt nach dem Update fuer alle User weiter.
UPDATE frontend_user_profile fup
SET fup.email_absender_id = (
    SELECT ea.id FROM email_absender ea
    WHERE ea.aktiv = TRUE
    ORDER BY ea.sortierung ASC, ea.id ASC
    LIMIT 1
)
WHERE fup.email_absender_id IS NULL;
