package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.model.lead.LeadDraft;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras de linha: o que pula, o que vira aviso e como a chave de dedup é montada. */
class LeadRowNormalizerTest {

    private static final String ADDRESS =
            "R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080";

    // Fake: só aceita números com 12 ou 13 dígitos; 13 dígitos = celular
    private final LeadRowNormalizer normalizer = new LeadRowNormalizer(raw -> {
        String digits = raw.replaceAll("\\D", "");
        return digits.length() >= 12
                ? Optional.of(new PhoneNumber("+" + digits, digits.length() == 13))
                : Optional.empty();
    });

    @Test
    void normalizaLinhaCompleta() {
        var result = normalizer.normalize(row("Cordial Pro Pilates", "+55 17 99772-6959", ADDRESS, "5", "novo"));

        LeadDraft draft = result.draft();
        assertEquals("+5517997726959", draft.phone().e164());
        assertTrue(draft.phone().mobile());
        assertEquals(new BigDecimal("5.0"), draft.rating());
        assertEquals(LeadStatus.NOVO, draft.status());
        assertNull(draft.websiteUrl());          // "" no CSV = desconhecido
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void pulaLinhaSemNome() {
        var result = normalizer.normalize(row("  ", "+55 17 99772-6959", ADDRESS, "5", "novo"));

        assertTrue(result.skipped());
        assertEquals("nome vazio", result.error().reason());
    }

    @Test
    void pulaLinhaSemEndereco() {
        assertTrue(normalizer.normalize(row("Clínica X", "", "", "5", "novo")).skipped());
    }

    @Test
    void semTelefoneESemNotaAindaImporta() {
        var result = normalizer.normalize(row("Clínica Atlas", "", ADDRESS, "", "novo"));

        assertNull(result.draft().phone());
        assertNull(result.draft().rating());
        assertTrue(result.warnings().isEmpty());
        assertTrue(result.draft().dedupKey().startsWith("clinica atlas|end:"));
    }

    @Test
    void telefoneENotaInvalidosViramAviso() {
        var result = normalizer.normalize(row("Clínica X", "123", ADDRESS, "7,5", "novo"));

        assertNull(result.draft().phone());
        assertNull(result.draft().rating());
        assertEquals(2, result.warnings().size());
    }

    @Test
    void aceitaNotaComVirgula() {
        assertEquals(new BigDecimal("4.5"), normalizer.normalize(row("X", "", ADDRESS, "4,5", "")).draft().rating());
    }

    @Test
    void mesmoTelefoneComNomesDiferentesGeraChavesDiferentes() {
        // Cordial Pro e Leonardo Pires: mesmo número, anúncios diferentes
        String cordial = normalizer.normalize(row("Cordial Pro Pilates", "+55 17 99772-6959", ADDRESS, "5", "")).draft().dedupKey();
        String leonardo = normalizer.normalize(row("Fisioterapeuta Leonardo Pires", "+55 17 99772-6959", ADDRESS, "5", "")).draft().dedupKey();

        assertNotEquals(cordial, leonardo);
    }

    @Test
    void nomeComOutraCaixaOuAcentoGeraAMesmaChave() {
        String a = normalizer.normalize(row("CLÍNICA Reabilitar", "+55 17 3222-1862", ADDRESS, "", "")).draft().dedupKey();
        String b = normalizer.normalize(row("Clinica reabilitar", "+55 17 3222-1862", ADDRESS, "", "")).draft().dedupKey();

        assertEquals(a, b);
    }

    private static RawLeadRow row(String name, String phone, String address, String rating, String status) {
        return new RawLeadRow(2, name, "Policlínica", phone, address, "", "", rating, status);
    }
}
