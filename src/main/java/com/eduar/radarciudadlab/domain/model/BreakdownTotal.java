package com.eduar.radarciudadlab.domain.model;

/** Soma de um valor de dimensão num período. Ex.: CHANNEL "Direct" = 39 sessões na semana. */
public record BreakdownTotal(
        BreakdownDimension dimension,
        String value,
        long sessions,
        long engagedSessions,
        long pageViews,
        long eventCount,
        long keyEvents
) {

    /** % de sessões engajadas deste valor (0 a 100). */
    public double engagementRate() {
        return sessions == 0 ? 0 : engagedSessions * 100.0 / sessions;
    }

    /** Participação nas sessões do período (0 a 100), dado o total de sessões do site. */
    public double shareOf(long totalSessions) {
        return totalSessions == 0 ? 0 : sessions * 100.0 / totalSessions;
    }
}
