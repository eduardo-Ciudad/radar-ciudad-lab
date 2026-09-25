package com.eduar.radarciudadlab.adapter.in.scheduler;


import com.eduar.radarciudadlab.application.SiteSyncResult;
import com.eduar.radarciudadlab.application.SyncSiteAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "radar.analytics.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsSyncScheduler.class);

    private final SyncSiteAnalyticsService syncService;

    public AnalyticsSyncScheduler(SyncSiteAnalyticsService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${radar.analytics.sync.cron:0 0 6 * * *}", zone = "${radar.timezone:America/Sao_Paulo}")
    public void dailySync() {
        log.info("Iniciando sync diário do Google Analytics");
        List<SiteSyncResult> results = syncService.syncAll();

        results.stream()
                .filter(r -> !r.success())
                .forEach(r -> log.warn("Site '{}' ({}) ficou com status {}: {}",
                        r.siteName(), r.siteId(), r.status(), r.error()));
    }
}
