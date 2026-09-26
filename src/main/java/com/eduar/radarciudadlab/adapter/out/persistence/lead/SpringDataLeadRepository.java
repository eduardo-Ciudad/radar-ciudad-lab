package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface SpringDataLeadRepository extends JpaRepository<LeadEntity, Long> {

    List<LeadEntity> findByDedupKeyIn(Collection<String> dedupKeys);
}
