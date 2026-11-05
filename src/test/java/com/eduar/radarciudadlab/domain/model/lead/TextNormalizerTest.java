package com.eduar.radarciudadlab.domain.model.lead;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O TextNormalizer alimenta o dedup, o score e a busca do funil.
 * O JdbcLeadQueryRepository usa o resultado direto num LIKE sem ESCAPE: estes testes garantem
 * que os curingas do SQL (% e _) e a barra de escape nunca passam.
 */
class TextNormalizerTest {

    @Test
    void removeAcentoEMaiusculas() {
        assertEquals("clinica sao jose", TextNormalizer.normalize("Clínica São José"));
    }

    @Test
    void curingasDoLikeViramEspaco() {
        assertEquals("50 off", TextNormalizer.normalize("50%_off"));
        assertEquals("a b", TextNormalizer.normalize("a\\b"));
    }

    @Test
    void textoSoComCuringasViraVazio() {
        // Vazio = sem filtro de busca no repositório
        assertEquals("", TextNormalizer.normalize("%"));
        assertEquals("", TextNormalizer.normalize("__%%"));
    }

    @Test
    void espacosEPontuacaoRepetidosColapsam() {
        assertEquals("pilates fisio", TextNormalizer.normalize("  Pilates   & Fisio! "));
    }

    @Test
    void nuloViraVazio() {
        assertEquals("", TextNormalizer.normalize(null));
    }
}
