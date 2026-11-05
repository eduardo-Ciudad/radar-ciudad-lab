package com.eduar.radarciudadlab.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS em produção: CORS_ALLOWED_ORIGINS vazio precisa virar lista vazia (nenhuma origem aceita),
 * e não, por exemplo, uma origem "" ou o padrão de dev.
 */
class CorsConfigurationTest {

    @Test
    void variavelVaziaViraListaVazia() {
        Binder binder = new Binder(new MapConfigurationPropertySource(
                Map.of("radar.security.cors.allowed-origins", "")));

        SecurityProperties.Cors cors = binder.bind("radar.security.cors", Bindable.of(SecurityProperties.Cors.class))
                .orElseThrow(() -> new AssertionError("não fez o bind"));

        assertTrue(cors.allowedOrigins().isEmpty());
    }

    @Test
    void listaVaziaRecusaQualquerOrigem() {
        CorsConfiguration config = configFor(List.of());

        assertNull(config.checkOrigin("http://localhost:3000"));
        assertNull(config.checkOrigin("https://site-malicioso.com"));
    }

    @Test
    void origemConfiguradaEAceitaEOutrasNao() {
        CorsConfiguration config = configFor(List.of("http://localhost:3000"));

        assertEquals("http://localhost:3000", config.checkOrigin("http://localhost:3000"));
        assertNull(config.checkOrigin("https://site-malicioso.com"));
    }

    private static CorsConfiguration configFor(List<String> origins) {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("nao-usado", Duration.ofHours(8), "radar-ciudadlab"),
                new SecurityProperties.Cors(origins),
                new SecurityProperties.Admin(null, null),
                new SecurityProperties.Login(5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new SecurityProperties.ClientKey(null, false));
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(properties);
        return source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/sites"));
    }
}
