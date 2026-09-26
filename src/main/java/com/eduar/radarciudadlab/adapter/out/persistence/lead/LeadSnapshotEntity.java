package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Tabela lead_snapshot (V1): nota do lead em cada importação. Só insert, nunca update. */
@Entity
@Table(name = "lead_snapshot")
class LeadSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "lead_id", nullable = false)
    Long leadId;

    @Column(name = "batch_id", nullable = false)
    Long batchId;

    @Column(precision = 2, scale = 1)
    BigDecimal rating;

    @Column(name = "reviews_count")
    Integer reviewsCount;

    @Column(name = "captured_at", nullable = false, updatable = false)
    Instant capturedAt;

    protected LeadSnapshotEntity() {}

    LeadSnapshotEntity(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount) {
        this.leadId = leadId;
        this.batchId = batchId;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
    }

    @PrePersist
    void onCreate() {
        capturedAt = Instant.now();
    }
}
