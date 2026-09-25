package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.model.InsightSeverity;
import com.eduar.radarciudadlab.domain.model.InsightType;
import com.eduar.radarciudadlab.domain.model.SiteInsight;

import java.time.Instant;
import java.time.LocalDate;

public record SiteInsightResponse(
        Long id,
        InsightType type,
        InsightSeverity severity,
        LocalDate periodStart,
        LocalDate periodEnd,
        String title,
        String body,
        Instant createdAt,
        boolean read
) {

    public static SiteInsightResponse from(SiteInsight i) {
        return new SiteInsightResponse(i.id(), i.type(), i.severity(), i.periodStart(), i.periodEnd(),
                i.title(), i.body(), i.createdAt(), i.readAt() != null);
    }
}
