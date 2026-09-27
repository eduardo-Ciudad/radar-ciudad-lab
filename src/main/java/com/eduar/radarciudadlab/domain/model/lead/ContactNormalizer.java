package com.eduar.radarciudadlab.domain.model.lead;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normaliza site e Instagram, venham do CSV ou da edição manual. Vazio vira null. */
public final class ContactNormalizer {

    private static final Pattern INSTAGRAM_URL = Pattern.compile("instagram\\.com/([A-Za-z0-9._]+)");
    private static final Pattern INSTAGRAM_HANDLE = Pattern.compile("[a-z0-9._]{1,30}");
    private static final Pattern HAS_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    private ContactNormalizer() {}

    /** Aceita "@perfil", "perfil" ou a URL do perfil; guarda só o handle, sem @, em minúsculas. */
    public static String instagramHandle(String value) {
        String text = TextNormalizer.blankToNull(value);
        if (text == null) {
            return null;
        }
        Matcher url = INSTAGRAM_URL.matcher(text);
        String handle = url.find() ? url.group(1) : text.replaceFirst("^@", "");
        return TextNormalizer.limit(handle.toLowerCase(Locale.ROOT), 100);
    }

    /** Igual a instagramHandle, mas recusa texto que não pareça um perfil (usado na edição manual). */
    public static String validInstagramHandle(String value) {
        String handle = instagramHandle(value);
        if (handle != null && !INSTAGRAM_HANDLE.matcher(handle).matches()) {
            throw new IllegalArgumentException("Instagram inválido: use @perfil ou o link do perfil");
        }
        return handle;
    }

    /** "clinica.com.br" -> "https://clinica.com.br". Recusa texto sem ponto (não é um domínio). */
    public static String websiteUrl(String value) {
        String text = TextNormalizer.blankToNull(value);
        if (text == null) {
            return null;
        }
        if (text.contains(" ") || !text.contains(".")) {
            throw new IllegalArgumentException("Site inválido: informe um endereço como clinica.com.br");
        }
        String url = HAS_SCHEME.matcher(text).matches() ? text : "https://" + text;
        return TextNormalizer.limit(url, 500);
    }
}
