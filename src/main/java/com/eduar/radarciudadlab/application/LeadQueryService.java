package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.LeadNotFoundException;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadDetail;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilterOptions;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadScorer;
import com.eduar.radarciudadlab.domain.model.lead.LeadSort;
import com.eduar.radarciudadlab.domain.port.out.LeadQueryRepository;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leituras do módulo de leads: funil, listagem, opções de filtro e detalhe. */
@Service
public class LeadQueryService {

    public static final int DEFAULT_PER_COLUMN = 20;
    static final int MAX_PER_COLUMN = 100;
    static final int MAX_PAGE_SIZE = 100;

    private final LeadQueryRepository queries;
    private final LeadRepository leads;
    private final LeadScorer scorer;

    public LeadQueryService(LeadQueryRepository queries, LeadRepository leads, LeadScorer scorer) {
        this.queries = queries;
        this.leads = leads;
        this.scorer = scorer;
    }

    @Transactional(readOnly = true)
    public LeadBoard board(LeadFilter filter, int perColumn) {
        if (perColumn < 1 || perColumn > MAX_PER_COLUMN) {
            throw new IllegalArgumentException("O parâmetro 'perColumn' deve estar entre 1 e " + MAX_PER_COLUMN);
        }
        validate(filter);
        return queries.board(filter, perColumn);
    }

    @Transactional(readOnly = true)
    public LeadPage page(LeadFilter filter, String sort, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("O parâmetro 'page' começa em 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("O parâmetro 'size' deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        validate(filter);
        return queries.page(filter, LeadSort.from(sort), page, size);
    }

    @Transactional(readOnly = true)
    public LeadFilterOptions filterOptions() {
        return queries.filterOptions();
    }

    @Transactional(readOnly = true)
    public LeadDetail detail(Long id) {
        Lead lead = leads.findById(id).orElseThrow(() -> new LeadNotFoundException(id));
        return new LeadDetail(lead, scorer.score(lead),
                queries.ratingHistory(id), queries.statusHistory(id), queries.sameLocation(id));
    }

    private static void validate(LeadFilter filter) {
        if (filter.minScore() != null && (filter.minScore() < 0 || filter.minScore() > 100)) {
            throw new IllegalArgumentException("O parâmetro 'minScore' deve estar entre 0 e 100");
        }
    }
}
