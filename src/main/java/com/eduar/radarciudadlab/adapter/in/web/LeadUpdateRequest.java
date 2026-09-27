package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.LeadUpdate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PATCH /api/leads/{id}. Tudo opcional: campo ausente ou null não muda nada.
 * Para apagar um texto, mande "". Para voltar ao score calculado, mande "clearScoreOverride": true.
 */
public record LeadUpdateRequest(
        LeadStatus status,
        @Size(max = 1000) String statusNote,
        @Size(max = 5000) String notes,
        @Size(max = 500) String websiteUrl,
        @Size(max = 200) String instagramHandle,
        @Min(0) @Max(100) Integer scoreOverride,
        Boolean clearScoreOverride
) {

    LeadUpdate toDomain() {
        return new LeadUpdate(status, statusNote, notes, websiteUrl, instagramHandle, scoreOverride,
                Boolean.TRUE.equals(clearScoreOverride));
    }
}
