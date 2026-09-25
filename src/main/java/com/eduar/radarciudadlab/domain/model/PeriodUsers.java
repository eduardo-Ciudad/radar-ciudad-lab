package com.eduar.radarciudadlab.domain.model;

public record PeriodUsers(
        DateRange range,
        int totalUsers,
        int activeUsers,
        int newUsers
) {
}
