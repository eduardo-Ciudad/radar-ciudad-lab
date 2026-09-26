package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabela import_batch (V1). */
@Entity
@Table(name = "import_batch")
class ImportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "file_name", nullable = false)
    String fileName;

    @Column(name = "file_hash", nullable = false, unique = true, length = 64)
    String fileHash;

    @Column(name = "imported_at", nullable = false, updatable = false)
    Instant importedAt;

    @Column(name = "total_rows", nullable = false)
    int totalRows;

    @Column(name = "created_count", nullable = false)
    int createdCount;

    @Column(name = "updated_count", nullable = false)
    int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    int skippedCount;

    // TEXT no banco. Nas listagens ele não é carregado: as consultas usam projeção (ver SpringDataImportBatchRepository)
    @Column(name = "raw_content")
    String rawContent;

    protected ImportBatchEntity() {}

    ImportBatchEntity(String fileName, String fileHash, String rawContent) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.rawContent = rawContent;
    }

    @PrePersist
    void onCreate() {
        importedAt = Instant.now();
    }
}
