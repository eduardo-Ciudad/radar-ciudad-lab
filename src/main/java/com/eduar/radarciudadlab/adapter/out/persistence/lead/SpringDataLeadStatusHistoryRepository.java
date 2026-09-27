package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLeadStatusHistoryRepository extends JpaRepository<LeadStatusHistoryEntity, Long> {}
