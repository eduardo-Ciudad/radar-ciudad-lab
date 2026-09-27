package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.DuplicateImportException;
import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import com.eduar.radarciudadlab.domain.model.lead.ImportReport;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadScorer;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;
import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import com.eduar.radarciudadlab.domain.port.out.ImportBatchRepository;
import com.eduar.radarciudadlab.domain.port.out.LeadFileParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fluxo da importação com fakes em memória: sem banco, sem Spring. */
class ImportLeadsServiceTest {

    private static final String ADDRESS =
            "R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080";

    private final FakeParser parser = new FakeParser();
    private final InMemoryLeadRepository leads = new InMemoryLeadRepository();
    private final FakeBatches batches = new FakeBatches();
    private final ImportLeadsService service = new ImportLeadsService(parser, new LeadRowNormalizer(raw -> {
        String digits = raw.replaceAll("\\D", "");
        return digits.length() >= 12
                ? Optional.of(new PhoneNumber("+" + digits, digits.length() == 13))
                : Optional.empty();
    }), leads, batches, new LeadScorer(Set.of("fisioterap", "pilates")));

    @Test
    void criaLeadsEPulaLinhasInvalidasEDuplicadas() {
        parser.rows = List.of(
                row(2, "Cordial Pro", "+55 17 99772-6959", "5"),
                row(3, "", "+55 17 3000-0000", "5"),                 // sem nome: pula
                row(4, "Cordial Pro", "+55 17 99772-6959", "4.8"),   // mesma chave da linha 2: pula
                row(5, "Clínica Atlas", "", ""));                     // sem telefone e sem nota: entra

        ImportReport report = service.importFile("leads.csv", bytes("a"));

        assertEquals(4, report.totalRows());
        assertEquals(2, report.created());
        assertEquals(0, report.updated());
        assertEquals(2, report.skipped());
        assertEquals("duplicada da linha 2", report.errors().get(1).reason());
        assertEquals(1, leads.snapshots);                             // só quem tinha nota
        assertEquals(List.of(4, 2, 0, 2), batches.lastCounts);
    }

    @Test
    void calculaScoreERegistraEntradaNoFunil() {
        parser.rows = List.of(row(2, "Cordial Pro Pilates", "+55 17 99772-6959", "5"));

        service.importFile("leads.csv", bytes("score"));

        Lead lead = leads.byName("Cordial Pro Pilates");
        // nota 5 (35) + avaliações desconhecidas (10) + celular (15) + sem site (15) + pilates (10)
        assertEquals(85, lead.score());
        assertEquals(1, leads.statusChanges.size());
        assertNull(leads.statusChanges.get(0).from());
        assertEquals(LeadStatus.NOVO, leads.statusChanges.get(0).to());
        assertNotNull(leads.statusChanges.get(0).note());
    }

    @Test
    void reimportacaoAtualizaSoDadosDaFerramenta() {
        parser.rows = List.of(row(2, "Cordial Pro", "+55 17 99772-6959", "4.5"));
        service.importFile("leads.csv", bytes("v1"));

        // você mexeu no lead pelo sistema
        Lead saved = leads.byName("Cordial Pro");
        leads.byId.put(saved.id(), withStatusAndNotes(saved, LeadStatus.REUNIAO, "ligar sexta"));

        parser.rows = List.of(row(2, "Cordial Pro", "+55 17 99772-6959", "4.9"));
        ImportReport report = service.importFile("leads-2.csv", bytes("v2"));

        Lead after = leads.byId.get(saved.id());
        assertEquals(0, report.created());
        assertEquals(1, report.updated());
        assertEquals(new BigDecimal("4.9"), after.rating());          // dado da ferramenta: atualiza
        assertEquals(LeadStatus.REUNIAO, after.status());             // seu: não mexe
        assertEquals("ligar sexta", after.notes());
        assertEquals(saved.firstBatchId(), after.firstBatchId());
        assertEquals(report.batchId(), after.lastBatchId());
        assertEquals(1, leads.statusChanges.size());                  // reimportação não mexe no funil
    }

    @Test
    void mesmoArquivoDuasVezesDaConflito() {
        parser.rows = List.of(row(2, "Cordial Pro", "+55 17 99772-6959", "5"));
        service.importFile("leads.csv", bytes("igual"));

        assertThrows(DuplicateImportException.class, () -> service.importFile("copia.csv", bytes("igual")));
    }

    @Test
    void arquivoVazioEInvalido() {
        assertThrows(InvalidLeadFileException.class, () -> service.importFile("x.csv", new byte[0]));
    }

    @Test
    void limpaCaminhoDoNomeDoArquivo() {
        assertEquals("leads.csv", ImportLeadsService.cleanFileName("C:\\Users\\eduar\\Downloads\\leads.csv"));
        assertEquals("leads.csv", ImportLeadsService.cleanFileName(null));
    }

    private static RawLeadRow row(int line, String name, String phone, String rating) {
        return new RawLeadRow(line, name, "Policlínica", phone, ADDRESS, "", "", rating, "novo");
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static Lead withStatusAndNotes(Lead l, LeadStatus status, String notes) {
        return new Lead(l.id(), l.dedupKey(), l.name(), l.category(), l.phoneE164(), l.mobile(), l.address(),
                l.websiteUrl(), l.instagramHandle(), l.rating(), l.reviewsCount(), status, l.score(),
                l.scoreOverride(), notes, l.aiSummary(), l.firstBatchId(), l.lastBatchId(), l.createdAt(), l.updatedAt());
    }

    // ---- fakes ----

    private static class FakeParser implements LeadFileParser {
        List<RawLeadRow> rows = List.of();

        @Override
        public ParsedLeadFile parse(byte[] content) {
            return new ParsedLeadFile(new String(content, StandardCharsets.UTF_8), rows);
        }
    }

    private static class FakeBatches implements ImportBatchRepository {
        final List<ImportBatch> all = new ArrayList<>();
        List<Integer> lastCounts;

        @Override
        public Optional<ImportBatch> findByFileHash(String fileHash) {
            return all.stream().filter(b -> b.fileHash().equals(fileHash)).findFirst();
        }

        @Override
        public ImportBatch create(String fileName, String fileHash, String rawContent) {
            ImportBatch b = new ImportBatch((long) all.size() + 1, fileName, fileHash, Instant.now(), 0, 0, 0, 0);
            all.add(b);
            return b;
        }

        @Override
        public void updateCounts(Long batchId, int totalRows, int created, int updated, int skipped) {
            lastCounts = List.of(totalRows, created, updated, skipped);
        }

        @Override
        public List<ImportBatch> findRecent(int limit) {
            return all;
        }
    }
}
