package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.QuotaUsage;
import com.eduar.radarciudadlab.domain.model.TrackedSite;

public record SiteSyncResult(
        Long siteId,
        String siteName,
        boolean success,
        AnalyticsAccessStatus status,
        int requestsMade,
        QuotaUsage quota,
        String error
) {

    public static SiteSyncResult success(TrackedSite site, int requestsMade, QuotaUsage quota) {
        return new SiteSyncResult(site.id(), site.name(), true, AnalyticsAccessStatus.OK, requestsMade, quota, null);
    }

    public static SiteSyncResult failure(TrackedSite site, AnalyticsAccessStatus status,
                                         int requestsMade, QuotaUsage quota, String error) {
        return new SiteSyncResult(site.id(), site.name(), false, status, requestsMade, quota, error);
    }
}
