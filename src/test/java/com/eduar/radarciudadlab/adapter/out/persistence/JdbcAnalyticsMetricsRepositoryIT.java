package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Roda contra o PostgreSQL local. Tudo é desfeito no fim de cada teste (@Transactional). */
@SpringBootTest
@Transactional
class JdbcAnalyticsMetricsRepositoryIT {

    private static final LocalDate D1 = LocalDate.of(2026, 9, 19);
    private static final LocalDate D2 = LocalDate.of(2026, 9, 20);
    private static final DateRange RANGE = new DateRange(D1, D2);

    @Autowired AnalyticsMetricsRepository repository;
    @Autowired NamedParameterJdbcTemplate jdbc;

    private Long siteId;

    @BeforeEach
    void criaSiteDeTeste() {
        siteId = jdbc.queryForObject(
                "INSERT INTO site (name, url, ga_property_id) VALUES ('Teste', 'https://teste.local', '000') RETURNING id",
                new MapSqlParameterSource(), Long.class);
    }

    @Test
    void upsertSobrescreveODiaExistente() {
        repository.upsertDailyMetrics(siteId, List.of(daily(D1, 10)));
        repository.upsertDailyMetrics(siteId, List.of(daily(D1, 12))); // reprocessamento

        List<DailyMetrics> saved = repository.findDailyMetrics(siteId, RANGE);
        assertEquals(1, saved.size());
        assertEquals(12, saved.get(0).sessions());
    }

    @Test
    void replaceRemoveValoresQueSumiram() {
        repository.replaceDailyBreakdowns(siteId, BreakdownDimension.CHANNEL, RANGE, List.of(
                channel(D1, "(not set)", 3), channel(D1, "Direct", 5)));

        // GA reclassificou: "(not set)" virou "Organic Search"
        repository.replaceDailyBreakdowns(siteId, BreakdownDimension.CHANNEL, RANGE, List.of(
                channel(D1, "Organic Search", 3), channel(D1, "Direct", 5)));

        List<BreakdownTotal> top = repository.findTopBreakdowns(siteId, BreakdownDimension.CHANNEL, RANGE, 10);
        assertEquals(2, top.size());
        assertEquals(List.of("Direct", "Organic Search"), top.stream().map(BreakdownTotal::value).toList());
    }

    @Test
    void topSomaOsDiasDoPeriodo() {
        repository.replaceDailyBreakdowns(siteId, BreakdownDimension.CHANNEL, RANGE, List.of(
                channel(D1, "Direct", 12), channel(D2, "Direct", 7), channel(D1, "Organic Social", 3)));

        List<BreakdownTotal> top = repository.findTopBreakdowns(siteId, BreakdownDimension.CHANNEL, RANGE, 10);
        assertEquals("Direct", top.get(0).value());
        assertEquals(19, top.get(0).sessions());
    }

    private static DailyMetrics daily(LocalDate date, int sessions) {
        return new DailyMetrics(date, sessions, sessions / 2, 1, sessions, sessions * 2, sessions * 5, 0, 100);
    }

    private static DailyBreakdown channel(LocalDate date, String value, int sessions) {
        return new DailyBreakdown(date, BreakdownDimension.CHANNEL, value, sessions, sessions / 2, sessions, sessions, 0);
    }
}
