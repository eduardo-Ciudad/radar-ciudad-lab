package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.TrackedSite;
import com.eduar.radarciudadlab.domain.port.out.TrackedSiteRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Sites a sincronizar. Sites com NO_ACCESS continuam na lista de propósito:
 * se ficassem de fora, nunca se recuperariam quando o cliente devolvesse o acesso.
 */
@Repository
public class JdbcTrackedSiteRepository implements TrackedSiteRepository {

    private static final String BASE_SELECT = """
            SELECT s.id, s.name, s.ga_property_id
            FROM site s
            LEFT JOIN client c ON c.id = s.client_id
            WHERE s.ga_property_id IS NOT NULL
              AND (c.id IS NULL OR c.active)
            """;

    private static final RowMapper<TrackedSite> MAPPER = (rs, i) -> new TrackedSite(
            rs.getLong("id"), rs.getString("name"), rs.getString("ga_property_id"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTrackedSiteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TrackedSite> findAllWithAnalytics() {
        return jdbc.query(BASE_SELECT + " ORDER BY s.id", MAPPER);
    }

    @Override
    public Optional<TrackedSite> findById(Long siteId) {
        return jdbc.query(BASE_SELECT + " AND s.id = :id", new MapSqlParameterSource("id", siteId), MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public void markSyncSuccess(Long siteId) {
        jdbc.update("""
                UPDATE site
                SET ga_access_status = 'OK', ga_last_sync_at = now(), ga_last_sync_error = NULL
                WHERE id = :id
                """, new MapSqlParameterSource("id", siteId));
    }

    @Override
    public void markSyncFailure(Long siteId, AnalyticsAccessStatus status, String error) {
        jdbc.update("""
                UPDATE site
                SET ga_access_status = :status, ga_last_sync_error = :error
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", siteId)
                .addValue("status", status.name())
                .addValue("error", error));
    }
}
