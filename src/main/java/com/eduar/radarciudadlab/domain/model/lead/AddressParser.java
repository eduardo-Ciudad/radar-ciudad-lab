package com.eduar.radarciudadlab.domain.model.lead;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressParser {

    private static final Pattern FORMAT = Pattern.compile(
            "^(?<street>[^,]+),\\s*(?<number>[^,-]+?)\\s*-\\s*(?<middle>.+),\\s*(?<city>[^,]+?)\\s*-\\s*"
                    + "(?<state>[A-Za-z]{2}),\\s*(?<zip>\\d{5})-?(?<zipSuffix>\\d{3})$");

    private static final Pattern MIDDLE_SEPARATOR = Pattern.compile("\\s+-\\s+");

    private static final Map<String, String> STREET_TYPES = Map.of(
            "r", "rua",
            "av", "avenida",
            "al", "alameda",
            "tv", "travessa",
            "trav", "travessa",
            "pca", "praca",
            "rod", "rodovia",
            "estr", "estrada");

    private AddressParser() {}

    public static ParsedAddress parse(String raw) {
        String text = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        Matcher m = FORMAT.matcher(text);
        if (!m.matches()) {
            return new ParsedAddress(text, null, null, null, null, null, null, null, TextNormalizer.normalize(text));
        }

        String[] middle = MIDDLE_SEPARATOR.split(m.group("middle").trim());
        String neighborhood = middle[middle.length - 1].trim();
        String complement = middle.length > 1
                ? String.join(" - ", Arrays.copyOf(middle, middle.length - 1)).trim()
                : null;

        String street = m.group("street").trim();
        String number = m.group("number").trim();
        String zipCode = m.group("zip") + "-" + m.group("zipSuffix");

        return new ParsedAddress(
                text,
                TextNormalizer.limit(street, 255),
                TextNormalizer.limit(number, 20),
                TextNormalizer.limit(complement, 100),
                TextNormalizer.limit(neighborhood, 120),
                TextNormalizer.limit(m.group("city").trim(), 120),
                m.group("state").toUpperCase(),
                zipCode,
                normalizeStreet(street) + " " + TextNormalizer.normalize(number) + " " + zipCode);
    }

    private static String normalizeStreet(String street) {
        String normalized = TextNormalizer.normalize(street);
        int space = normalized.indexOf(' ');
        if (space < 0) {
            return normalized;
        }
        String type = STREET_TYPES.getOrDefault(normalized.substring(0, space), normalized.substring(0, space));
        return type + normalized.substring(space);
    }
}
