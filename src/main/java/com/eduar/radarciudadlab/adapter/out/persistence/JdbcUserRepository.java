package com.eduar.radarciudadlab.adapter.out.persistence;


import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcUserRepository implements UserRepository {

    private static final RowMapper<AppUser> MAPPER = (rs, i) -> new AppUser(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            UserRole.valueOf(rs.getString("role")),
            rs.getBoolean("active"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return jdbc.query("""
                SELECT id, email, password_hash, role, active
                FROM app_user
                WHERE lower(email) = lower(:email)
                """, new MapSqlParameterSource("email", email), MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public long count() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM app_user", new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public AppUser create(String email, String passwordHash, UserRole role) {
        Long id = jdbc.queryForObject("""
                INSERT INTO app_user (email, password_hash, role)
                VALUES (:email, :hash, :role)
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("hash", passwordHash)
                .addValue("role", role.name()), Long.class);
        return new AppUser(id, email, passwordHash, role, true);
    }

    @Override
    public void recordLogin(Long userId) {
        jdbc.update("UPDATE app_user SET last_login_at = now() WHERE id = :id",
                new MapSqlParameterSource("id", userId));
    }
}