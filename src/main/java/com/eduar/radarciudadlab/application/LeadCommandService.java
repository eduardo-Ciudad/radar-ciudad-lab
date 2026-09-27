package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.LeadNotFoundException;
import com.eduar.radarciudadlab.domain.model.lead.ContactNormalizer;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadDetail;
import com.eduar.radarciudadlab.domain.model.lead.LeadScorer;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.LeadUpdate;
import com.eduar.radarciudadlab.domain.model.lead.TextNormalizer;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Escritas do módulo de leads feitas pelo usuário: editar o lead e recalcular scores. */
@Service
public class LeadCommandService {

    private final LeadRepository leads;
    private final LeadScorer scorer;
    private final LeadQueryService queries;

    public LeadCommandService(LeadRepository leads, LeadScorer scorer, LeadQueryService queries) {
        this.leads = leads;
        this.scorer = scorer;
        this.queries = queries;
    }

    /**
     * Edição parcial. Mudança de status grava histórico (com a nota, se vier).
     * O score é recalculado no fim porque site e Instagram entram na fórmula.
     */
    @Transactional
    public LeadDetail update(Long id, LeadUpdate update) {
        Lead lead = leads.findById(id).orElseThrow(() -> new LeadNotFoundException(id));
        LeadStatus previousStatus = lead.status();

        if (update.status() != null) {
            lead = lead.withStatus(update.status());
        }
        if (update.notes() != null) {
            lead = lead.withNotes(TextNormalizer.blankToNull(update.notes()));
        }
        if (update.websiteUrl() != null) {
            lead = lead.withWebsiteUrl(ContactNormalizer.websiteUrl(update.websiteUrl()));
        }
        if (update.instagramHandle() != null) {
            lead = lead.withInstagramHandle(ContactNormalizer.validInstagramHandle(update.instagramHandle()));
        }
        if (update.clearScoreOverride()) {
            lead = lead.withScoreOverride(null);
        } else if (update.scoreOverride() != null) {
            if (update.scoreOverride() < 0 || update.scoreOverride() > 100) {
                throw new IllegalArgumentException("O score manual deve estar entre 0 e 100");
            }
            lead = lead.withScoreOverride(update.scoreOverride());
        }

        leads.save(lead.withScore(scorer.score(lead).total()));

        if (!Objects.equals(previousStatus, lead.status())) {
            leads.recordStatusChange(id, previousStatus, lead.status(), TextNormalizer.blankToNull(update.statusNote()));
        }
        return queries.detail(id);
    }

    /**
     * Recalcula o score de todos os leads. Use depois de mudar o nicho-alvo (radar.leads.score)
     * ou para leads importados antes do score existir. Devolve quantos mudaram.
     */
    @Transactional
    public int rescoreAll() {
        int changed = 0;
        for (Lead lead : leads.findAll()) {
            int score = scorer.score(lead).total();
            if (!Objects.equals(lead.score(), score)) {
                leads.save(lead.withScore(score));
                changed++;
            }
        }
        return changed;
    }
}
