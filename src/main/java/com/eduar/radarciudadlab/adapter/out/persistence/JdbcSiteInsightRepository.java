package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.InsightSeverity;
import com.eduar.radarciudadlab.domain.model.InsightType;
import com.eduar.radarciudadlab.domain.model.SiteInsight;
import com.eduar.radarciudadlab.domain.port.out.SiteInsightRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcSiteInsightRepository implements SiteInsightRepository {

    private static final String FIND_LATEST = """
            SELECT id, site_id, type, severity, period_start, period_end, title, body, created_at, read_at
            FROM site_insight
            WHERE site_id = :siteId
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """;

    private static final RowMapper<SiteInsight> MAPPER = (rs, i) -> {
        String severity = rs.getString("severity");
        return new SiteInsight(
                rs.getLong("id"),
                rs.getLong("site_id"),
                InsightType.valueOf(rs.getString("type")),
                severity == null ? null : InsightSeverity.valueOf(severity),
                toLocalDate(rs.getDate("period_start")),
                toLocalDate(rs.getDate("period_end")),
                rs.getString("title"),
                rs.getString("body"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("read_at", OffsetDateTime.class)));
    };

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSiteInsightRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SiteInsight> findLatest(Long siteId, int limit) {
        return jdbc.query(FIND_LATEST, new MapSqlParameterSource()
                .addValue("siteId", siteId)
                .addValue("limit", limit), MAPPER);
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
