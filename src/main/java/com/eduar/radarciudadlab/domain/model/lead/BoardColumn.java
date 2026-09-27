package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/** Uma coluna do funil: total de leads no status (com os filtros) e os primeiros por score. */
public record BoardColumn(LeadStatus status, long count, List<LeadCard> leads) {}
