package com.eduar.radarciudadlab.domain.model.lead;

import java.time.Instant;

public record ImportBatch(
        Long id,
        String fileName,
        String fileHash,
        Instant importedAt,
        int totalRows,
        int createdCount,
        int updatedCount,
        int skippedCount
) {}