package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

public record ImportReport(
        Long batchId,
        String fileName,
        int totalRows,
        int created,
        int updated,
        int skipped,
        List<RowIssue> errors,
        List<RowIssue> warnings
) {}