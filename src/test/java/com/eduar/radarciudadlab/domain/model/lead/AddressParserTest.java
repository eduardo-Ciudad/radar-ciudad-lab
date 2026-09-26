package com.eduar.radarciudadlab.domain.model.lead;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddressParserTest {

    @Test
    void decompoeEnderecoSemComplemento() {
        ParsedAddress a = AddressParser.parse(
                "R. Josina Teixeira de Carvalho, 367 - Vila Anchieta, São José do Rio Preto - SP, 15050-305");

        assertEquals("R. Josina Teixeira de Carvalho", a.street());
        assertEquals("367", a.number());
        assertNull(a.complement());
        assertEquals("Vila Anchieta", a.neighborhood());
        assertEquals("São José do Rio Preto", a.city());
        assertEquals("SP", a.state());
        assertEquals("15050-305", a.zipCode());
    }

    @Test
    void separaComplementoDoBairro() {
        ParsedAddress a = AddressParser.parse(
                "Av. Romeu Strazzi, 325 - Sala 102 - Vila Sinibaldi, São José do Rio Preto - SP, 15084-010");

        assertEquals("Sala 102", a.complement());
        assertEquals("Vila Sinibaldi", a.neighborhood());
    }

    @Test
    void mesmoPredioComSalasDiferentesEOMesmoLocal() {
        ParsedAddress jensen = AddressParser.parse(
                "R. Rio Preto, 3258 - Vila Redentora, São José do Rio Preto - SP, 15015-760");
        ParsedAddress talita = AddressParser.parse(
                "R. Rio Preto, 3258 - Sl 2 - Vila Redentora, São José do Rio Preto - SP, 15015-760");

        assertEquals(jensen.normalized(), talita.normalized());
    }

    @Test
    void abreviacaoDoLogradouroNaoMudaOEnderecoNormalizado() {
        String abreviado = AddressParser.parse(
                "R. Delegado Pinto de Tolêdo, 3250 - Centro, São José do Rio Preto - SP, 15010-080").normalized();
        String porExtenso = AddressParser.parse(
                "Rua Delegado Pinto de Toledo, 3250 - Centro, São José do Rio Preto - SP, 15010080").normalized();

        assertEquals("rua delegado pinto de toledo 3250 15010-080", abreviado);
        assertEquals(abreviado, porExtenso);
    }

    @Test
    void enderecoForaDoPadraoGuardaSoOTexto() {
        ParsedAddress a = AddressParser.parse("Próximo ao Shopping Iguatemi");

        assertFalse(a.isStructured());
        assertEquals("Próximo ao Shopping Iguatemi", a.raw());
        assertEquals("proximo ao shopping iguatemi", a.normalized());
    }
}
