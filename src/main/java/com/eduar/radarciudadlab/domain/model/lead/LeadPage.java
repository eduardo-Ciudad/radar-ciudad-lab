package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/** Página da listagem. page começa em 0. */
public record LeadPage(List<LeadCard> items, int page, int size, long totalElements, int totalPages) {}
