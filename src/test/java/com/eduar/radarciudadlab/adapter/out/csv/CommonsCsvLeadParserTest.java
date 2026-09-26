package com.eduar.radarciudadlab.adapter.out.csv;

import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonsCsvLeadParserTest {

    private final CommonsCsvLeadParser parser = new CommonsCsvLeadParser();

    @Test
    void leOArquivoRealDoExportador() throws IOException {
        ParsedLeadFile file = parser.parse(fixture("leads/leads-2026-09-24.csv"));

        assertEquals(28, file.rows().size());
        RawLeadRow first = file.rows().get(0);
        assertEquals(2, first.line());                         // cabeçalho é a linha 1
        assertEquals("MR Fisioterapia", first.name());         // BOM não "come" a coluna Nome
        assertEquals("+55 17 3225-1791", first.phone());
        assertEquals("5", first.rating());
        assertFalse(file.content().startsWith("\uFEFF"));      // raw_content vai sem BOM
    }

    @Test
    void aceitaPontoEVirgulaEWindows1252() {
        String csv = "Nome;Endereço;Avaliação\n\"Clínica São José\";\"R. X, 1 - Centro, Rio Preto - SP, 15010-080\";\"4,5\"\n";

        ParsedLeadFile file = parser.parse(csv.getBytes(Charset.forName("windows-1252")));

        RawLeadRow row = file.rows().get(0);
        assertEquals("Clínica São José", row.name());
        assertEquals("4,5", row.rating());
        assertNull(row.phone());                               // coluna ausente vira null
    }

    @Test
    void recusaArquivoSemColunaDeEndereco() {
        byte[] csv = "Nome,Telefone\n\"X\",\"1\"\n".getBytes(StandardCharsets.UTF_8);

        assertThrows(InvalidLeadFileException.class, () -> parser.parse(csv));
    }

    @Test
    void recusaArquivoVazio() {
        assertThrows(InvalidLeadFileException.class, () -> parser.parse(new byte[0]));
    }

    private static byte[] fixture(String path) throws IOException {
        try (InputStream in = CommonsCsvLeadParserTest.class.getClassLoader().getResourceAsStream(path)) {
            return in.readAllBytes();
        }
    }
}
