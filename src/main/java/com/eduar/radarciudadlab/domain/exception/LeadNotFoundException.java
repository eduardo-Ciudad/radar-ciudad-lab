package com.eduar.radarciudadlab.domain.exception;

public class LeadNotFoundException extends RuntimeException {
    public LeadNotFoundException(Long leadId) {
        super("Lead " + leadId + " não encontrado");
    }
}
