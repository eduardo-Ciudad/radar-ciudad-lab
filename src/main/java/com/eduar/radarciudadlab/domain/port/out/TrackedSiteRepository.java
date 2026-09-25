package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.TrackedSite;

import java.util.List;
import java.util.Optional;

/** Porta de saída: quais sites têm GA configurado e o status do último sync. */
public interface TrackedSiteRepository {

    /** Sites com propriedade GA cadastrada e cliente ativo (ou sites próprios). */
    List<TrackedSite> findAllWithAnalytics();

    Optional<TrackedSite> findById(Long siteId);

    void markSyncSuccess(Long siteId);

    void markSyncFailure(Long siteId, AnalyticsAccessStatus status, String error);
}
