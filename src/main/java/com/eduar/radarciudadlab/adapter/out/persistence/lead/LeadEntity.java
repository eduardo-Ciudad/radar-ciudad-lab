package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tabela lead (V1). Entidade só de persistência: a regra de negócio fica no record de domínio Lead,
 * e a conversão é feita no LeadEntityMapper. Campos package-private para o mapper ler sem getters.
 */
@Entity
@Table(name = "lead")
class LeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "dedup_key", nullable = false, unique = true)
    String dedupKey;

    @Column(nullable = false)
    String name;

    String category;

    @Column(name = "phone_e164")
    String phoneE164;

    @Column(name = "is_mobile")
    Boolean mobile;

    @Column(name = "address_raw", nullable = false)
    String addressRaw;

    String street;
    String number;
    String complement;
    String neighborhood;
    String city;
    String state;

    @Column(name = "zip_code")
    String zipCode;

    @Column(name = "address_normalized")
    String addressNormalized;

    @Column(name = "website_url")
    String websiteUrl;

    @Column(name = "instagram_handle")
    String instagramHandle;

    @Column(precision = 2, scale = 1)
    BigDecimal rating;

    @Column(name = "reviews_count")
    Integer reviewsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LeadStatus status;

    Integer score;

    @Column(name = "score_override")
    Integer scoreOverride;

    String notes;

    @Column(name = "ai_summary")
    String aiSummary;

    @Column(name = "first_batch_id")
    Long firstBatchId;

    @Column(name = "last_batch_id")
    Long lastBatchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected LeadEntity() {}

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
