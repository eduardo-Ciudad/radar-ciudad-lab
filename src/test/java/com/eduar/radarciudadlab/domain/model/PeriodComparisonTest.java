package com.eduar.radarciudadlab.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PeriodComparisonTest {

    private static final DateRange WEEK = new DateRange(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 23));

    @Test
    void calculaVariacaoPercentualEPontosDeEngajamento() {
        // semana atual: 79 sessões, 49 engajadas (62%) | anterior: 50 sessões, 25 engajadas (50%)
        MetricsSummary current = summary(79, 49);
        MetricsSummary previous = summary(50, 25);

        PeriodComparison comparison = PeriodComparison.between(current, previous);

        double sessionsChange = comparison.sessionsChangePercent();
        double engagementChange = comparison.engagementRateChangePoints();
        assertEquals(58.0, sessionsChange, 0.01);     // (79-50)/50
        assertEquals(12.03, engagementChange, 0.01);  // 62,03% - 50%
    }

    @Test
    void periodoAnteriorZeradoNaoTemBaseDeComparacao() {
        PeriodComparison comparison = PeriodComparison.between(summary(79, 49), summary(0, 0));

        assertNull(comparison.sessionsChangePercent());
        assertNull(comparison.engagementRateChangePoints());
    }

    @Test
    void somaAntesDeDividirNuncaMediaDeMedias() {
        // dia 1: 1 sessão, 1 engajada (100%) | dia 2: 99 sessões, 0 engajadas (0%)
        // média das taxas diárias daria 50%; o certo é 1/100 = 1%
        MetricsSummary summary = MetricsSummary.of(WEEK, List.of(
                new DailyMetrics(WEEK.from(), 1, 1, 0, 1, 1, 1, 0, 0),
                new DailyMetrics(WEEK.to(), 99, 0, 0, 99, 99, 99, 0, 0)));

        assertEquals(1.0, summary.engagementRate(), 0.001);
    }

    private static MetricsSummary summary(int sessions, int engaged) {
        return MetricsSummary.of(WEEK, List.of(
                new DailyMetrics(WEEK.from(), sessions, engaged, 0, sessions, sessions, sessions, 0, 0)));
    }
}
