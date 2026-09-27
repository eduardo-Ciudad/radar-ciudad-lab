package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadDetail;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.ParsedAddress;
import com.eduar.radarciudadlab.domain.model.lead.RatingPoint;
import com.eduar.radarciudadlab.domain.model.lead.RelatedLead;
import com.eduar.radarciudadlab.domain.model.lead.ScoreItem;
import com.eduar.radarciudadlab.domain.model.lead.StatusChange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * GET /api/leads/{id}. score = o que vale no funil (manual ou calculado);
 * calculatedScore + scoreBreakdown = cálculo atual, para mostrar "por que esse número".
 * dedupKey e ids de importação ficam de fora: são detalhe interno.
 */
public record LeadDetailResponse(
        Long id,
        String name,
        String category,
        String phoneE164,
        Boolean mobile,
        Address address,
        String websiteUrl,
        String instagramHandle,
        BigDecimal rating,
        Integer reviewsCount,
        LeadStatus status,
        Integer score,
        int calculatedScore,
        Integer scoreOverride,
        List<ScoreItem> scoreBreakdown,
        String notes,
        String aiSummary,
        List<RatingPoint> ratingHistory,
        List<StatusChange> statusHistory,
        List<RelatedLead> sameLocation,
        Instant createdAt,
        Instant updatedAt
) {

    public record Address(String raw, String street, String number, String complement, String neighborhood,
                          String city, String state, String zipCode) {}

    static LeadDetailResponse from(LeadDetail d) {
        Lead l = d.lead();
        ParsedAddress a = l.address();
        return new LeadDetailResponse(l.id(), l.name(), l.category(), l.phoneE164(), l.mobile(),
                new Address(a.raw(), a.street(), a.number(), a.complement(), a.neighborhood(), a.city(), a.state(),
                        a.zipCode()),
                l.websiteUrl(), l.instagramHandle(), l.rating(), l.reviewsCount(), l.status(),
                l.effectiveScore(), d.score().total(), l.scoreOverride(), d.score().items(),
                l.notes(), l.aiSummary(), d.ratingHistory(), d.statusHistory(), d.sameLocation(),
                l.createdAt(), l.updatedAt());
    }
}
