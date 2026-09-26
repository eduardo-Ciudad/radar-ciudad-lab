package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import com.eduar.radarciudadlab.domain.port.out.ImportBatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapter JPA da porta ImportBatchRepository. */
@Repository
public class JpaImportBatchRepository implements ImportBatchRepository {

    private final SpringDataImportBatchRepository batches;

    public JpaImportBatchRepository(SpringDataImportBatchRepository batches) {
        this.batches = batches;
    }

    @Override
    public Optional<ImportBatch> findByFileHash(String fileHash) {
        return batches.findSummaryByFileHash(fileHash);
    }

    @Override
    public ImportBatch create(String fileName, String fileHash, String rawContent) {
        ImportBatchEntity e = batches.save(new ImportBatchEntity(fileName, fileHash, rawContent));
        return new ImportBatch(e.id, e.fileName, e.fileHash, e.importedAt, 0, 0, 0, 0);
    }

    @Override
    public void updateCounts(Long batchId, int totalRows, int created, int updated, int skipped) {
        ImportBatchEntity e = batches.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Importação " + batchId + " não encontrada"));
        e.totalRows = totalRows;
        e.createdCount = created;
        e.updatedCount = updated;
        e.skippedCount = skipped;
    }

    @Override
    public List<ImportBatch> findRecent(int limit) {
        return batches.findRecentSummaries(PageRequest.of(0, limit));
    }
}
