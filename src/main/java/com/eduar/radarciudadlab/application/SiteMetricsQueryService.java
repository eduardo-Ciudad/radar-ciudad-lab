package com.eduar.radarciudadlab.application;



import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.MetricsSummary;
import com.eduar.radarciudadlab.domain.model.PeriodComparison;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import com.eduar.radarciudadlab.domain.port.out.TrackedSiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteMetricsQueryService {

    static final int TOP_LIMIT = 5;
    static final int EVENTS_LIMIT = 10;
    static final long MAX_RANGE_DAYS = 366;

    private final AnalyticsMetricsRepository metrics;
    private final TrackedSiteRepository sites;

    public SiteMetricsQueryService(AnalyticsMetricsRepository metrics, TrackedSiteRepository sites) {
        this.metrics = metrics;
        this.sites = sites;
    }

    public SiteDashboard getDashboard(Long siteId, DateRange range) {
        if (range.days() > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Intervalo máximo é de " + MAX_RANGE_DAYS + " dias");
        }
        TrackedSite site = sites.findById(siteId).orElseThrow(() -> new SiteNotFoundException(siteId));

        List<DailyMetrics> daily = metrics.findDailyMetrics(siteId, range);
        MetricsSummary current = MetricsSummary.of(range, daily);

        DateRange previousRange = range.previous();
        MetricsSummary previous = MetricsSummary.of(previousRange, metrics.findDailyMetrics(siteId, previousRange));

        return new SiteDashboard(
                site,
                range,
                current,
                previous,
                PeriodComparison.between(current, previous),
                daily,
                metrics.findTopBreakdowns(siteId, BreakdownDimension.CHANNEL, range, TOP_LIMIT),
                metrics.findTopBreakdowns(siteId, BreakdownDimension.LANDING_PAGE, range, TOP_LIMIT),
                metrics.findTopBreakdowns(siteId, BreakdownDimension.DEVICE, range, TOP_LIMIT),
                metrics.findTopBreakdowns(siteId, BreakdownDimension.EVENT, range, EVENTS_LIMIT));
    }
}
