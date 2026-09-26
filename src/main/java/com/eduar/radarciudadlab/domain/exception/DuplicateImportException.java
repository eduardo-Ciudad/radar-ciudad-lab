package com.eduar.radarciudadlab.domain.exception;

import java.time.Instant;

public class DuplicateImportException extends RuntimeException {

    private final Long batchId;

    public DuplicateImportException(Long batchId, Instant importedAt) {
        super("Este arquivo já foi importado (importação " + batchId + " em " + importedAt + ")");
        this.batchId = batchId;
    }

    public Long getBatchId() {
        return batchId;
    }
}
