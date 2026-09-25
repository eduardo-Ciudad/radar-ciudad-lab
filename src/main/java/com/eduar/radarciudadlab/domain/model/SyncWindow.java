package com.eduar.radarciudadlab.domain.model;

import java.time.LocalDate;

public final class SyncWindow {

    public static final int REPROCESS_DAYS = 3;

    private SyncWindow() {}

    public static DateRange daily(LocalDate today) {
        return new DateRange(today.minusDays(REPROCESS_DAYS), today.minusDays(1));
    }

    public static DateRange currentMonth(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        return new DateRange(yesterday.withDayOfMonth(1), yesterday);
    }
}
