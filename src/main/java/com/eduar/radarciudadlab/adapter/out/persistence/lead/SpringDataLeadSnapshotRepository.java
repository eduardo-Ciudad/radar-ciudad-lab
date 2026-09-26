package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLeadSnapshotRepository extends JpaRepository<LeadSnapshotEntity, Long> {}
