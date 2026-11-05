package com.eduar.radarciudadlab;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Trava as proteções do application-prod.yml, que não aparecem em nenhum outro teste:
 * se alguém mudar o arquivo por descuido, o build quebra. Lê o YAML direto, sem subir o Spring.
 */
class ProdProfileConfigTest {

    private static Properties prod;

    @BeforeAll
    static void carregaProfile() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        prod = yaml.getObject();
    }

    @Test
    void bancoEChaveDoGoogleSaoObrigatorios() {
        // Sem ":padrão" no placeholder: sem a variável, a aplicação não sobe
        assertEquals("${DB_URL}", value("spring.datasource.url"));
        assertEquals("${GA_CREDENTIALS_PATH}", value("radar.analytics.google.credentials-path"));
    }

    @Test
    void chaveDoClienteEObrigatoria() {
        assertEquals("true", value("radar.security.client-key.required"));
    }

    @Test
    void corsVazioPorPadrao() {
        assertEquals("${CORS_ALLOWED_ORIGINS:}", value("radar.security.cors.allowed-origins"));
    }

    @Test
    void errosNaoExpoemDetalheInterno() {
        assertEquals("never", value("server.error.include-message"));
        assertEquals("never", value("server.error.include-stacktrace"));
        assertEquals("never", value("server.error.include-binding-errors"));
        assertEquals("false", value("server.error.include-exception"));
    }

    @Test
    void flywayNaoPodeApagarOBanco() {
        assertEquals("true", value("spring.flyway.clean-disabled"));
    }

    @Test
    void desligamentoGracioso() {
        assertEquals("graceful", value("server.shutdown"));
    }

    /** O YAML guarda booleanos como Boolean; String.valueOf deixa tudo comparável como texto. */
    private static String value(String key) {
        return String.valueOf(prod.get(key));
    }
}
