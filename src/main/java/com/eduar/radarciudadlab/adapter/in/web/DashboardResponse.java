package com.eduar.radarciudadlab.adapter.in.web;


import com.eduar.radarciudadlab.application.SiteDashboard;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.MetricsSummary;
import com.eduar.radarciudadlab.domain.model.PeriodComparison;

import java.time.LocalDate;
import java.util.List;


public record DashboardResponse(
        SiteInfo site,
        Period period,
        Summary current,
        Summary previous,
        Comparison comparison,
        List<DailyPoint> daily,
        List<Breakdown> channels,
        List<Breakdown> landingPages,
        List<Breakdown> devices,
        List<Breakdown> events
) {

    public record SiteInfo(Long id, String name) {}

    public record Period(LocalDate from, LocalDate to, long days) {}

    public record Summary(
            long sessions,
            long engagedSessions,
            long newUsers,
            long pageViews,
            long keyEvents,
            double engagementRate,
            double conversionRate,
            double pagesPerSession,
            double averageEngagementSeconds
    ) {
        static Summary from(MetricsSummary s) {
            return new Summary(s.sessions(), s.engagedSessions(), s.newUsers(), s.pageViews(), s.keyEvents(),
                    round(s.engagementRate()), round(s.conversionRate()),
                    round(s.pagesPerSession()), round(s.averageEngagementSeconds()));
        }
    }

    public record Comparison(
            Double sessionsChangePercent,
            Double newUsersChangePercent,
            Double pageViewsChangePercent,
            Double keyEventsChangePercent,
            Double engagementRateChangePoints
    ) {
        static Comparison from(PeriodComparison c) {
            return new Comparison(round(c.sessionsChangePercent()), round(c.newUsersChangePercent()),
                    round(c.pageViewsChangePercent()), round(c.keyEventsChangePercent()),
                    round(c.engagementRateChangePoints()));
        }
    }

    public record DailyPoint(LocalDate date, int sessions, int engagedSessions, int newUsers,
                             int pageViews, int keyEvents) {
        static DailyPoint from(DailyMetrics d) {
            return new DailyPoint(d.date(), d.sessions(), d.engagedSessions(), d.newUsers(),
                    d.pageViews(), d.keyEvents());
        }
    }

    public record Breakdown(String value, long sessions, long engagedSessions, long pageViews,
                            long eventCount, long keyEvents, double engagementRate, Double sharePercent) {
        static Breakdown from(BreakdownTotal b, long totalSessions) {
            Double share = b.dimension() == BreakdownDimension.EVENT ? null : round(b.shareOf(totalSessions));
            return new Breakdown(b.value(), b.sessions(), b.engagedSessions(), b.pageViews(),
                    b.eventCount(), b.keyEvents(), round(b.engagementRate()), share);
        }
    }

    public static DashboardResponse from(SiteDashboard d) {
        long total = d.current().sessions();
        return new DashboardResponse(
                new SiteInfo(d.site().id(), d.site().name()),
                new Period(d.range().from(), d.range().to(), d.range().days()),
                Summary.from(d.current()),
                Summary.from(d.previous()),
                Comparison.from(d.comparison()),
                d.daily().stream().map(DailyPoint::from).toList(),
                breakdowns(d.topChannels(), total),
                breakdowns(d.topLandingPages(), total),
                breakdowns(d.devices(), total),
                breakdowns(d.topEvents(), total));
    }

    private static List<Breakdown> breakdowns(List<BreakdownTotal> items, long totalSessions) {
        return items.stream().map(b -> Breakdown.from(b, totalSessions)).toList();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Double round(Double value) {
        return value == null ? null : round(value.doubleValue());
    }
}