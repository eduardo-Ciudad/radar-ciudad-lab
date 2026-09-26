package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.exception.DuplicateImportException;
import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ImportBatch;
import com.eduar.radarciudadlab.domain.model.lead.ImportReport;
import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadDraft;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import com.eduar.radarciudadlab.domain.model.lead.RowIssue;
import com.eduar.radarciudadlab.domain.port.out.ImportBatchRepository;
import com.eduar.radarciudadlab.domain.port.out.LeadFileParser;
import com.eduar.radarciudadlab.domain.port.out.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importa o CSV da ferramenta de leads.
 * Tudo numa transação: se algo quebrar no meio, nem o import_batch nem os leads ficam pela metade.
 */
@Service
public class ImportLeadsService {

    static final int MAX_RECENT = 50;
    private static final int FILE_NAME_MAX = 255;

    private final LeadFileParser parser;
    private final LeadRowNormalizer normalizer;
    private final LeadRepository leads;
    private final ImportBatchRepository batches;

    public ImportLeadsService(LeadFileParser parser, LeadRowNormalizer normalizer,
                              LeadRepository leads, ImportBatchRepository batches) {
        this.parser = parser;
        this.normalizer = normalizer;
        this.leads = leads;
        this.batches = batches;
    }

    @Transactional
    public ImportReport importFile(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidLeadFileException("O arquivo está vazio");
        }
        String hash = sha256(content);
        batches.findByFileHash(hash).ifPresent(existing -> {
            throw new DuplicateImportException(existing.id(), existing.importedAt());
        });

        ParsedLeadFile file = parser.parse(content);
        ImportBatch batch = batches.create(cleanFileName(fileName), hash, file.content());

        List<RowIssue> errors = new ArrayList<>();
        List<RowIssue> warnings = new ArrayList<>();
        Map<String, LeadDraft> drafts = new LinkedHashMap<>(); // mantém a ordem do arquivo

        for (RawLeadRow row : file.rows()) {
            LeadRowNormalizer.Result result = normalizer.normalize(row);
            if (result.skipped()) {
                errors.add(result.error());
                continue;
            }
            warnings.addAll(result.warnings());
            LeadDraft draft = result.draft();
            LeadDraft first = drafts.putIfAbsent(draft.dedupKey(), draft);
            if (first != null) {
                errors.add(new RowIssue(row.line(), "duplicada da linha " + first.line()));
            }
        }

        Map<String, Lead> existing = leads.findByDedupKeys(drafts.keySet()).stream()
                .collect(Collectors.toMap(Lead::dedupKey, Function.identity()));

        int created = 0;
        int updated = 0;
        for (LeadDraft draft : drafts.values()) {
            Lead current = existing.get(draft.dedupKey());
            Lead saved;
            if (current == null) {
                saved = leads.save(Lead.createFrom(draft, batch.id()));
                created++;
            } else {
                saved = leads.save(current.refreshFrom(draft, batch.id()));
                updated++;
            }
            // Histórico só quando o arquivo trouxe nota: snapshot vazio não diz nada sobre evolução
            if (draft.rating() != null) {
                leads.saveSnapshot(saved.id(), batch.id(), draft.rating(), null);
            }
        }

        int total = file.rows().size();
        batches.updateCounts(batch.id(), total, created, updated, errors.size());

        return new ImportReport(batch.id(), batch.fileName(), total, created, updated, errors.size(),
                List.copyOf(errors), List.copyOf(warnings));
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> recentImports(int limit) {
        if (limit < 1 || limit > MAX_RECENT) {
            throw new IllegalArgumentException("O parâmetro 'limit' deve estar entre 1 e " + MAX_RECENT);
        }
        return batches.findRecent(limit);
    }

    /** Só o nome do arquivo: alguns navegadores mandam o caminho inteiro (C:\Users\...\leads.csv). */
    static String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "leads.csv";
        }
        String name = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1).trim();
        if (name.isEmpty()) {
            return "leads.csv";
        }
        return name.length() > FILE_NAME_MAX ? name.substring(0, FILE_NAME_MAX) : name;
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
