package com.eduar.radarciudadlab.adapter.out.persistence.lead;

import com.eduar.radarciudadlab.application.ImportLeadsService;
import com.eduar.radarciudadlab.domain.exception.DuplicateImportException;
import com.eduar.radarciudadlab.domain.model.lead.ImportReport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Importação de ponta a ponta contra o PostgreSQL local (parser real, libphonenumber, JPA).
 * Os nomes levam um sufixo aleatório para não colidir com leads que você já importou de verdade;
 * @Transactional desfaz tudo no fim de cada teste.
 *
 * Como o teste inteiro roda numa transação só, flushAndClear() simula o que acontece entre duas requisições
 * de verdade: grava o que o JPA tem pendente e esquece as entidades em memória.
 */
@SpringBootTest
@Transactional
class LeadImportIT {

    @Autowired ImportLeadsService service;
    @Autowired NamedParameterJdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void importaEReimportaPreservandoOQueEDoUsuario() {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        ImportReport first = service.importFile("it-" + tag + ".csv", csv(tag, "5", "4.8"));
        flushAndClear();

        assertEquals(3, first.totalRows());
        assertEquals(2, first.created());
        assertEquals(1, first.skipped());                     // linha sem nome

        Map<String, Object> lead = findLead("Cordial IT " + tag);
        assertEquals("+5517997726959", lead.get("phone_e164"));
        assertEquals(true, lead.get("is_mobile"));
        assertEquals("15010-080", lead.get("zip_code"));
        assertEquals("NOVO", lead.get("status"));

        // você anda com o lead no pipeline
        jdbc.update("UPDATE lead SET status = 'REUNIAO', notes = 'ligar sexta' WHERE id = :id",
                new MapSqlParameterSource("id", lead.get("id")));

        ImportReport second = service.importFile("it-" + tag + "-2.csv", csv(tag, "4.9", "4.8"));
        flushAndClear();

        assertEquals(0, second.created());
        assertEquals(2, second.updated());
        Map<String, Object> after = findLead("Cordial IT " + tag);
        assertEquals(0, new BigDecimal("4.9").compareTo((BigDecimal) after.get("rating")));
        assertEquals("REUNIAO", after.get("status"));
        assertEquals("ligar sexta", after.get("notes"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM lead_snapshot WHERE lead_id = :id",
                new MapSqlParameterSource("id", lead.get("id")), Integer.class));
    }

    @Test
    void mesmoArquivoDuasVezesEhRecusado() {
        byte[] content = csv(UUID.randomUUID().toString().substring(0, 8), "5", "5");
        service.importFile("a.csv", content);

        assertThrows(DuplicateImportException.class, () -> service.importFile("b.csv", content));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Map<String, Object> findLead(String name) {
        return jdbc.queryForMap("SELECT * FROM lead WHERE name = :name", new MapSqlParameterSource("name", name));
    }

    /** Mesmo formato do exportador, com BOM. */
    private static byte[] csv(String tag, String cordialRating, String atlasRating) {
        String text = "\uFEFFNome,Categoria,Telefone,Endereço,Site,Instagram,Avaliação,Status\n"
                + "\"Cordial IT " + tag + "\",\"Policlínica\",\"+55 17 99772-6959\","
                + "\"R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080\",\"\",\"\",\""
                + cordialRating + "\",\"novo\"\n"
                + "\"\",\"Policlínica\",\"+55 17 3000-0000\",\"R. X, 1 - Centro, São José do Rio Preto - SP, 15010-080\",\"\",\"\",\"5\",\"novo\"\n"
                + "\"Atlas IT " + tag + "\",\"Fisioterapeuta\",\"\","
                + "\"R. Pernambuco, 2816 - Vila Redentora, São José do Rio Preto - SP, 15015-770\",\"\",\"\",\""
                + atlasRating + "\",\"novo\"\n";
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
