package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.SiteInsight;

import java.util.List;

public interface SiteInsightRepository {

    List<SiteInsight> findLatest(Long siteId, int limit);
}
