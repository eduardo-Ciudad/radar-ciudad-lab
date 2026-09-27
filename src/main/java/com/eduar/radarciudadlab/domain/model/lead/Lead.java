package com.eduar.radarciudadlab.domain.model.lead;


import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lead salvo. Os campos "da ferramenta" vêm do CSV e a reimportação atualiza;
 * status, score, scoreOverride, notes e aiSummary são do usuário/sistema e a importação nunca mexe.
 */
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

    /** Lead novo: aqui o status do arquivo vale. */
    public static Lead createFrom(LeadDraft draft, Long batchId) {
        PhoneNumber phone = draft.phone();
        return new Lead(null, draft.dedupKey(), draft.name(), draft.category(),
                phone == null ? null : phone.e164(), phone == null ? null : phone.mobile(),
                draft.address(), draft.websiteUrl(), draft.instagramHandle(), draft.rating(), null,
                draft.status(), null, null, null, null,
                batchId, batchId, null, null);
    }

    /**
     * Reimportação: atualiza só o que o arquivo trouxe. Campo vazio no CSV mantém o valor atual
     * (o exportador deixa Site/Instagram sempre vazios; isso não pode apagar o que você preencheu).
     */
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

    /** Score que vale para filtro e ordenação: o ajuste manual, se houver; senão o calculado. */
    public Integer effectiveScore() {
        return scoreOverride != null ? scoreOverride : score;
    }

    public Lead withScore(Integer newScore) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, websiteUrl, instagramHandle,
                rating, reviewsCount, status, newScore, scoreOverride, notes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    public Lead withStatus(LeadStatus newStatus) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, websiteUrl, instagramHandle,
                rating, reviewsCount, newStatus, score, scoreOverride, notes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    public Lead withNotes(String newNotes) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, websiteUrl, instagramHandle,
                rating, reviewsCount, status, score, scoreOverride, newNotes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    public Lead withWebsiteUrl(String newWebsiteUrl) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, newWebsiteUrl, instagramHandle,
                rating, reviewsCount, status, score, scoreOverride, notes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    public Lead withInstagramHandle(String newInstagramHandle) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, websiteUrl, newInstagramHandle,
                rating, reviewsCount, status, score, scoreOverride, notes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    public Lead withScoreOverride(Integer newScoreOverride) {
        return new Lead(id, dedupKey, name, category, phoneE164, mobile, address, websiteUrl, instagramHandle,
                rating, reviewsCount, status, score, newScoreOverride, notes, aiSummary,
                firstBatchId, lastBatchId, createdAt, updatedAt);
    }

    private static <T> T coalesce(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
