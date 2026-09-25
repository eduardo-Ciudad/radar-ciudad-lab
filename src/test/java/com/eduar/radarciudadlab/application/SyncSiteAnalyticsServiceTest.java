package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.AnalyticsAccessDeniedException;
import com.eduar.radarciudadlab.domain.exception.AnalyticsQuotaExceededException;
import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.FetchResult;
import com.eduar.radarciudadlab.domain.model.PeriodUsers;
import com.eduar.radarciudadlab.domain.model.QuotaUsage;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsProvider;
import com.eduar.radarciudadlab.domain.port.out.SyncRunLog;
import com.eduar.radarciudadlab.domain.port.out.TrackedSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste unitário do sync: sem Google, sem banco, sem Spring.
 * As portas são substituídas por fakes em memória — é aqui que a arquitetura hexagonal se paga.
 */
class SyncSiteAnalyticsServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(6, 0).atZone(SP).toInstant(), SP);

    private FakeProvider provider;
    private FakeMetrics metrics;
    private FakeSites sites;
    private FakeRunLog runLog;
    private SyncSiteAnalyticsService service;

    @BeforeEach
    void setUp() {
        provider = new FakeProvider();
        metrics = new FakeMetrics();
        sites = new FakeSites();
        runLog = new FakeRunLog();
        service = new SyncSiteAnalyticsService(provider, metrics, sites, runLog, CLOCK);
    }

    @Test
    void sincronizaTudoEMarcaSucesso() {
        sites.add(new TrackedSite(1L, "CiudadLab", "111"));

        List<SiteSyncResult> results = service.syncAll();

        SiteSyncResult result = results.get(0);
        assertTrue(result.success());
        assertEquals(1 + BreakdownDimension.values().length + 1, result.requestsMade()); // diário + 7 quebras + usuários
        assertEquals(9, result.quota().tokensConsumed());                                 // 1 token por chamada no fake

        // janela diária: hoje-3 até ontem
        assertEquals(new DateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 23)), provider.dailyRanges.get(0));
        // usuários: mês corrente até ontem
        assertEquals(new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 23)), provider.periodRanges.get(0));

        assertEquals(1, metrics.dailyWrites);
        assertEquals(Set.of(BreakdownDimension.values()), metrics.replacedDimensions);
        assertEquals(1, metrics.periodWrites);
        assertEquals(AnalyticsAccessStatus.OK, sites.status.get(1L));
        assertEquals("SUCCESS", runLog.status.get(1L));
    }

    @Test
    void semAcessoMarcaNoAccessENaoGravaMetricas() {
        sites.add(new TrackedSite(1L, "Cliente", "111"));
        provider.denied.add("111");

        SiteSyncResult result = service.syncAll().get(0);

        assertFalse(result.success());
        assertEquals(AnalyticsAccessStatus.NO_ACCESS, result.status());
        assertEquals(AnalyticsAccessStatus.NO_ACCESS, sites.status.get(1L));
        assertEquals("FAILED", runLog.status.get(1L));
        assertEquals(0, metrics.dailyWrites);
    }

    @Test
    void falhaDeUmSiteNaoInterrompeOsOutros() {
        sites.add(new TrackedSite(1L, "Cota estourada", "111"));
        sites.add(new TrackedSite(2L, "Saudável", "222"));
        provider.quotaExceeded.add("111");

        List<SiteSyncResult> results = service.syncAll();

        assertEquals(2, results.size());
        assertFalse(results.get(0).success());
        assertEquals(AnalyticsAccessStatus.ERROR, results.get(0).status());
        assertTrue(results.get(1).success());
        assertEquals(AnalyticsAccessStatus.OK, sites.status.get(2L));
    }

    @Test
    void falhaNoMeioRegistraQuantasChamadasForamFeitas() {
        sites.add(new TrackedSite(1L, "Instável", "111"));
        provider.failOnDimension = BreakdownDimension.PAGE; // 1 diário + CHANNEL, SOURCE, LANDING_PAGE ok, PAGE falha

        SiteSyncResult result = service.syncAll().get(0);

        assertFalse(result.success());
        assertEquals(4, result.requestsMade());
        assertEquals(4, runLog.requests.get(1L));
    }

    // ------------------------------------------------------------------ fakes

    private static final class FakeProvider implements AnalyticsProvider {
        final Set<String> denied = new HashSet<>();
        final Set<String> quotaExceeded = new HashSet<>();
        BreakdownDimension failOnDimension;
        final List<DateRange> dailyRanges = new ArrayList<>();
        final List<DateRange> periodRanges = new ArrayList<>();

        private void check(String propertyId) {
            if (denied.contains(propertyId)) throw new AnalyticsAccessDeniedException(propertyId, null);
            if (quotaExceeded.contains(propertyId)) throw new AnalyticsQuotaExceededException(propertyId, null);
        }

        @Override
        public FetchResult<DailyMetrics> fetchDailyMetrics(String propertyId, DateRange range) {
            check(propertyId);
            dailyRanges.add(range);
            return new FetchResult<>(List.of(new DailyMetrics(range.from(), 10, 6, 3, 8, 20, 50, 1, 300)),
                    new QuotaUsage(1, 1000));
        }

        @Override
        public FetchResult<DailyBreakdown> fetchDailyBreakdown(String propertyId, DateRange range,
                                                               BreakdownDimension dimension) {
            check(propertyId);
            if (dimension == failOnDimension) throw new IllegalStateException("falha simulada em " + dimension);
            return new FetchResult<>(List.of(new DailyBreakdown(range.from(), dimension, "x", 1, 1, 1, 1, 0)),
                    new QuotaUsage(1, 1000));
        }

        @Override
        public FetchResult<PeriodUsers> fetchPeriodUsers(String propertyId, DateRange range) {
            check(propertyId);
            periodRanges.add(range);
            return new FetchResult<>(List.of(new PeriodUsers(range, 50, 48, 40)), new QuotaUsage(1, 1000));
        }
    }

    private static final class FakeMetrics implements AnalyticsMetricsRepository {
        int dailyWrites;
        int periodWrites;
        final Set<BreakdownDimension> replacedDimensions = new HashSet<>();

        @Override public void upsertDailyMetrics(Long siteId, List<DailyMetrics> m) { dailyWrites++; }
        @Override public void replaceDailyBreakdowns(Long siteId, BreakdownDimension d, DateRange r, List<DailyBreakdown> b) { replacedDimensions.add(d); }
        @Override public void upsertPeriodUsers(Long siteId, PeriodUsers u) { periodWrites++; }
        @Override public List<DailyMetrics> findDailyMetrics(Long siteId, DateRange r) { return List.of(); }
        @Override public List<BreakdownTotal> findTopBreakdowns(Long siteId, BreakdownDimension d, DateRange r, int l) { return List.of(); }
    }

    private static final class FakeSites implements TrackedSiteRepository {
        private final Map<Long, TrackedSite> all = new java.util.LinkedHashMap<>();
        final Map<Long, AnalyticsAccessStatus> status = new HashMap<>();

        void add(TrackedSite site) { all.put(site.id(), site); }

        @Override public List<TrackedSite> findAllWithAnalytics() { return List.copyOf(all.values()); }
        @Override public Optional<TrackedSite> findById(Long id) { return Optional.ofNullable(all.get(id)); }
        @Override public void markSyncSuccess(Long id) { status.put(id, AnalyticsAccessStatus.OK); }
        @Override public void markSyncFailure(Long id, AnalyticsAccessStatus s, String e) { status.put(id, s); }
    }

    /** Guarda o status final e as requisições por site (runId == siteId no fake, para simplificar). */
    private static final class FakeRunLog implements SyncRunLog {
        final Map<Long, String> status = new HashMap<>();
        final Map<Long, Integer> requests = new HashMap<>();

        @Override public Long start(Long siteId, DateRange range) { status.put(siteId, "RUNNING"); return siteId; }
        @Override public void finishSuccess(Long runId, int req, QuotaUsage q) { status.put(runId, "SUCCESS"); requests.put(runId, req); }
        @Override public void finishFailure(Long runId, int req, QuotaUsage q, String e) { status.put(runId, "FAILED"); requests.put(runId, req); }
    }
}
