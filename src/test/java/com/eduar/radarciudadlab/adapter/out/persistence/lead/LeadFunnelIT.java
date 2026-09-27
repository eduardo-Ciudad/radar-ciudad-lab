package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.application.ImportLeadsService;
import com.eduar.radarciudadlab.application.LeadCommandService;
import com.eduar.radarciudadlab.application.LeadQueryService;
import com.eduar.radarciudadlab.domain.model.lead.BoardColumn;
import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadCard;
import com.eduar.radarciudadlab.domain.model.lead.LeadDetail;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.LeadUpdate;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Funil de ponta a ponta contra o PostgreSQL local: importa 3 leads com um sufixo aleatório no nome
 * e filtra tudo por esse sufixo (q=tag), para não depender dos leads reais que já estão no banco.
 * @Transactional desfaz tudo no fim.
 */
@SpringBootTest
@Transactional
class LeadFunnelIT {

    @Autowired ImportLeadsService importer;
    @Autowired LeadQueryService queries;
    @Autowired LeadCommandService commands;
    @Autowired EntityManager entityManager;

    private String tag;

    @BeforeEach
    void importaLeadsDeTeste() {
        tag = "it" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String address = "\"R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080\"";
        String csv = "Nome,Categoria,Telefone,Endereço,Site,Instagram,Avaliação,Status\n"
                + "\"Cordial Pilates " + tag + "\",\"Policlínica\",\"+55 17 99772-6959\"," + address + ",\"\",\"\",\"5\",\"novo\"\n"
                + "\"Leonardo Pires " + tag + "\",\"Policlínica\",\"+55 17 99772-6959\"," + address + ",\"\",\"\",\"4.2\",\"novo\"\n"
                + "\"Atlas Fisioterapia " + tag + "\",\"Fisioterapeuta\",\"\","
                + "\"R. Pernambuco, 2816 - Vila Redentora, São José do Rio Preto - SP, 15015-770\",\"\",\"\",\"5\",\"novo\"\n";
        importer.importFile(tag + ".csv", csv.getBytes(StandardCharsets.UTF_8));
        flushAndClear();
    }

    @Test
    void funilMostraColunasComContagemEOrdemPorScore() {
        LeadBoard board = queries.board(byTag(), 20);

        assertEquals(5, board.columns().size());
        BoardColumn novo = board.columns().get(0);
        assertEquals(LeadStatus.NOVO, novo.status());
        assertEquals(3, novo.count());
        assertEquals(3, board.active());
        assertEquals(0, board.outOfFunnel());
        assertTrue(novo.leads().get(0).name().startsWith("Cordial"));   // maior score primeiro
        assertTrue(novo.leads().stream().allMatch(c -> c.score() != null));
    }

    @Test
    void filtroDeCelularEBuscaSemAcento() {
        LeadPage mobiles = queries.page(new LeadFilter(null, null, null, null, true, null, tag), "score", 0, 20);
        LeadPage semAcento = queries.page(new LeadFilter(null, null, null, null, null, null, "leonardo pires " + tag),
                "name", 0, 20);

        assertEquals(2, mobiles.totalElements());                           // Atlas não tem telefone
        assertEquals(1, semAcento.totalElements());
    }

    @Test
    void mudarStatusMoveDeColunaEGravaHistorico() {
        Long cordial = find("Cordial");

        commands.update(cordial, new LeadUpdate(LeadStatus.CONTATADO, "mandei WhatsApp", null, null, null, null, false));
        flushAndClear();

        LeadBoard board = queries.board(byTag(), 20);
        assertEquals(2, board.columns().get(0).count());
        assertEquals(1, board.columns().get(1).count());

        LeadDetail detail = queries.detail(cordial);
        assertEquals(2, detail.statusHistory().size());
        assertNull(detail.statusHistory().get(0).from());                  // entrada pela importação
        assertEquals(LeadStatus.CONTATADO, detail.statusHistory().get(1).to());
        assertEquals("mandei WhatsApp", detail.statusHistory().get(1).note());
        assertEquals(1, detail.ratingHistory().size());
    }

    @Test
    void detalheMostraLeadsNoMesmoLocal() {
        LeadDetail detail = queries.detail(find("Cordial"));

        assertTrue(detail.sameLocation().stream()
                .anyMatch(r -> r.name().startsWith("Leonardo") && r.matchType().equals("PHONE")));
    }

    @Test
    void scoreManualReordenaEFiltroMinScoreUsaOEfetivo() {
        Long atlas = find("Atlas");
        commands.update(atlas, new LeadUpdate(null, null, null, null, null, 99, false));
        flushAndClear();

        LeadCard first = queries.board(byTag(), 20).columns().get(0).leads().get(0);
        LeadPage acima95 = queries.page(new LeadFilter(null, null, null, null, null, 95, tag), "score", 0, 20);

        assertEquals(atlas, first.id());
        assertTrue(first.scoreOverridden());
        assertEquals(1, acima95.totalElements());
    }

    @Test
    void siteCadastradoSaiDoFiltroSemSite() {
        Long cordial = find("Cordial");
        commands.update(cordial, new LeadUpdate(null, null, null, "cordialpilates.com.br", null, null, false));
        flushAndClear();

        LeadPage semSite = queries.page(new LeadFilter(null, null, null, true, null, null, tag), "score", 0, 20);

        assertEquals(2, semSite.totalElements());
        assertEquals("https://cordialpilates.com.br", queries.detail(cordial).lead().websiteUrl());
    }

    private LeadFilter byTag() {
        return new LeadFilter(null, null, null, null, null, null, tag);
    }

    private Long find(String prefix) {
        return queries.page(byTag(), "name", 0, 20).items().stream()
                .filter(c -> c.name().startsWith(prefix)).findFirst().orElseThrow().id();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
