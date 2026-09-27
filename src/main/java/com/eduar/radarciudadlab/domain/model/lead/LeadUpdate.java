package com.eduar.radarciudadlab.domain.model.lead;

/**
 * Edição parcial. Campo null = não mexe.
 * Textos (notes, websiteUrl, instagramHandle): "" apaga. scoreOverride: clearScoreOverride=true volta ao calculado.
 * statusNote vai para o histórico junto com a mudança de status.
 */
public record LeadUpdate(
        LeadStatus status,
        String statusNote,
        String notes,
        String websiteUrl,
        String instagramHandle,
        Integer scoreOverride,
        boolean clearScoreOverride
) {}
