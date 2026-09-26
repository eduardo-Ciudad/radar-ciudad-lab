package com.eduar.radarciudadlab.domain.model.lead;

public record RawLeadRow(
        int line,
        String name,
        String category,
        String phone,
        String address,
        String website,
        String instagram,
        String rating,
        String status
) {}
