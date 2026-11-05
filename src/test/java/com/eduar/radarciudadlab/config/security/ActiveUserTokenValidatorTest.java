package com.eduar.radarciudadlab.config.security;

import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Teste unitário da revogação: sem Spring, sem banco, UserRepository fake. */
class ActiveUserTokenValidatorTest {

    private static final long USER_ID = 7L;

    private FakeUsers users;
    private ActiveUserTokenValidator validator;

    @BeforeEach
    void setUp() {
        users = new FakeUsers();
        validator = new ActiveUserTokenValidator(users);
    }

    @Test
    void usuarioAtivoComMesmaVersaoPassa() {
        users.put(user(true, 2));

        assertFalse(validator.validate(token(USER_ID, 2)).hasErrors());
    }

    @Test
    void tokenSemUidFalha() {
        users.put(user(true, 0));

        assertRevoked(validator.validate(jwt(Map.of("ver", 0))));
    }

    @Test
    void tokenSemVersaoFalha() {
        // Tokens emitidos antes da regra não têm "ver": precisam de login novo
        users.put(user(true, 0));

        assertRevoked(validator.validate(jwt(Map.of("uid", USER_ID))));
    }

    @Test
    void usuarioInexistenteFalha() {
        assertRevoked(validator.validate(token(USER_ID, 0)));
    }

    @Test
    void usuarioInativoFalha() {
        users.put(user(false, 0));

        assertRevoked(validator.validate(token(USER_ID, 0)));
    }

    @Test
    void versaoDiferenteFalha() {
        // Logout ou desativação somou 1 na versão: o token antigo morre
        users.put(user(true, 1));

        assertRevoked(validator.validate(token(USER_ID, 0)));
    }

    private static void assertRevoked(OAuth2TokenValidatorResult result) {
        assertTrue(result.hasErrors());
        assertEquals(OAuth2ErrorCodes.INVALID_TOKEN, result.getErrors().iterator().next().getErrorCode());
    }

    private static AppUser user(boolean active, int tokenVersion) {
        return new AppUser(USER_ID, "admin@teste.local", "{noop}x", UserRole.ADMIN, active, tokenVersion);
    }

    private static Jwt token(long userId, int version) {
        return jwt(Map.of("uid", userId, "ver", version));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("admin@teste.local")
                .claims(c -> c.putAll(claims))
                .build();
    }

    private static final class FakeUsers implements UserRepository {
        private final Map<Long, AppUser> byId = new HashMap<>();

        void put(AppUser user) {
            byId.put(user.id(), user);
        }

        @Override
        public Optional<AppUser> findById(Long userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public Optional<AppUser> findByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AppUser create(String email, String passwordHash, UserRole role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordLogin(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revokeTokens(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deactivate(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
