package com.eduar.radarciudadlab.domain.model;

import java.time.LocalDate;

public record DailyMetrics(
        LocalDate date,
        int sessions,
        int engagedSessions,
        int newUsers,
        int activeUsers,
        int pageViews,
        int eventCount,
        int keyEvents,
        long engagementSeconds
) {
}
