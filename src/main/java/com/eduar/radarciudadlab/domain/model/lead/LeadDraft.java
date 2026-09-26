package com.eduar.radarciudadlab.domain.model.lead;

import java.math.BigDecimal;



public record LeadDraft(
        int line,
        String name,
        String category,
        PhoneNumber phone,
        ParsedAddress address,
        String websiteUrl,
        String instagramHandle,
        BigDecimal rating,
        LeadStatus status
) {

    private static final int NAME_KEY_MAX = 180;

    /**
     * Chave de reimportação: nome normalizado + telefone E.164, ou + endereço quando não há telefone.
     * O telefone sozinho não serve: a clínica e um profissional dela dividem o mesmo número.
     */
    public String dedupKey() {
        String nameKey = TextNormalizer.limit(TextNormalizer.normalize(name), NAME_KEY_MAX);
        String locationKey = phone != null
                ? phone.e164()
                : "end:" + address.normalized();
        return TextNormalizer.limit(nameKey + "|" + locationKey, 255);
    }
}