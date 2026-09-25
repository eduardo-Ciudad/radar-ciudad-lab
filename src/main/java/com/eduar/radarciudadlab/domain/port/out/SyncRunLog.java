package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.QuotaUsage;

public interface SyncRunLog {

    Long start(Long siteId, DateRange range);

    void finishSuccess(Long runId, int requestsMade, QuotaUsage quota);

    void finishFailure(Long runId, int requestsMade, QuotaUsage quota, String error);
}
