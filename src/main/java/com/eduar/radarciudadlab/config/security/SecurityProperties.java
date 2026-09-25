package com.eduar.radarciudadlab.config.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        @DefaultValue Admin admin
) {

    public record Jwt(
            @NotBlank(message = "defina a variável de ambiente JWT_SECRET (Base64, mínimo 32 bytes)") String secret,
            @DefaultValue("12h") Duration expiration,
            @DefaultValue("radar-ciudadlab") String issuer
    ) {}

    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {}

    /** Administrador criado na primeira inicialização, se ainda não existir nenhum usuário. */
    public record Admin(String email, String password) {}
}