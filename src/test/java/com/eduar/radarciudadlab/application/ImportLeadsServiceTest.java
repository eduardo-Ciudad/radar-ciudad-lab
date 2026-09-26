package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.DuplicateImportException;
import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import com.eduar.radarciudadlab.domain.model.lead.ImportReport;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;
import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import com.eduar.radarciudadlab.domain.port.out.ImportBatchRepository;
import com.eduar.radarciudadlab.domain.port.out.LeadFileParser;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fluxo da importação com fakes em memória: sem banco, sem Spring. */
class ImportLeadsServiceTest {

    private static final String ADDRESS =
            "R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080";

    private final FakeParser parser = new FakeParser();
    private final FakeLeads leads = new FakeLeads();
    private final FakeBatches batches = new FakeBatches();
    private final ImportLeadsService service = new ImportLeadsService(parser, new LeadRowNormalizer(raw -> {
        String digits = raw.replaceAll("\\D", "");
        return digits.length() >= 12
                ? Optional.of(new PhoneNumber("+" + digits, digits.length() == 13))
                : Optional.empty();
    }), leads, batches);

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
    void reimportacaoAtualizaSoDadosDaFerramenta() {
        parser.rows = List.of(row(2, "Cordial Pro", "+55 17 99772-6959", "4.5"));
        service.importFile("leads.csv", bytes("v1"));

        // você mexeu no lead pelo sistema
        Lead saved = leads.byKey.values().iterator().next();
        leads.byKey.put(saved.dedupKey(), withStatusAndNotes(saved, LeadStatus.REUNIAO, "ligar sexta"));

        parser.rows = List.of(row(2, "Cordial Pro", "+55 17 99772-6959", "4.9"));
        ImportReport report = service.importFile("leads-2.csv", bytes("v2"));

        Lead after = leads.byKey.get(saved.dedupKey());
        assertEquals(0, report.created());
        assertEquals(1, report.updated());
        assertEquals(new BigDecimal("4.9"), after.rating());          // dado da ferramenta: atualiza
        assertEquals(LeadStatus.REUNIAO, after.status());             // seu: não mexe
        assertEquals("ligar sexta", after.notes());
        assertEquals(saved.firstBatchId(), after.firstBatchId());
        assertEquals(report.batchId(), after.lastBatchId());
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

    private static class FakeLeads implements LeadRepository {
        final Map<String, Lead> byKey = new HashMap<>();
        long nextId = 1;
        int snapshots;

        @Override
        public List<Lead> findByDedupKeys(Collection<String> keys) {
            return keys.stream().map(byKey::get).filter(l -> l != null).toList();
        }

        @Override
        public Lead save(Lead l) {
            Lead saved = l.id() != null ? l : new Lead(nextId++, l.dedupKey(), l.name(), l.category(), l.phoneE164(),
                    l.mobile(), l.address(), l.websiteUrl(), l.instagramHandle(), l.rating(), l.reviewsCount(),
                    l.status(), l.score(), l.scoreOverride(), l.notes(), l.aiSummary(), l.firstBatchId(),
                    l.lastBatchId(), Instant.now(), Instant.now());
            byKey.put(saved.dedupKey(), saved);
            return saved;
        }

        @Override
        public void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount) {
            snapshots++;
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
