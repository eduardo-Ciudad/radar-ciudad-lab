package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * As leituras usam projeção direto no record de domínio (sem raw_content):
 * o CSV original pode ter centenas de KB e não precisa sair do banco para listar importações.
 */
interface SpringDataImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    String SUMMARY = """
            select new com.eduar.radarciudadlab.domain.model.lead.ImportBatch(
                b.id, b.fileName, b.fileHash, b.importedAt, b.totalRows, b.createdCount, b.updatedCount, b.skippedCount)
            from ImportBatchEntity b
            """;

    @Query(SUMMARY + " where b.fileHash = :fileHash")
    Optional<ImportBatch> findSummaryByFileHash(String fileHash);

    @Query(SUMMARY + " order by b.importedAt desc, b.id desc")
    List<ImportBatch> findRecentSummaries(Pageable pageable);
}
