package com.eduar.radarciudadlab.domain.port.out;


import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;

import java.util.Optional;

public interface UserRepository {

    Optional<AppUser> findByEmail(String email);

    long count();

    AppUser create(String email, String passwordHash, UserRole role);

    void recordLogin(Long userId);
}
