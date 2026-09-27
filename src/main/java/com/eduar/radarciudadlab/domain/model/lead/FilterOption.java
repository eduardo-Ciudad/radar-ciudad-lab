package com.eduar.radarciudadlab.domain.model.lead;

/** Valor possível de um filtro e quantos leads têm esse valor. */
public record FilterOption(String value, long count) {}
