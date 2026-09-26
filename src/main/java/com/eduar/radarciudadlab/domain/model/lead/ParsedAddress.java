package com.eduar.radarciudadlab.domain.model.lead;


public record ParsedAddress(
        String raw,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        String normalized
) {

    public boolean isStructured() {
        return street != null;
    }
}