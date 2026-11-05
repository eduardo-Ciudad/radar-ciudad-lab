package com.eduar.radarciudadlab.config.security;

import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Teste unitário da emissão: encoder e decoder reais com chave de teste, relógio fixo, sem Spring. */
class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T18:00:00Z");
    private static final Duration EXPIRATION = Duration.ofHours(8);
    private static final String ISSUER = "radar-ciudadlab";
    private static final SecretKey KEY = new SecretKeySpec(
            "chave-de-teste-do-radar-com-mais-de-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    @Test
    void tokenLevaUsuarioVersaoPapelEValidade() {
        JwtTokenService service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), properties(), Clock.fixed(NOW, ZoneOffset.UTC));
        AppUser user = new AppUser(7L, "admin@teste.local", "{noop}x", UserRole.ADMIN, true, 3);

        JwtTokenService.IssuedToken issued = service.issue(user);
        Jwt jwt = decoder().decode(issued.value());

        assertEquals("admin@teste.local", jwt.getSubject());
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(7L, ((Number) jwt.getClaim("uid")).longValue());
        assertEquals(3L, ((Number) jwt.getClaim("ver")).longValue());
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals(NOW, jwt.getIssuedAt());
        assertEquals(NOW.plus(EXPIRATION), jwt.getExpiresAt());
        assertEquals(NOW.plus(EXPIRATION), issued.expiresAt());
    }

    /** Só o issuer: o relógio do teste é fixo, então validar exp contra a hora real quebraria com o tempo. */
    private static NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new JwtIssuerValidator(ISSUER));
        return decoder;
    }

    private static SecurityProperties properties() {
        return new SecurityProperties(
                new SecurityProperties.Jwt("nao-usado", EXPIRATION, ISSUER),
                new SecurityProperties.Cors(List.of()),
                new SecurityProperties.Admin(null, null),
                new SecurityProperties.Login(5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new SecurityProperties.ClientKey(null, false));
    }
}
