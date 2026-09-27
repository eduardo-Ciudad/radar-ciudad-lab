package com.eduar.radarciudadlab.domain.model.lead;

import java.math.BigDecimal;
import java.time.Instant;

/** Resumo do lead para o card do funil e para a listagem. score = efetivo (ajuste manual ou calculado). */
public record LeadCard(
        Long id,
        String name,
        String category,
        String neighborhood,
        String city,
        String phoneE164,
        Boolean mobile,
        String websiteUrl,
        String instagramHandle,
        BigDecimal rating,
        Integer reviewsCount,
        LeadStatus status,
        Integer score,
        boolean scoreOverridden,
        Instant updatedAt
) {}
