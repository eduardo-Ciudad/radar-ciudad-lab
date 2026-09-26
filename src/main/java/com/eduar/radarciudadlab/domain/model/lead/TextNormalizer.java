package com.eduar.radarciudadlab.domain.model.lead;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


public final class TextNormalizer {

    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private TextNormalizer() {}

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = ACCENTS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALPHANUMERIC.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength).trim();
    }
}
