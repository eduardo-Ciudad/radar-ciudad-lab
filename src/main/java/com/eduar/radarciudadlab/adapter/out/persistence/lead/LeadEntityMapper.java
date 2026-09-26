package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.ParsedAddress;

/** Conversão entre o record de domínio e a entidade JPA. */
final class LeadEntityMapper {

    private LeadEntityMapper() {}

    static Lead toDomain(LeadEntity e) {
        ParsedAddress address = new ParsedAddress(e.addressRaw, e.street, e.number, e.complement,
                e.neighborhood, e.city, e.state, e.zipCode, e.addressNormalized);
        return new Lead(e.id, e.dedupKey, e.name, e.category, e.phoneE164, e.mobile, address,
                e.websiteUrl, e.instagramHandle, e.rating, e.reviewsCount, e.status, e.score, e.scoreOverride,
                e.notes, e.aiSummary, e.firstBatchId, e.lastBatchId, e.createdAt, e.updatedAt);
    }

    /**
     * Copia o domínio para a entidade. id, createdAt e updatedAt não são copiados:
     * id vem do banco e as datas são controladas por @PrePersist/@PreUpdate.
     */
    static void copy(Lead lead, LeadEntity e) {
        ParsedAddress address = lead.address();
        e.dedupKey = lead.dedupKey();
        e.name = lead.name();
        e.category = lead.category();
        e.phoneE164 = lead.phoneE164();
        e.mobile = lead.mobile();
        e.addressRaw = address.raw();
        e.street = address.street();
        e.number = address.number();
        e.complement = address.complement();
        e.neighborhood = address.neighborhood();
        e.city = address.city();
        e.state = address.state();
        e.zipCode = address.zipCode();
        e.addressNormalized = address.normalized();
        e.websiteUrl = lead.websiteUrl();
        e.instagramHandle = lead.instagramHandle();
        e.rating = lead.rating();
        e.reviewsCount = lead.reviewsCount();
        e.status = lead.status();
        e.score = lead.score();
        e.scoreOverride = lead.scoreOverride();
        e.notes = lead.notes();
        e.aiSummary = lead.aiSummary();
        e.firstBatchId = lead.firstBatchId();
        e.lastBatchId = lead.lastBatchId();
    }
}
