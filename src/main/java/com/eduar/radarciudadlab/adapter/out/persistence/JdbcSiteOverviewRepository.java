package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.AnalyticsAccessStatus;
import com.eduar.radarciudadlab.domain.model.SiteOverview;
import com.eduar.radarciudadlab.domain.model.SiteType;
import com.eduar.radarciudadlab.domain.port.out.SiteOverviewRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public class JdbcSiteOverviewRepository implements SiteOverviewRepository {

    private static final String BASE_SELECT = """
            SELECT s.id, s.name, s.url, s.client_id, c.name AS client_name,
                   s.ga_property_id IS NOT NULL AS ga_configured,
                   s.ga_access_status, s.ga_last_sync_at, s.ga_last_sync_error,
                   (SELECT MIN(m.date) FROM ga_daily_metrics m WHERE m.site_id = s.id) AS data_since
            FROM site s
            LEFT JOIN client c ON c.id = s.client_id
            WHERE (c.id IS NULL OR c.active)
            """;

    private static final RowMapper<SiteOverview> MAPPER = (rs, i) -> {
        OffsetDateTime lastSync = rs.getObject("ga_last_sync_at", OffsetDateTime.class);
        Date dataSince = rs.getDate("data_since");
        return new SiteOverview(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("url"),
                rs.getObject("client_id") == null ? SiteType.OWN : SiteType.CLIENT,
                rs.getString("client_name"),
                rs.getBoolean("ga_configured"),
                AnalyticsAccessStatus.valueOf(rs.getString("ga_access_status")),
                lastSync == null ? null : lastSync.toInstant(),
                rs.getString("ga_last_sync_error"),
                dataSince == null ? null : dataSince.toLocalDate());
    };

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSiteOverviewRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SiteOverview> findAll() {
        return jdbc.query(BASE_SELECT + " ORDER BY s.id", MAPPER);
    }

    @Override
    public Optional<SiteOverview> findById(Long siteId) {
        return jdbc.query(BASE_SELECT + " AND s.id = :id", new MapSqlParameterSource("id", siteId), MAPPER)
                .stream()
                .findFirst();
    }
}
