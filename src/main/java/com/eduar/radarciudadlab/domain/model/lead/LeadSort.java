package com.eduar.radarciudadlab.domain.model.lead;

import java.util.Arrays;

/** Ordenações aceitas na listagem. */
public enum LeadSort {
    SCORE,
    RATING,
    NAME,
    RECENT;

    public static LeadSort from(String value) {
        if (value == null || value.isBlank()) {
            return SCORE;
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "O parâmetro 'sort' deve ser score, rating, name ou recent"));
    }
}
