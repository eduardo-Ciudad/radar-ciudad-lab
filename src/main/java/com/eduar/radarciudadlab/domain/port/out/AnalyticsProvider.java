package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.*;

public interface AnalyticsProvider {
    FetchResult<DailyMetrics> fetchDailyMetrics(String propertyId, DateRange range);

    FetchResult<DailyBreakdown> fetchDailyBreakdown(String propertyId, DateRange range, BreakdownDimension dimension);

    FetchResult<PeriodUsers> fetchPeriodUsers(String propertyId, DateRange range);
}