package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.application.SiteMetricsQueryService;
import com.eduar.radarciudadlab.application.SiteSyncResult;
import com.eduar.radarciudadlab.application.SyncSiteAnalyticsService;
import com.eduar.radarciudadlab.domain.model.DateRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/sites")
public class SiteAnalyticsController {

    static final int DEFAULT_DAYS = 28;
    static final long MAX_SYNC_DAYS = 92;

    private final SiteMetricsQueryService queryService;
    private final SyncSiteAnalyticsService syncService;
    private final Clock clock;

    public SiteAnalyticsController(SiteMetricsQueryService queryService,
                                   SyncSiteAnalyticsService syncService,
                                   Clock clock) {
        this.queryService = queryService;
        this.syncService = syncService;
        this.clock = clock;
    }

    @GetMapping("/{siteId}/dashboard")
    public DashboardResponse dashboard(
            @PathVariable("siteId") Long siteId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return DashboardResponse.from(queryService.getDashboard(siteId, resolveRange(from, to)));
    }


    @PostMapping("/{siteId}/sync")
    public ResponseEntity<SiteSyncResult> sync(
            @PathVariable("siteId") Long siteId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SiteSyncResult result;
        if (from == null && to == null) {
            result = syncService.syncSite(siteId);
        } else {
            DateRange range = resolveRange(from, to);
            if (range.days() > MAX_SYNC_DAYS) {
                throw new IllegalArgumentException(
                        "Backfill máximo de " + MAX_SYNC_DAYS + " dias por chamada; divida em blocos menores");
            }
            result = syncService.syncSite(siteId, range);
        }
        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY).body(result);
    }

    @PostMapping("/sync")
    public List<SiteSyncResult> syncAll() {
        return syncService.syncAll();
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock).minusDays(1);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
        return new DateRange(start, end);
    }
}