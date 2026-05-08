-- Erstkontakt-Tracking fuer den Spam-Filter (Issue #56).
--
-- Eine Domain, die wir noch nie als Absender gesehen haben, ist in
-- Kombination mit Free-Mailer + mehreren Links ein typisches
-- Cold-Mail-/SEO-Spam-Signal. Wir merken uns den Erstkontakt-Zeitpunkt,
-- damit der Filter eingehende Mails entsprechend bewerten kann.
--
-- Die Tabelle wird fuer JEDE eingehende Domain genau einmal beschrieben
-- (INSERT ... ON DUPLICATE KEY). Sie traegt keine personenbezogenen Daten
-- ueber den Domain-Teil hinaus, der bei einer geschaeftlichen Mail-Domain
-- ohnehin kaum schutzwuerdig ist; bei Free-Mailer-Domains (gmail.com etc.)
-- ist der Eintrag nicht-personenbezogen.
--
-- Index auf first_seen ermoeglicht spaeteres "Domains seit X Tagen"-Reporting,
-- ohne dass wir eine zweite Tabelle brauchen.

CREATE TABLE IF NOT EXISTS seen_sender_domain (
    domain      VARCHAR(255) NOT NULL,
    first_seen  DATETIME     NOT NULL,
    email_count INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (domain),
    INDEX idx_seen_sender_domain_first_seen (first_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill aus existierenden Mails: Beim allerersten Deployment ist die
-- Tabelle leer, was dazu fuehren wuerde, dass JEDE Bestandsdomain als
-- "neu" gilt — und der Filter ploetzlich bekannte Geschaeftspartner als
-- Erstkontakt bewertet. Wir spielen den Initialzustand aus den vorhandenen
-- email-Saetzen ein. INSERT IGNORE ist idempotent: bei spaeteren Re-Runs
-- (sollte Flyway das je tun) passiert nichts.
INSERT IGNORE INTO seen_sender_domain (domain, first_seen, email_count)
SELECT
    sender_domain,
    MIN(COALESCE(sent_at, processed_at, NOW())),
    COUNT(*)
FROM email
WHERE direction = 'IN'
  AND sender_domain IS NOT NULL
  AND sender_domain <> ''
GROUP BY sender_domain;
