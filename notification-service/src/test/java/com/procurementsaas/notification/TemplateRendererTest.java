package com.procurementsaas.notification;

import com.procurementsaas.notification.service.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for template substitution — no Spring, no database. */
class TemplateRendererTest {

    @Test
    void placeholdersAreReplacedByTheirValues() {
        String result = TemplateRenderer.render(
            "Tender {{tenderCode}} closes at {{bidDeadline}}.",
            Map.of("tenderCode", "T-1", "bidDeadline", "1 Jan 2027"));
        assertThat(result).isEqualTo("Tender T-1 closes at 1 Jan 2027.");
    }

    @Test
    void whitespaceInsideThePlaceholderIsTolerated() {
        String result = TemplateRenderer.render("Hi {{ name }}", Map.of("name", "Acme"));
        assertThat(result).isEqualTo("Hi Acme");
    }

    @Test
    void aRepeatedPlaceholderIsReplacedEveryTime() {
        String result = TemplateRenderer.render("{{x}}-{{x}}-{{x}}", Map.of("x", "9"));
        assertThat(result).isEqualTo("9-9-9");
    }

    @Test
    void anUnknownPlaceholderIsLeftVisibleRatherThanBlanked() {
        // A typo in a template should be obvious, not silently swallowed.
        String result = TemplateRenderer.render("Hello {{missing}}", Map.of());
        assertThat(result).isEqualTo("Hello {{missing}}");
    }

    @Test
    void aTemplateWithoutPlaceholdersIsUnchanged() {
        assertThat(TemplateRenderer.render("Plain text", Map.of("a", "b"))).isEqualTo("Plain text");
    }

    @Test
    void aNullTemplateRendersAsEmptyRatherThanThrowing() {
        assertThat(TemplateRenderer.render(null, Map.of())).isEmpty();
    }

    @Test
    void valuesContainingDollarOrBackslashDoNotCorruptTheOutput() {
        // Regex replacement treats $ and \ specially; the renderer must quote them.
        String result = TemplateRenderer.render("Price: {{amount}}", Map.of("amount", "$1,000\\yr"));
        assertThat(result).isEqualTo("Price: $1,000\\yr");
    }

    @Test
    void aValueThatLooksLikeAPlaceholderIsNotRenderedAgain() {
        String result = TemplateRenderer.render("{{a}}", Map.of("a", "{{b}}", "b", "boom"));
        assertThat(result).isEqualTo("{{b}}");
    }
}
