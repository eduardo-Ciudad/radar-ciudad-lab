package com.eduar.radarciudadlab.domain.model;

import java.util.List;

public record FetchResult<T>(List<T> rows, QuotaUsage quota) {

    public FetchResult {
        rows = List.copyOf(rows);
    }
}