package com.eduar.radarciudadlab.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Teste unitário do filtro da chave do BFF: sem Spring, requisições e cadeia de filtros falsas. */
class ClientKeyFilterTest {

    private static final String KEY = "chave-do-bff-de-teste-com-mais-de-32-caracteres";

    private final ClientKeyFilter enabled = new ClientKeyFilter(new SecurityProperties.ClientKey(KEY, true));

    @Test
    void semHeaderRecebe401ENaoSegueACadeia() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        enabled.doFilter(apiRequest(null), response, chain);

        assertEquals(401, response.getStatus());
        // O filtro define UTF-8, então o header sai como "application/problem+json;charset=UTF-8"
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
                MediaType.parseMediaType(response.getContentType())));
        assertTrue(response.getContentAsString().contains("Cliente não autorizado"));
        assertNull(chain.getRequest(), "a requisição não pode seguir para a aplicação");
    }

    @Test
    void chaveErradaRecebe401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        enabled.doFilter(apiRequest(KEY + "x"), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void chaveCertaSegueACadeia() throws Exception {
        assertPassesThrough(enabled, apiRequest(KEY));
    }

    @Test
    void filtroDesligadoSemChaveConfigurada() throws Exception {
        // Dev: sem RADAR_CLIENT_KEY o filtro não atrapalha Postman nem testes
        ClientKeyFilter disabled = new ClientKeyFilter(new SecurityProperties.ClientKey(null, false));

        assertPassesThrough(disabled, apiRequest(null));
    }

    @Test
    void rotaForaDeApiNaoEFiltrada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");

        assertPassesThrough(enabled, request);
    }

    private static void assertPassesThrough(ClientKeyFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "a requisição deveria seguir para a aplicação");
        assertEquals(200, response.getStatus());
    }

    private static MockHttpServletRequest apiRequest(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        if (key != null) {
            request.addHeader(ClientKeyFilter.HEADER, key);
        }
        return request;
    }
}
