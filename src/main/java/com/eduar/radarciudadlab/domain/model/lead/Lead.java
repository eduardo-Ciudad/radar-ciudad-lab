package com.eduar.radarciudadlab.domain.model.lead;

import com.google.type.PhoneNumber;

import java.math.BigDecimal;
import java.time.Instant;


public record Lead(
        Long id,
        String dedupKey,
        String name,
        String category,
        String phoneE164,
        Boolean mobile,
        ParsedAddress address,
        String websiteUrl,
        String instagramHandle,
        BigDecimal rating,
        Integer reviewsCount,
        LeadStatus status,
        Integer score,
        Integer scoreOverride,
        String notes,
        String aiSummary,
        Long firstBatchId,
        Long lastBatchId,
        Instant createdAt,
        Instant updatedAt
) {

    public static Lead createFrom(LeadDraft draft, Long batchId) {
        PhoneNumber phone = draft.phone();
        return new Lead(null, draft.dedupKey(), draft.name(), draft.category(),
                phone == null ? null : phone.e164(), phone == null ? null : phone.mobile(),
                draft.address(), draft.websiteUrl(), draft.instagramHandle(), draft.rating(), null,
                draft.status(), null, null, null, null,
                batchId, batchId, null, null);
    }


    public Lead refreshFrom(LeadDraft draft, Long batchId) {
        PhoneNumber phone = draft.phone();
        return new Lead(id, dedupKey, draft.name(),
                coalesce(draft.category(), category),
                phone == null ? phoneE164 : phone.e164(),
                phone == null ? mobile : Boolean.valueOf(phone.mobile()),
                draft.address(),
                coalesce(draft.websiteUrl(), websiteUrl),
                coalesce(draft.instagramHandle(), instagramHandle),
                coalesce(draft.rating(), rating),
                reviewsCount,
                status, score, scoreOverride, notes, aiSummary,
                firstBatchId, batchId, createdAt, updatedAt);
    }

    private static <T> T coalesce(T value, T fallback) {
        return value != null ? value : fallback;
    }
}