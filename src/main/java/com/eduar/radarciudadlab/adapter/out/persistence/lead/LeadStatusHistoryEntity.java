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
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabela lead_status_history (V1). Só insert: é o log do funil. */
@Entity
@Table(name = "lead_status_history")
class LeadStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "lead_id", nullable = false)
    Long leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    LeadStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    LeadStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    Instant changedAt;

    String note;

    protected LeadStatusHistoryEntity() {}

    LeadStatusHistoryEntity(Long leadId, LeadStatus fromStatus, LeadStatus toStatus, String note) {
        this.leadId = leadId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        changedAt = Instant.now();
    }
}
