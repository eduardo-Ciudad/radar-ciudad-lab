package com.eduar.radarciudadlab.domain.model.lead;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadScorerTest {

    private final LeadScorer scorer = new LeadScorer(Set.of("Fisioterapêutico", "pilates"));

    @Test
    void leadIdealChegaPertoDe100() {
        LeadScore score = scorer.score(lead("Studio Pilates Nd", "Policlínica", "5", 240, true, null));

        assertEquals(100, score.total());   // 35 + 25 + 15 + 15 + 10
        assertEquals(5, score.items().size());
    }

    @Test
    void dadosAusentesDaoPontuacaoNeutra() {
        // CSV atual: sem avaliações; aqui também sem nota e sem telefone
        LeadScore score = scorer.score(lead("Clínica Atlas", "Fisioterapeuta", null, null, null, null));

        assertEquals(15 + 10 + 0 + 15 + 0, score.total());   // "fisioterapeuta" não contém "fisioterapeutico"
    }

    @Test
    void palavraDoNichoComparaSemAcentoNoNomeENaCategoria() {
        LeadScorer fisio = new LeadScorer(Set.of("Fisioterap"));

        ScoreItem pelaCategoria = item(fisio.score(lead("Espaço GZ", "Fisioterapeuta", "5", null, true, null)));
        ScoreItem peloNome = item(fisio.score(lead("Espaço GZ - Fisioterapia", "Policlínica", "5", null, true, null)));
        ScoreItem fora = item(fisio.score(lead("Centro Integrado", "Laboratório médico", "5", null, true, null)));

        assertEquals(10, pelaCategoria.points());
        assertEquals(10, peloNome.points());
        assertEquals(0, fora.points());
    }

    @Test
    void terSiteReduzAOportunidade() {
        int semSite = scorer.score(lead("X Pilates", "Policlínica", "4.5", null, true, null)).total();
        int comSite = scorer.score(lead("X Pilates", "Policlínica", "4.5", null, true, "https://x.com.br")).total();

        assertEquals(10, semSite - comSite);
    }

    @Test
    void avaliacoesSeguemEscalaLogaritmica() {
        int poucas = reviewPoints(2);
        int algumas = reviewPoints(30);
        int muitas = reviewPoints(500);

        assertTrue(poucas < algumas && algumas < muitas);
        assertEquals(25, muitas);
    }

    private int reviewPoints(int reviews) {
        return scorer.score(lead("X", "Y", "5", reviews, false, null)).items().get(1).points();
    }

    private static ScoreItem item(LeadScore score) {
        return score.items().stream().filter(i -> i.criterion().equals("CATEGORY")).findFirst().orElseThrow();
    }

    private static Lead lead(String name, String category, String rating, Integer reviews, Boolean mobile, String site) {
        ParsedAddress address = AddressParser.parse("R. X, 1 - Centro, São José do Rio Preto - SP, 15010-080");
        return new Lead(1L, "k", name, category, mobile == null ? null : "+5517999999999", mobile, address, site, null,
                rating == null ? null : new BigDecimal(rating), reviews, LeadStatus.NOVO, null, null, null, null,
                1L, 1L, null, null);
    }
}
