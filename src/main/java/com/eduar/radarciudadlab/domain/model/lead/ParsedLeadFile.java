package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

public record ParsedLeadFile(String content, List<RawLeadRow> rows) {}
