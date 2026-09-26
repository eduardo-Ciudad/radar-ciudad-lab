package com.eduar.radarciudadlab.domain.model.lead;

import java.util.Arrays;

public enum LeadStatus {
    NOVO,
    CONTATADO,
    REUNIAO,
    PROPOSTA,
    FECHADO,
    PERDIDO,
    DESCARTADO;


    public static LeadStatus fromCsv(String value) {
        if (value == null || value.isBlank()) {
            return NOVO;
        }
        String normalized = TextNormalizer.normalize(value).replace(" ", "");
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(NOVO);
    }
}
