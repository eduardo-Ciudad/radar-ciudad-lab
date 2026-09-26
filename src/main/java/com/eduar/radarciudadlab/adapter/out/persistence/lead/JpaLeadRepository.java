package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Adapter JPA da porta LeadRepository. */
@Repository
public class JpaLeadRepository implements LeadRepository {

    private final SpringDataLeadRepository leads;
    private final SpringDataLeadSnapshotRepository snapshots;

    public JpaLeadRepository(SpringDataLeadRepository leads, SpringDataLeadSnapshotRepository snapshots) {
        this.leads = leads;
        this.snapshots = snapshots;
    }

    @Override
    public List<Lead> findByDedupKeys(Collection<String> dedupKeys) {
        if (dedupKeys.isEmpty()) {
            return List.of();
        }
        return leads.findByDedupKeyIn(dedupKeys).stream().map(LeadEntityMapper::toDomain).toList();
    }

    @Override
    public Lead save(Lead lead) {
        // Update: dentro da transação a entidade já está no contexto de persistência (veio do findByDedupKeys),
        // então o findById não vai ao banco de novo e o dirty checking gera só o UPDATE.
        LeadEntity entity = lead.id() == null
                ? new LeadEntity()
                : leads.findById(lead.id()).orElseThrow(
                        () -> new IllegalStateException("Lead " + lead.id() + " não existe mais"));
        LeadEntityMapper.copy(lead, entity);
        return LeadEntityMapper.toDomain(leads.save(entity));
    }

    @Override
    public void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount) {
        snapshots.save(new LeadSnapshotEntity(leadId, batchId, rating, reviewsCount));
    }
}
