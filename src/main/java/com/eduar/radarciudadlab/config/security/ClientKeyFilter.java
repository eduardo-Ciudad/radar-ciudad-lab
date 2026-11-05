package com.eduar.radarciudadlab.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Só deixa chegar na API quem manda o segredo do BFF no header X-Radar-Client-Key.
 *
 * A API fica pública (a Vercel não tem IP fixo para liberar no firewall), então este filtro tira da frente
 * da aplicação o tráfego que não vem do frontend: robôs varrendo a internet nem chegam no login.
 * Não substitui o JWT: é uma camada a mais, antes dele.
 *
 * Registrado na cadeia do Spring Security (SecurityConfig), não como @Component, para não rodar duas vezes.
 */
public class ClientKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Radar-Client-Key";

    private final SecurityProperties.ClientKey config;
    private final byte[] expectedDigest;

    public ClientKeyFilter(SecurityProperties.ClientKey config) {
        this.config = config;
        this.expectedDigest = config.enabled() ? sha256(config.value().trim()) : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !config.enabled() || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        if (received != null && matches(received)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Cliente não autorizado\","
                + "\"status\":401,\"detail\":\"Requisição sem a chave do cliente.\"}");
    }

    /**
     * Compara os hashes, não as strings: MessageDigest.isEqual leva o mesmo tempo onde quer que a diferença
     * esteja, e o hash tem tamanho fixo, então o tempo de resposta não revela nem o tamanho da chave.
     * Um equals comum pararia no primeiro caractere diferente e permitiria descobrir a chave aos poucos.
     */
    private boolean matches(String received) {
        return MessageDigest.isEqual(expectedDigest, sha256(received.trim()));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
