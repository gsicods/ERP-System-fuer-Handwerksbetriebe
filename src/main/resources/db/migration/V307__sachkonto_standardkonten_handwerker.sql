-- Ergaenzt die in V303 angelegten Standard-Sachkonten um weitere, fuer
-- Handwerksbetriebe typische Konten. Fokus auf den Belege-&-Kasse-Workflow:
-- Mobile-Scans validieren, Kassenbuch fuehren, Privatentnahmen verbuchen.
--
-- Nummerierung an SKR03 angelehnt. Eindeutigkeit ueber UNIQUE(bezeichnung) aus
-- V303 -> INSERT IGNORE macht die Migration idempotent. Bezeichnungen sind
-- bewusst in Handwerker-Sprache (siehe CLAUDE.md), keine SAP-Begriffe.
--
-- Umlaute werden konsequent als 'ae/oe/ue' geschrieben (wie in V303), damit
-- die Datei zeichensatz-unabhaengig eingespielt werden kann.

INSERT IGNORE INTO sachkonto (nummer, bezeichnung, konto_typ, beschreibung, aktiv, sortierung) VALUES
  -- AUFWAND - Wareneinsatz / Fremdleistung
  ('3100', 'Fremdleistungen / Subunternehmer', 'AUFWAND', 'Bezahlte Rechnungen von Sub-Handwerkern und Dienstleistern',                 TRUE,  11),
  ('3300', 'Wareneingang 19%',                 'AUFWAND', 'Handelswaren / Material zum Weiterverkauf (19% VSt)',                        TRUE,  12),
  ('3735', 'Skonti Aufwand',                   'AUFWAND', 'Gewaehrte Skonti / Boni an Kunden',                                          TRUE,  13),

  -- AUFWAND - Personal
  ('4120', 'Loehne & Gehaelter',               'AUFWAND', 'Bruttoloehne und Gehaelter der Mitarbeiter',                                 TRUE,  21),
  ('4130', 'Sozialabgaben (AG-Anteil)',        'AUFWAND', 'Arbeitgeberanteil zur Sozialversicherung',                                   TRUE,  22),
  ('4140', 'Berufsgenossenschaft',             'AUFWAND', 'Beitraege zur BG (BG BAU, BG ETEM, ...)',                                    TRUE,  23),
  ('4150', 'Aushilfsloehne',                   'AUFWAND', 'Minijobs, Aushilfen, kurzfristig Beschaeftigte',                             TRUE,  24),
  ('4665', 'Berufskleidung',                   'AUFWAND', 'Arbeitskleidung, Schutzkleidung, Sicherheitsschuhe',                          TRUE,  25),
  ('4946', 'Fortbildung & Schulung',           'AUFWAND', 'Lehrgaenge, Meisterkurse, Sicherheitsschulungen',                            TRUE,  26),

  -- AUFWAND - Raum / Energie
  ('4210', 'Miete & Pacht (Geschaeft)',        'AUFWAND', 'Miete fuer Werkstatt, Lager, Buero',                                         TRUE,  31),
  ('4240', 'Strom, Gas, Wasser',               'AUFWAND', 'Energie- und Wasserkosten Betriebsstaette',                                  TRUE,  32),
  ('4250', 'Instandhaltung Gebaeude',          'AUFWAND', 'Reparaturen am Geschaeftsgebaeude',                                          TRUE,  33),

  -- AUFWAND - Bewirtung / Geschenke
  ('4650', 'Bewirtung geschaeftlich (70%)',    'AUFWAND', 'Bewirtungsbelege mit Kundenbezug, 70% abzugsfaehig',                         TRUE,  71),
  ('4630', 'Geschenke abzugsfaehig',           'AUFWAND', 'Kundengeschenke bis 50 EUR netto',                                           TRUE,  72),
  ('4635', 'Geschenke nicht abzugsfaehig',     'AUFWAND', 'Geschenke ueber 50 EUR netto',                                               TRUE,  73),

  -- AUFWAND - Buero / Kommunikation / IT
  ('4920', 'Porto',                            'AUFWAND', 'Briefporto, Paketversand',                                                   TRUE,  41),
  ('4925', 'Software & IT-Abos',               'AUFWAND', 'Cloud-Software, Lizenzen, Office-Abos',                                      TRUE,  42),

  -- AUFWAND - Beratung / Steuern
  ('4955', 'Buchfuehrungs- & Steuerberatung',  'AUFWAND', 'Honorare Steuerberater, Buchfuehrungsservice',                               TRUE,  91),
  ('4957', 'Rechts- & Beratungskosten',        'AUFWAND', 'Anwaltskosten, Unternehmensberatung',                                        TRUE,  92),
  ('4390', 'Beitraege IHK / HWK / Innung',     'AUFWAND', 'Pflichtbeitraege Kammer, Innung, Verbaende',                                 TRUE,  93),

  -- AUFWAND - Finanzen
  ('4970', 'Bankgebuehren & Kontofuehrung',    'AUFWAND', 'Kontofuehrung, Kartengebuehren, Auslandsspesen',                             TRUE, 101),
  ('4975', 'Zinsaufwand',                      'AUFWAND', 'Zinsen fuer Betriebskredite, Kontokorrent',                                  TRUE, 102),

  -- AUFWAND - Abschreibungen
  ('4830', 'Abschreibung Anlagen (AfA)',       'AUFWAND', 'Planmaessige Abschreibung Sachanlagen',                                      TRUE, 110),
  ('4855', 'Abschreibung GWG',                 'AUFWAND', 'Sofortabschreibung geringwertiger Wirtschaftsgueter (bis 800 EUR)',          TRUE, 111),

  -- ERTRAG - weitere Erloesarten
  ('8338', 'Erloese steuerfrei (Reverse Charge)', 'ERTRAG', 'Bauleistungen an andere Unternehmer (§ 13b UStG)',                          TRUE, 320),
  ('8125', 'Erloese steuerfrei innergem.',     'ERTRAG', 'Innergemeinschaftliche Lieferungen (EU)',                                     TRUE, 330),
  ('8736', 'Skontoertraege',                   'ERTRAG', 'Erhaltene Skonti von Lieferanten',                                            TRUE, 340),
  ('8100', 'Mieteinnahmen',                    'ERTRAG', 'Vermietung von Raeumen / Gegenstaenden',                                      TRUE, 350),

  -- PRIVAT - weitere Privatkonten
  ('1820', 'Privatsteuer (Einkommensteuer)',   'PRIVAT', 'Ueberwiesene Einkommensteuer-Vorauszahlung an FA',                            TRUE, 420),
  ('1830', 'Privatanteil KFZ',                 'PRIVAT', 'Private Nutzung Firmenfahrzeug',                                              TRUE, 430),
  ('1840', 'Privatanteil Telefon',             'PRIVAT', 'Privatnutzungsanteil Festnetz / Mobilfunk',                                   TRUE, 440),

  -- NEUTRAL - weitere
  ('1600', 'Geldtransit',                      'NEUTRAL', 'Geld unterwegs zwischen Kasse / Bank (Verrechnung)',                         TRUE, 520);
