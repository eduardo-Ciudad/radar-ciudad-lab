package com.eduar.radarciudadlab.domain.model;

import java.util.List;

public record MetricsSummary(
        DateRange range,
        long sessions,
        long engagedSessions,
        long newUsers,
        long pageViews,
        long keyEvents,
        long engagementSeconds,
        long sumOfDailyActiveUsers
) {

    public static MetricsSummary of(DateRange range, List<DailyMetrics> days) {
        long sessions = 0, engaged = 0, newUsers = 0, views = 0, keyEvents = 0, seconds = 0, dailyActive = 0;
        for (DailyMetrics d : days) {
            sessions += d.sessions();
            engaged += d.engagedSessions();
            newUsers += d.newUsers();
            views += d.pageViews();
            keyEvents += d.keyEvents();
            seconds += d.engagementSeconds();
            dailyActive += d.activeUsers();
        }
        return new MetricsSummary(range, sessions, engaged, newUsers, views, keyEvents, seconds, dailyActive);
    }


    public double engagementRate() {
        return percent(engagedSessions, sessions);
    }


    public double conversionRate() {
        return percent(keyEvents, sessions);
    }


    public double averageEngagementSeconds() {
        return sumOfDailyActiveUsers == 0 ? 0 : (double) engagementSeconds / sumOfDailyActiveUsers;
    }

    public double pagesPerSession() {
        return sessions == 0 ? 0 : (double) pageViews / sessions;
    }


    public static Double percentChange(long current, long previous) {
        if (previous == 0) return null;
        return (current - previous) * 100.0 / previous;
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0 : part * 100.0 / total;
    }
}