package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/** Tudo sobre um lead para a tela de detalhe. score = cálculo atual com a explicação por critério. */
public record LeadDetail(
        Lead lead,
        LeadScore score,
        List<RatingPoint> ratingHistory,
        List<StatusChange> statusHistory,
        List<RelatedLead> sameLocation
) {}
