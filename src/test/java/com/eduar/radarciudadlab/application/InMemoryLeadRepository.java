package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.StatusChange;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** LeadRepository em memória para os testes unitários dos serviços. */
class InMemoryLeadRepository implements LeadRepository {

    final Map<Long, Lead> byId = new LinkedHashMap<>();
    final List<StatusChange> statusChanges = new ArrayList<>();
    int snapshots;
    private long nextId = 1;

    @Override
    public List<Lead> findByDedupKeys(Collection<String> keys) {
        return byId.values().stream().filter(l -> keys.contains(l.dedupKey())).toList();
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Lead> findAll() {
        return byId.values().stream().sorted(Comparator.comparing(Lead::id)).toList();
    }

    @Override
    public Lead save(Lead l) {
        Long id = l.id() != null ? l.id() : nextId++;
        Instant now = Instant.now();
        Lead saved = new Lead(id, l.dedupKey(), l.name(), l.category(), l.phoneE164(), l.mobile(), l.address(),
                l.websiteUrl(), l.instagramHandle(), l.rating(), l.reviewsCount(), l.status(), l.score(),
                l.scoreOverride(), l.notes(), l.aiSummary(), l.firstBatchId(), l.lastBatchId(),
                Objects.requireNonNullElse(l.createdAt(), now), now);
        byId.put(id, saved);
        return saved;
    }

    @Override
    public void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount) {
        snapshots++;
    }

    @Override
    public void recordStatusChange(Long leadId, LeadStatus from, LeadStatus to, String note) {
        statusChanges.add(new StatusChange(from, to, Instant.now(), note));
    }

    Lead byName(String name) {
        return byId.values().stream().filter(l -> l.name().equals(name)).findFirst().orElseThrow();
    }
}
