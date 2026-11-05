package com.eduar.radarciudadlab.config.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "radar.security")
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @DefaultValue Cors cors,
        @DefaultValue Admin admin,
        @Valid @DefaultValue Login login,
        @DefaultValue ClientKey clientKey
) {

    public record Jwt(
            @NotBlank(message = "defina a variável de ambiente JWT_SECRET (Base64, mínimo 32 bytes)") String secret,
            @DefaultValue("8h") Duration expiration,
            @DefaultValue("radar-ciudadlab") String issuer
    ) {}

    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {}

    /** Administrador criado na primeira inicialização, se ainda não existir nenhum usuário. */
    public record Admin(String email, String password) {}

    /** Limite de login por conta: maxFailures falhas dentro de window bloqueiam o e-mail por lockDuration. */
    public record Login(
            @DefaultValue("5") @Positive int maxFailures,
            @DefaultValue("15m") Duration window,
            @DefaultValue("15m") Duration lockDuration
    ) {}

    /**
     * Segredo compartilhado com o BFF, enviado no header X-Radar-Client-Key (ClientKeyFilter).
     * required=true (profile prod) faz a aplicação recusar subir sem uma chave de 32+ caracteres.
     */
    public record ClientKey(String value, @DefaultValue("false") boolean required) {

        public static final int MIN_LENGTH = 32;

        public ClientKey {
            if (required && (value == null || value.isBlank() || value.trim().length() < MIN_LENGTH)) {
                throw new IllegalStateException("defina RADAR_CLIENT_KEY com no mínimo " + MIN_LENGTH
                        + " caracteres (gere com: openssl rand -base64 48)");
            }
        }

        public boolean enabled() {
            return value != null && !value.isBlank();
        }
    }
}