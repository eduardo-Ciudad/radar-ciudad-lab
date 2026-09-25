package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.SiteOverview;
import com.eduar.radarciudadlab.domain.model.SiteType;

import java.time.Instant;
import java.time.LocalDate;

public record SiteSummaryResponse(
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
) {

    public static SiteSummaryResponse from(SiteOverview s) {
        String error = s.gaStatus() == AnalyticsAccessStatus.OK ? null : s.lastSyncError();
        return new SiteSummaryResponse(s.id(), s.name(), s.url(), s.type(), s.clientName(),
                s.gaConfigured(), s.gaStatus(), s.lastSyncAt(), error, s.dataSince());
    }
}
