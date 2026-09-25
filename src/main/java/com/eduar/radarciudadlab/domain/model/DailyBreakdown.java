package com.eduar.radarciudadlab.domain.model;

import java.time.LocalDate;

public record DailyBreakdown(
        LocalDate date,
        BreakdownDimension dimension,
        String value,
        int sessions,
        int engagedSessions,
        int pageViews,
        int eventCount,
        int keyEvents
) {
}
