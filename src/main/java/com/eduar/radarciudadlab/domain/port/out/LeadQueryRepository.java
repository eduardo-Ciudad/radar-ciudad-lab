package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilterOptions;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadSort;
import com.eduar.radarciudadlab.domain.model.lead.RatingPoint;
import com.eduar.radarciudadlab.domain.model.lead.RelatedLead;
import com.eduar.radarciudadlab.domain.model.lead.StatusChange;

import java.util.List;

/**
 * Porta de saída só de leitura (telas do funil). Separada do LeadRepository de propósito:
 * a escrita passa pelo agregado Lead (JPA); a leitura são consultas agregadas com filtros (SQL direto).
 */
public interface LeadQueryRepository {

    /** Colunas NOVO..FECHADO com contagem e os perColumn primeiros por score; ignora filter.status. */
    LeadBoard board(LeadFilter filter, int perColumn);

    LeadPage page(LeadFilter filter, LeadSort sort, int page, int size);

    LeadFilterOptions filterOptions();

    List<RatingPoint> ratingHistory(Long leadId);

    List<StatusChange> statusHistory(Long leadId);

    List<RelatedLead> sameLocation(Long leadId);
}
