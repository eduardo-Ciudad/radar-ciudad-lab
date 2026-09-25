package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.application.SiteCatalogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteCatalogController {

    private final SiteCatalogQueryService queryService;

    public SiteCatalogController(SiteCatalogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<SiteSummaryResponse> list() {
        return queryService.listSites().stream().map(SiteSummaryResponse::from).toList();
    }

    @GetMapping("/{siteId}/insights")
    public List<SiteInsightResponse> insights(
            @PathVariable("siteId") Long siteId,
            @RequestParam(name = "limit", defaultValue = "" + SiteCatalogQueryService.DEFAULT_INSIGHTS_LIMIT) int limit) {

        return queryService.latestInsights(siteId, limit).stream().map(SiteInsightResponse::from).toList();
    }
}