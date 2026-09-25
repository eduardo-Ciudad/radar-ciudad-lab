package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.SiteOverview;

import java.util.List;
import java.util.Optional;

public interface SiteOverviewRepository {

    List<SiteOverview> findAll();

    Optional<SiteOverview> findById(Long siteId);
}