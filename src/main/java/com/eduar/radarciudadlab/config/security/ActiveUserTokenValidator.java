package com.eduar.radarciudadlab.config.security;

import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Torna o JWT revogável: além da assinatura e da validade, o usuário do token precisa existir, estar ativo
 * e ter a mesma token_version que o claim "ver". Roda no JwtDecoder, então a falha vira o 401 padrão
 * do JsonAuthErrorHandler. Só chega aqui token com assinatura válida (o decoder confere antes).
 *
 * Custa uma consulta por chave primária a cada requisição, irrelevante no volume do Radar.
 */
public class ActiveUserTokenValidator implements OAuth2TokenValidator<Jwt> {

    static final String USER_ID_CLAIM = "uid";
    static final String VERSION_CLAIM = "ver";

    private static final OAuth2Error REVOKED = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "Token revogado ou usuário inativo", null);

    private final UserRepository users;

    public ActiveUserTokenValidator(UserRepository users) {
        this.users = users;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Long userId = asLong(jwt.getClaim(USER_ID_CLAIM));
        Long version = asLong(jwt.getClaim(VERSION_CLAIM));
        // Token sem "ver" = emitido antes desta regra: precisa logar de novo
        if (userId == null || version == null) {
            return OAuth2TokenValidatorResult.failure(REVOKED);
        }

        return users.findById(userId)
                .filter(AppUser::active)
                .filter(user -> user.tokenVersion() == version)
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(REVOKED));
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
