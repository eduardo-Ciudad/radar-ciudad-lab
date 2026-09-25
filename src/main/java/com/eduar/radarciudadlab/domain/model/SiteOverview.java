package com.eduar.radarciudadlab.domain.model;

import java.time.Instant;
import java.time.LocalDate;


public record SiteOverview(
        Long id,
        String name,
        String url,
        SiteType type,
        String clientName,
        boolean gaConfigured,
        AnalyticsAccessStatus gaStatus,
        Instant lastSyncAt,
        String lastSyncError,
        LocalDate dataSince
) {}