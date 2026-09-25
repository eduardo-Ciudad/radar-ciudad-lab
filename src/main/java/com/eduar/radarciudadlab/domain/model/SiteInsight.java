package com.eduar.radarciudadlab.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record SiteInsight(
        Long id,
        Long siteId,
        InsightType type,
        InsightSeverity severity,
        LocalDate periodStart,
        LocalDate periodEnd,
        String title,
        String body,
        Instant createdAt,
        Instant readAt
) {}