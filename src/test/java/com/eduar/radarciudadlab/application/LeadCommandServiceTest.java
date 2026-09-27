package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.LeadNotFoundException;
import com.eduar.radarciudadlab.domain.model.lead.AddressParser;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadDetail;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilterOptions;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadScorer;
import com.eduar.radarciudadlab.domain.model.lead.LeadSort;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.LeadUpdate;
import com.eduar.radarciudadlab.domain.model.lead.RatingPoint;
import com.eduar.radarciudadlab.domain.model.lead.RelatedLead;
import com.eduar.radarciudadlab.domain.model.lead.StatusChange;
import com.eduar.radarciudadlab.domain.port.out.LeadQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Edição do lead com fakes em memória: sem banco, sem Spring. */
class LeadCommandServiceTest {

    private final InMemoryLeadRepository leads = new InMemoryLeadRepository();
    private final LeadScorer scorer = new LeadScorer(Set.of("pilates"));
    private final LeadCommandService service =
            new LeadCommandService(leads, scorer, new LeadQueryService(new EmptyQueries(), leads, scorer));

    private Long id;

    @BeforeEach
    void criaLead() {
        Lead lead = new Lead(null, "cordial pro pilates|+5517997726959", "Cordial Pro Pilates", "Policlínica",
                "+5517997726959", true,
                AddressParser.parse("R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080"),
                null, null, new BigDecimal("5.0"), null, LeadStatus.NOVO, 85, null, null, null, 1L, 1L, null, null);
        id = leads.save(lead).id();
    }

    @Test
    void mudarStatusGravaHistoricoComANota() {
        LeadDetail detail = service.update(id, edit(LeadStatus.CONTATADO, "mandei WhatsApp", null, null, null));

        assertEquals(LeadStatus.CONTATADO, detail.lead().status());
        assertEquals(1, leads.statusChanges.size());
        assertEquals(LeadStatus.NOVO, leads.statusChanges.get(0).from());
        assertEquals("mandei WhatsApp", leads.statusChanges.get(0).note());
    }

    @Test
    void mesmoStatusNaoGeraHistorico() {
        service.update(id, edit(LeadStatus.NOVO, null, "observação", null, null));

        assertEquals(0, leads.statusChanges.size());
        assertEquals("observação", leads.byId.get(id).notes());
    }

    @Test
    void textoVazioApagaCampo() {
        service.update(id, edit(null, null, "algo", null, null));
        service.update(id, edit(null, null, "", null, null));

        assertNull(leads.byId.get(id).notes());
    }

    @Test
    void cadastrarSiteNormalizaURLERecalculaScore() {
        service.update(id, edit(null, null, null, "cordialpro.com.br", null));

        Lead lead = leads.byId.get(id);
        assertEquals("https://cordialpro.com.br", lead.websiteUrl());
        assertEquals(75, lead.score());                                  // sem site 15 -> com site 5
    }

    @Test
    void siteInvalidoERecusado() {
        assertThrows(IllegalArgumentException.class, () -> service.update(id, edit(null, null, null, "não sei", null)));
    }

    @Test
    void instagramAceitaURLDoPerfil() {
        service.update(id, edit(null, null, null, null, "https://www.instagram.com/Cordial.Pro/"));

        assertEquals("cordial.pro", leads.byId.get(id).instagramHandle());
    }

    @Test
    void scoreManualSubstituiOCalculadoEPodeSerDesfeito() {
        service.update(id, new LeadUpdate(null, null, null, null, null, 40, false));
        assertEquals(40, leads.byId.get(id).effectiveScore());

        service.update(id, new LeadUpdate(null, null, null, null, null, null, true));
        assertEquals(85, leads.byId.get(id).effectiveScore());
    }

    @Test
    void leadInexistenteDa404() {
        assertThrows(LeadNotFoundException.class, () -> service.update(999L, edit(LeadStatus.CONTATADO, null, null, null, null)));
    }

    @Test
    void rescoreSoContaOsQueMudaram() {
        Lead lead = leads.byId.get(id);
        leads.byId.put(id, lead.withScore(null));                          // importado antes do score existir

        assertEquals(1, service.rescoreAll());
        assertEquals(85, leads.byId.get(id).score());
        assertEquals(0, service.rescoreAll());
    }

    private static LeadUpdate edit(LeadStatus status, String statusNote, String notes, String site, String instagram) {
        return new LeadUpdate(status, statusNote, notes, site, instagram, null, false);
    }

    private static class EmptyQueries implements LeadQueryRepository {
        @Override public LeadBoard board(LeadFilter filter, int perColumn) { return new LeadBoard(List.of(), 0, 0, 0, 0); }
        @Override public LeadPage page(LeadFilter filter, LeadSort sort, int page, int size) { return new LeadPage(List.of(), page, size, 0, 0); }
        @Override public LeadFilterOptions filterOptions() { return new LeadFilterOptions(List.of(), List.of()); }
        @Override public List<RatingPoint> ratingHistory(Long leadId) { return List.of(); }
        @Override public List<StatusChange> statusHistory(Long leadId) { return List.of(); }
        @Override public List<RelatedLead> sameLocation(Long leadId) { return List.of(); }
    }
}
