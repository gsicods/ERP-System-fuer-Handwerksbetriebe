package org.example.kalkulationsprogramm.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Reine Unit-Tests für {@link UnifiedEmailController#extractFirstEmailAddress(String)}.
 * Kein Spring-Context, damit die ReDoS-Stress-Assertion zuverlässig in Millisekunden misst.
 *
 * <p>Hintergrund: Das zugrunde liegende Pattern wurde nach CodeQL-Alert
 * {@code java/polynomial-redos} auf possessive Quantoren umgestellt. Diese Tests
 * sichern beide Seiten ab — Korrektheit für gültige Header und Backtracking-Freiheit
 * für adversariale Eingaben.
 */
@DisplayName("UnifiedEmailController.extractFirstEmailAddress")
class UnifiedEmailControllerExtractEmailTest {

    @Test
    @DisplayName("null-Eingabe → null")
    void nullInput_returnsNull() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(null)).isNull();
    }

    @Test
    @DisplayName("Leere/whitespace-Eingabe → null")
    void blankInput_returnsNull() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress("")).isNull();
        assertThat(UnifiedEmailController.extractFirstEmailAddress("   \t \n")).isNull();
    }

    @Test
    @DisplayName("Reine E-Mail-Adresse → unverändert zurück")
    void plainAddress_returnsItself() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress("max@mustermann.de"))
                .isEqualTo("max@mustermann.de");
    }

    @Test
    @DisplayName("Display-Name mit Adresse in spitzen Klammern → nur Adresse")
    void displayNameAndAddress_returnsAddressOnly() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(
                "\"Max Mustermann\" <max@mustermann.de>"))
                .isEqualTo("max@mustermann.de");
    }

    @Test
    @DisplayName("Mehrere Adressen → erste gewinnt")
    void multipleAddresses_returnsFirst() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(
                "first@example.com, second@example.com, third@example.com"))
                .isEqualTo("first@example.com");
    }

    @Test
    @DisplayName("Subdomains und mehrteilige TLDs werden korrekt erkannt")
    void subdomainAndMultiPartTld() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(
                "foo.bar@sub.example.co.uk"))
                .isEqualTo("foo.bar@sub.example.co.uk");
    }

    @Test
    @DisplayName("Punkt, Plus und Bindestrich im Local-Part werden gematcht")
    void specialCharsInLocalPart() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(
                "user.name+tag-1@my-host.example.org"))
                .isEqualTo("user.name+tag-1@my-host.example.org");
    }

    @Test
    @DisplayName("Eingehende-Mail-Header mit Sammeladresse → Adresse extrahiert")
    void groupAddressHeader_returnsAddress() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress(
                "info@gemeinde-musterstadt.de"))
                .isEqualTo("info@gemeinde-musterstadt.de");
    }

    @Test
    @DisplayName("Header ohne gültige Adresse → null")
    void noEmailFound_returnsNull() {
        assertThat(UnifiedEmailController.extractFirstEmailAddress("kein-treffer-hier"))
                .isNull();
        assertThat(UnifiedEmailController.extractFirstEmailAddress("only @ sign no domain"))
                .isNull();
    }

    @Test
    @DisplayName("Adversariale Eingabe (3.000 % gefolgt von Adresse) läuft in <500ms (kein ReDoS)")
    void adversarialInput_isLinear() {
        // 3000 < EMAIL_HEADER_MAX_LEN (4096): nicht abgeschnitten, das `@example.com` ist Teil des Inputs.
        String adversarial = "%".repeat(3_000) + "@example.com";
        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            String result = UnifiedEmailController.extractFirstEmailAddress(adversarial);
            assertThat(result).isNotNull().endsWith("@example.com");
        });
    }

    @Test
    @DisplayName("Adversariale Eingabe ohne @ (10k Zeichen) läuft in <500ms (kein polynomiales Backtracking)")
    void adversarialInputWithoutAt_isLinear() {
        // Klassischer ReDoS-Auslöser für das alte Pattern: viele % ohne `@` → ohne possessive
        // Quantoren würde der Engine an jeder Startposition den ++ greedy probieren und scheitern.
        String adversarial = "%".repeat(10_000);
        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                assertThat(UnifiedEmailController.extractFirstEmailAddress(adversarial)).isNull());
    }

    @Test
    @DisplayName("Über-langer Header (> 4096 Zeichen) wird abgeschnitten und liefert keine Exception")
    void overlongHeader_truncatedSafely() {
        String longPrefix = "x".repeat(5_000);
        String header = longPrefix + " <admin@example.com>";
        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                UnifiedEmailController.extractFirstEmailAddress(header));
    }
}
