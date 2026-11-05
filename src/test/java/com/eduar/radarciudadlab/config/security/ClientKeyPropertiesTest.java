package com.eduar.radarciudadlab.config.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Em produção (required=true) a aplicação não pode subir sem uma chave forte. */
class ClientKeyPropertiesTest {

    private static final String STRONG = "a".repeat(SecurityProperties.ClientKey.MIN_LENGTH);

    @Test
    void obrigatoriaSemChaveFalha() {
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.ClientKey(null, true));
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.ClientKey("   ", true));
    }

    @Test
    void obrigatoriaComChaveCurtaFalha() {
        String curta = "a".repeat(SecurityProperties.ClientKey.MIN_LENGTH - 1);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new SecurityProperties.ClientKey(curta, true));
        assertTrue(e.getMessage().contains("RADAR_CLIENT_KEY"));
    }

    @Test
    void obrigatoriaComChaveForteSobe() {
        assertTrue(assertDoesNotThrow(() -> new SecurityProperties.ClientKey(STRONG, true)).enabled());
    }

    @Test
    void opcionalSemChaveSobeDesligada() {
        assertFalse(new SecurityProperties.ClientKey(null, false).enabled());
        assertFalse(new SecurityProperties.ClientKey("", false).enabled());
    }
}
