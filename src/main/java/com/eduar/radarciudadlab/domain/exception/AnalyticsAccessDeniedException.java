package com.eduar.radarciudadlab.domain.exception;

public class AnalyticsAccessDeniedException extends RuntimeException {
    public AnalyticsAccessDeniedException(String propertyId, Throwable cause) {
        super("Sem acesso à propriedade GA " + propertyId, cause);
    }
}
