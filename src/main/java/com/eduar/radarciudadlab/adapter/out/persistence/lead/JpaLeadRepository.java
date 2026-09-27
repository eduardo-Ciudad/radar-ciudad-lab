package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JPA da porta LeadRepository.
 * save e recordStatusChange fazem flush: as telas leem pelo JDBC (JdbcLeadQueryRepository) na mesma
 * transação, e o JDBC só enxerga o que já foi enviado ao banco.
 */
@Repository
public class JpaLeadRepository implements LeadRepository {

    private final SpringDataLeadRepository leads;
    private final SpringDataLeadSnapshotRepository snapshots;
    private final SpringDataLeadStatusHistoryRepository statusHistory;

    public JpaLeadRepository(SpringDataLeadRepository leads, SpringDataLeadSnapshotRepository snapshots,
                             SpringDataLeadStatusHistoryRepository statusHistory) {
        this.leads = leads;
        this.snapshots = snapshots;
        this.statusHistory = statusHistory;
    }

    @Override
    public List<Lead> findByDedupKeys(Collection<String> dedupKeys) {
        if (dedupKeys.isEmpty()) {
            return List.of();
        }
        return leads.findByDedupKeyIn(dedupKeys).stream().map(LeadEntityMapper::toDomain).toList();
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return leads.findById(id).map(LeadEntityMapper::toDomain);
    }

    @Override
    public List<Lead> findAll() {
        return leads.findAll(Sort.by("id")).stream().map(LeadEntityMapper::toDomain).toList();
    }

    @Override
    public Lead save(Lead lead) {
        // Update: dentro da transação a entidade já está no contexto de persistência (veio do find),
        // então o findById não vai ao banco de novo e o dirty checking gera só o UPDATE.
        LeadEntity entity = lead.id() == null
                ? new LeadEntity()
                : leads.findById(lead.id()).orElseThrow(
                        () -> new IllegalStateException("Lead " + lead.id() + " não existe mais"));
        LeadEntityMapper.copy(lead, entity);
        return LeadEntityMapper.toDomain(leads.saveAndFlush(entity));
    }

    @Override
    public void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount) {
        snapshots.save(new LeadSnapshotEntity(leadId, batchId, rating, reviewsCount));
    }

    @Override
    public void recordStatusChange(Long leadId, LeadStatus from, LeadStatus to, String note) {
        statusHistory.saveAndFlush(new LeadStatusHistoryEntity(leadId, from, to, note));
    }
}
