package com.procurementsaas.notification.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {{name}}} placeholders in a template.
 *
 * <p>Deliberately not a full expression language: templates are business-editable content,
 * and anything that can evaluate expressions against a live object graph is an injection
 * risk. Simple key lookup is all that is needed.
 *
 * <p>An unknown placeholder is left as-is rather than blanked, so a typo in a template
 * shows up plainly in the message instead of silently vanishing.
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.get(key);
            String replacement = (value != null) ? value : matcher.group(0);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
