package com.eduar.radarciudadlab.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class JsonAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
                "Token ausente, inválido ou expirado. Faça login em POST /api/auth/login.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
                "Seu usuário não tem permissão para esta operação.");
    }

    private static void write(HttpServletRequest request, HttpServletResponse response,
                              int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + escape(title)
                + "\",\"status\":" + status
                + ",\"detail\":\"" + escape(detail)
                + "\",\"instance\":\"" + escape(request.getRequestURI()) + "\"}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}