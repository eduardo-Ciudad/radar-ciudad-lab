package com.eduar.radarciudadlab.adapter.out.csv;

import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import com.eduar.radarciudadlab.domain.model.lead.TextNormalizer;
import com.eduar.radarciudadlab.domain.port.out.LeadFileParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class CommonsCsvLeadParser implements LeadFileParser {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final Map<String, String> COLUMNS = Map.of(
            "nome", "name",
            "categoria", "category",
            "telefone", "phone",
            "endereco", "address",
            "site", "website",
            "instagram", "instagram",
            "avaliacao", "rating",
            "status", "status");

    private static final List<String> REQUIRED = List.of("name", "address");

    @Override
    public ParsedLeadFile parse(byte[] content) {
        String text = decode(content);
        if (text.isBlank()) {
            throw new InvalidLeadFileException("O arquivo está vazio");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(text))
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (CSVParser parser = format.parse(new StringReader(text))) {
            Map<String, String> headerByColumn = mapHeader(parser.getHeaderNames());

            List<RawLeadRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new RawLeadRow(
                        (int) record.getRecordNumber() + 1, // +1 do cabeçalho: bate com a linha no editor
                        value(record, headerByColumn, "name"),
                        value(record, headerByColumn, "category"),
                        value(record, headerByColumn, "phone"),
                        value(record, headerByColumn, "address"),
                        value(record, headerByColumn, "website"),
                        value(record, headerByColumn, "instagram"),
                        value(record, headerByColumn, "rating"),
                        value(record, headerByColumn, "status")));
            }
            return new ParsedLeadFile(text, rows);
        } catch (IOException | UncheckedIOException | IllegalStateException e) {
            // Commons CSV lança UncheckedIOException/IllegalStateException para aspas mal fechadas
            throw new InvalidLeadFileException("Não foi possível ler o CSV: " + e.getMessage(), e);
        }
    }

    static String decode(byte[] content) {
        int offset = hasUtf8Bom(content) ? 3 : 0;
        ByteBuffer bytes = ByteBuffer.wrap(content, offset, content.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(content, offset, content.length - offset, WINDOWS_1252);
        }
    }

    private static boolean hasUtf8Bom(byte[] content) {
        return content.length >= 3
                && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF;
    }

    static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String header = end < 0 ? text : text.substring(0, end);
        long semicolons = header.chars().filter(c -> c == ';').count();
        long commas = header.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private static Map<String, String> mapHeader(List<String> headerNames) {
        Map<String, String> headerByColumn = new HashMap<>();
        for (String header : headerNames) {
            String column = COLUMNS.get(TextNormalizer.normalize(header));
            if (column != null) {
                headerByColumn.putIfAbsent(column, header);
            }
        }
        List<String> missing = REQUIRED.stream().filter(c -> !headerByColumn.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidLeadFileException(
                    "Cabeçalho inválido: faltam as colunas Nome e/ou Endereço. Encontrado: " + headerNames);
        }
        return headerByColumn;
    }

    private static String value(CSVRecord record, Map<String, String> headerByColumn, String column) {
        String header = headerByColumn.get(column);
        if (header == null || !record.isSet(header)) {
            return null;
        }
        return record.get(header);
    }
}