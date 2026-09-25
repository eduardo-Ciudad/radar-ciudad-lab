package com.eduar.radarciudadlab.application;



import com.eduar.radarciudadlab.domain.exception.AnalyticsAccessDeniedException;
import com.eduar.radarciudadlab.domain.exception.AnalyticsQuotaExceededException;
import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.FetchResult;
import com.eduar.radarciudadlab.domain.model.PeriodUsers;
import com.eduar.radarciudadlab.domain.model.QuotaUsage;
import com.eduar.radarciudadlab.domain.model.SyncWindow;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsProvider;
import com.eduar.radarciudadlab.domain.port.out.SyncRunLog;
import com.eduar.radarciudadlab.domain.port.out.TrackedSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
public class SyncSiteAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(SyncSiteAnalyticsService.class);

    private final AnalyticsProvider provider;
    private final AnalyticsMetricsRepository metrics;
    private final TrackedSiteRepository sites;
    private final SyncRunLog runLog;
    private final Clock clock;

    public SyncSiteAnalyticsService(AnalyticsProvider provider,
                                    AnalyticsMetricsRepository metrics,
                                    TrackedSiteRepository sites,
                                    SyncRunLog runLog,
                                    Clock clock) {
        this.provider = provider;
        this.metrics = metrics;
        this.sites = sites;
        this.runLog = runLog;
        this.clock = clock;
    }

    public List<SiteSyncResult> syncAll() {
        DateRange window = SyncWindow.daily(today());
        List<SiteSyncResult> results = new ArrayList<>();

        for (TrackedSite site : sites.findAllWithAnalytics()) {
            try {
                results.add(sync(site, window));
            } catch (RuntimeException e) {

                log.error("Erro inesperado no sync do site {} ({})", site.name(), site.id(), e);
                results.add(SiteSyncResult.failure(site, AnalyticsAccessStatus.ERROR, 0, QuotaUsage.NONE, describe(e)));
            }
        }

        long ok = results.stream().filter(SiteSyncResult::success).count();
        log.info("Sync GA concluído: {}/{} sites OK ({} a {})", ok, results.size(), window.from(), window.to());
        return results;
    }

    public SiteSyncResult syncSite(Long siteId) {
        return syncSite(siteId, SyncWindow.daily(today()));
    }

    public SiteSyncResult syncSite(Long siteId, DateRange range) {
        TrackedSite site = sites.findById(siteId).orElseThrow(() -> new SiteNotFoundException(siteId));
        return sync(site, range);
    }

    private SiteSyncResult sync(TrackedSite site, DateRange range) {
        Long runId = runLog.start(site.id(), range);
        Progress progress = new Progress();
        String propertyId = site.gaPropertyId();

        try {
            metrics.upsertDailyMetrics(site.id(), progress.track(provider.fetchDailyMetrics(propertyId, range)));

            for (BreakdownDimension dimension : BreakdownDimension.values()) {
                List<DailyBreakdown> rows = progress.track(provider.fetchDailyBreakdown(propertyId, range, dimension));
                metrics.replaceDailyBreakdowns(site.id(), dimension, range, rows);
            }

            DateRange month = SyncWindow.currentMonth(today());
            for (PeriodUsers users : progress.track(provider.fetchPeriodUsers(propertyId, month))) {
                metrics.upsertPeriodUsers(site.id(), users);
            }

            sites.markSyncSuccess(site.id());
            runLog.finishSuccess(runId, progress.requests, progress.quota);
            log.info("Sync GA OK: {} ({} requisições, {} tokens)",
                    site.name(), progress.requests, progress.quota.tokensConsumed());
            return SiteSyncResult.success(site, progress.requests, progress.quota);

        } catch (AnalyticsAccessDeniedException e) {
            return fail(site, runId, progress, AnalyticsAccessStatus.NO_ACCESS, e);
        } catch (AnalyticsQuotaExceededException e) {
            return fail(site, runId, progress, AnalyticsAccessStatus.ERROR, e);
        } catch (RuntimeException e) {
            return fail(site, runId, progress, AnalyticsAccessStatus.ERROR, e);
        }
    }

    private SiteSyncResult fail(TrackedSite site, Long runId, Progress progress,
                                AnalyticsAccessStatus status, RuntimeException e) {
        String error = describe(e);
        log.warn("Sync GA falhou: {} -> {} ({})", site.name(), status, error);
        sites.markSyncFailure(site.id(), status, error);
        runLog.finishFailure(runId, progress.requests, progress.quota, error);
        return SiteSyncResult.failure(site, status, progress.requests, progress.quota, error);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String describe(Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private static final class Progress {
        private int requests;
        private QuotaUsage quota = QuotaUsage.NONE;

        <T> List<T> track(FetchResult<T> result) {
            requests++;
            quota = quota.plus(result.quota());
            return result.rows();
        }
    }
}

