package com.eduar.radarciudadlab.domain.exception;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(Long siteId) {
        super("Site " + siteId + " não encontrado ou sem Google Analytics configurado");
    }
}
