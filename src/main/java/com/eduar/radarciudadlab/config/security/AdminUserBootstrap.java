package com.eduar.radarciudadlab.config.security;

import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public AdminUserBootstrap(UserRepository users, PasswordEncoder passwordEncoder, SecurityProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }

        SecurityProperties.Admin admin = properties.admin();
        if (admin == null || isBlank(admin.email()) || isBlank(admin.password())) {
            log.warn("Nenhum usuário cadastrado. Defina ADMIN_EMAIL e ADMIN_PASSWORD e reinicie para criar o administrador.");
            return;
        }
        if (admin.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD precisa ter no mínimo " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        AppUser created = users.create(admin.email().trim(), passwordEncoder.encode(admin.password()), UserRole.ADMIN);
        log.info("Usuário administrador criado: {}", created.email());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}