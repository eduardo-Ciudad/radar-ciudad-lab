package com.eduar.radarciudadlab.domain.model.lead;

/** Outro lead no mesmo telefone ou endereço (view lead_shared_location). matchType = PHONE ou ADDRESS. */
public record RelatedLead(Long id, String name, LeadStatus status, String matchType) {}
