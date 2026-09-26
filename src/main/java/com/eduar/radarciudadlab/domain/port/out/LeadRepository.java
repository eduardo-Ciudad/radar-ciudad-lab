package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.lead.Lead;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface LeadRepository {

    List<Lead> findByDedupKeys(Collection<String> dedupKeys);

    Lead save(Lead lead);

    void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount);
}
