package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.model.*;

import java.time.LocalDate;
import java.util.List;

public record SiteDashboard(
        TrackedSite site,
        AnalyticsAccessStatus gaStatus,
        LocalDate dataSince,
        DateRange range,
        MetricsSummary current,
        MetricsSummary previous,
        PeriodComparison comparison,
        List<DailyMetrics> daily,
        List<BreakdownTotal> topChannels,
        List<BreakdownTotal> topLandingPages,
        List<BreakdownTotal> devices,
        List<BreakdownTotal> topEvents
) {}
