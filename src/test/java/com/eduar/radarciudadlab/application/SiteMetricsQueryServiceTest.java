package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.PeriodUsers;
import com.eduar.radarciudadlab.domain.model.SiteOverview;
import com.eduar.radarciudadlab.domain.model.SiteType;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import com.eduar.radarciudadlab.domain.port.out.SiteOverviewRepository;
import com.eduar.radarciudadlab.domain.port.out.TrackedSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste unitário das consultas do dashboard: sem banco, sem Spring.
 * Foco no limite de período (MAX_RANGE_DAYS), que protege o banco de consultas gigantes no GET.
 */
class SiteMetricsQueryServiceTest {

    private static final Long SITE_ID = 4L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private FakeSites sites;
    private SiteMetricsQueryService service;

    @BeforeEach
    void setUp() {
        sites = new FakeSites();
        sites.add(new TrackedSite(SITE_ID, "CiudadLab", "554830905"));

        FakeOverviews overviews = new FakeOverviews();
        overviews.add(new SiteOverview(SITE_ID, "CiudadLab", "https://ciudadlab.com.br", SiteType.OWN, null,
                true, AnalyticsAccessStatus.OK, null, null, START));

        service = new SiteMetricsQueryService(new EmptyMetrics(), sites, overviews);
    }

    @Test
    void periodoAcimaDoLimiteERejeitadoAntesDeConsultarOBanco() {
        DateRange range = new DateRange(START, START.plusDays(366));
        assertEquals(367, range.days());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.getDashboard(SITE_ID, range));

        assertTrue(e.getMessage().contains("366"));
        assertEquals(0, sites.findByIdCalls, "a validação precisa vir antes de qualquer acesso ao repositório");
    }

    @Test
    void periodoExatamenteNoLimiteEAceito() {
        DateRange range = new DateRange(START, START.plusDays(365));
        assertEquals(366, range.days());

        SiteDashboard dashboard = service.getDashboard(SITE_ID, range);

        assertEquals(range, dashboard.range());
        assertEquals(range.previous(), dashboard.previous().range());
    }

    @Test
    void siteInexistenteLancaSiteNotFound() {
        DateRange range = new DateRange(START, START.plusDays(27));

        assertThrows(SiteNotFoundException.class, () -> service.getDashboard(99L, range));
    }

    // ---- Fakes ----

    private static final class FakeSites implements TrackedSiteRepository {
        private final Map<Long, TrackedSite> byId = new HashMap<>();
        int findByIdCalls;

        void add(TrackedSite site) {
            byId.put(site.id(), site);
        }

        @Override
        public List<TrackedSite> findAllWithAnalytics() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<TrackedSite> findById(Long siteId) {
            findByIdCalls++;
            return Optional.ofNullable(byId.get(siteId));
        }

        @Override
        public void markSyncSuccess(Long siteId) {
            throw new UnsupportedOperationException("consulta não deve gravar");
        }

        @Override
        public void markSyncFailure(Long siteId, AnalyticsAccessStatus status, String error) {
            throw new UnsupportedOperationException("consulta não deve gravar");
        }
    }

    private static final class FakeOverviews implements SiteOverviewRepository {
        private final Map<Long, SiteOverview> byId = new HashMap<>();

        void add(SiteOverview overview) {
            byId.put(overview.id(), overview);
        }

        @Override
        public List<SiteOverview> findAll() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<SiteOverview> findById(Long siteId) {
            return Optional.ofNullable(byId.get(siteId));
        }
    }

    /** Site sem dados coletados: todas as leituras vêm vazias, e escrita é erro (consulta não grava). */
    private static final class EmptyMetrics implements AnalyticsMetricsRepository {

        @Override
        public void upsertDailyMetrics(Long siteId, List<DailyMetrics> metrics) {
            throw new UnsupportedOperationException("consulta não deve gravar");
        }

        @Override
        public void replaceDailyBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range,
                                           List<DailyBreakdown> breakdowns) {
            throw new UnsupportedOperationException("consulta não deve gravar");
        }

        @Override
        public void upsertPeriodUsers(Long siteId, PeriodUsers users) {
            throw new UnsupportedOperationException("consulta não deve gravar");
        }

        @Override
        public List<DailyMetrics> findDailyMetrics(Long siteId, DateRange range) {
            return List.of();
        }

        @Override
        public List<BreakdownTotal> findTopBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range,
                                                      int limit) {
            return List.of();
        }
    }
}
