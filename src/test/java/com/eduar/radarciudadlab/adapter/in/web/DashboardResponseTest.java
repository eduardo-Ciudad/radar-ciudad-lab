package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.application.SiteDashboard;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.MetricsSummary;
import com.eduar.radarciudadlab.domain.model.PeriodComparison;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Testa a conversão domínio -> JSON: campos calculados, arredondamento e participação por canal. */
class DashboardResponseTest {

    private static final DateRange WEEK = new DateRange(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 23));

    @Test
    void converteResumoComTaxasArredondadas() {
        DashboardResponse response = DashboardResponse.from(dashboard(79, 49, 0));

        assertEquals(79, response.current().sessions());
        assertEquals(62.03, response.current().engagementRate(), 0.0001); // 49/79 = 62,0253...
        assertEquals(7, response.period().days());
        assertNull(response.comparison().sessionsChangePercent());           // anterior zerado
    }

    @Test
    void calculaParticipacaoDosCanaisENaoDosEventos() {
        DashboardResponse response = DashboardResponse.from(dashboard(79, 49, 0));

        DashboardResponse.Breakdown direct = response.channels().get(0);
        double directShare = direct.sharePercent();
        assertEquals("Direct", direct.value());
        assertEquals(49.37, directShare, 0.0001);                             // 39/79
        assertNull(response.events().get(0).sharePercent());
    }

    @Test
    void comparaComPeriodoAnterior() {
        DashboardResponse response = DashboardResponse.from(dashboard(79, 49, 50));

        double change = response.comparison().sessionsChangePercent();
        assertEquals(58.0, change, 0.0001);                                    // (79-50)/50
    }

    private static SiteDashboard dashboard(int sessions, int engaged, int previousSessions) {
        List<DailyMetrics> daily = List.of(
                new DailyMetrics(WEEK.from(), sessions, engaged, 10, sessions, sessions * 2, 100, 0, 600));
        MetricsSummary current = MetricsSummary.of(WEEK, daily);
        MetricsSummary previous = MetricsSummary.of(WEEK.previous(), previousSessions == 0 ? List.of() : List.of(
                new DailyMetrics(WEEK.previous().from(), previousSessions, previousSessions / 2, 5, previousSessions,
                        previousSessions, 50, 0, 300)));

        return new SiteDashboard(
                new TrackedSite(1L, "CiudadLab", "554830905"),
                WEEK, current, previous, PeriodComparison.between(current, previous), daily,
                List.of(new BreakdownTotal(BreakdownDimension.CHANNEL, "Direct", 39, 26, 80, 200, 0),
                        new BreakdownTotal(BreakdownDimension.CHANNEL, "Organic Social", 28, 17, 50, 150, 0)),
                List.of(),
                List.of(),
                List.of(new BreakdownTotal(BreakdownDimension.EVENT, "page_view", 0, 0, 0, 158, 0)));
    }
}
