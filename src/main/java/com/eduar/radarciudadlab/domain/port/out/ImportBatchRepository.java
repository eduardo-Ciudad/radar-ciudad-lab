package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository {

    Optional<ImportBatch> findByFileHash(String fileHash);

    ImportBatch create(String fileName, String fileHash, String rawContent);

    void updateCounts(Long batchId, int totalRows, int created, int updated, int skipped);

    List<ImportBatch> findRecent(int limit);
}