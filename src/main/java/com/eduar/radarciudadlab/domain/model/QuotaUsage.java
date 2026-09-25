package com.eduar.radarciudadlab.domain.model;

public record QuotaUsage(int tokensConsumed, int tokensRemainingToday) {

    public static final QuotaUsage NONE = new QuotaUsage(0, 0);

    public QuotaUsage plus(QuotaUsage other) {
        return new QuotaUsage(tokensConsumed + other.tokensConsumed, other.tokensRemainingToday);
    }
}
