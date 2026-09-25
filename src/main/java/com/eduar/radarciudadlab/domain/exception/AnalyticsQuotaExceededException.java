package com.eduar.radarciudadlab.domain.exception;

public class AnalyticsQuotaExceededException extends RuntimeException {
    public AnalyticsQuotaExceededException(String propertyId, Throwable cause) {
        super("Cota do GA esgotada para a propriedade " + propertyId, cause);
    }
}