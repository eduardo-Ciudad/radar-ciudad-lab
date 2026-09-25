package com.eduar.radarciudadlab.domain.model;

public record PeriodComparison(
        Double sessionsChangePercent,
        Double newUsersChangePercent,
        Double pageViewsChangePercent,
        Double keyEventsChangePercent,
        Double engagementRateChangePoints
) {

    public static PeriodComparison between(MetricsSummary current, MetricsSummary previous) {
        return new PeriodComparison(
                MetricsSummary.percentChange(current.sessions(), previous.sessions()),
                MetricsSummary.percentChange(current.newUsers(), previous.newUsers()),
                MetricsSummary.percentChange(current.pageViews(), previous.pageViews()),
                MetricsSummary.percentChange(current.keyEvents(), previous.keyEvents()),
                previous.sessions() == 0 ? null : current.engagementRate() - previous.engagementRate());
    }
}