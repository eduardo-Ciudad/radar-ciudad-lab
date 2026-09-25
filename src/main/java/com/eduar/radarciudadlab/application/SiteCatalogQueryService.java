package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import com.eduar.radarciudadlab.domain.model.SiteInsight;
import com.eduar.radarciudadlab.domain.model.SiteOverview;
import com.eduar.radarciudadlab.domain.port.out.SiteInsightRepository;
import com.eduar.radarciudadlab.domain.port.out.SiteOverviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteCatalogQueryService {

    public static final int DEFAULT_INSIGHTS_LIMIT = 5;
    static final int MAX_INSIGHTS_LIMIT = 20;

    private final SiteOverviewRepository sites;
    private final SiteInsightRepository insights;

    public SiteCatalogQueryService(SiteOverviewRepository sites, SiteInsightRepository insights) {
        this.sites = sites;
        this.insights = insights;
    }

    public List<SiteOverview> listSites() {
        return sites.findAll();
    }

    public List<SiteInsight> latestInsights(Long siteId, int limit) {
        if (limit < 1 || limit > MAX_INSIGHTS_LIMIT) {
            throw new IllegalArgumentException("O parâmetro 'limit' deve estar entre 1 e " + MAX_INSIGHTS_LIMIT);
        }
        if (sites.findById(siteId).isEmpty()) {
            throw new SiteNotFoundException(siteId);
        }
        return insights.findLatest(siteId, limit);
    }
}
