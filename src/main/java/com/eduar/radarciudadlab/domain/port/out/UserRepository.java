package com.eduar.radarciudadlab.domain.port.out;


import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;

import java.util.Optional;

public interface UserRepository {

    Optional<AppUser> findByEmail(String email);

    long count();

    AppUser create(String email, String passwordHash, UserRole role);

    void recordLogin(Long userId);

    Optional<AppUser> findById(Long userId);

    /** Soma 1 na token_version: todos os tokens já emitidos para o usuário deixam de valer. */
    void revokeTokens(Long userId);

    /** Desativa e revoga os tokens no mesmo UPDATE: desativar sem revogar deixaria o token vivo até expirar. */
    void deactivate(Long userId);
}
