package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.PeriodUsers;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsMetricsRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Cache das métricas do GA no PostgreSQL, via JDBC.
 * JDBC em vez de JPA: são gravações em lote com upsert (INSERT ... ON CONFLICT) e leituras agregadas.
 */
@Repository
public class JdbcAnalyticsMetricsRepository implements AnalyticsMetricsRepository {

    private static final String UPSERT_DAILY = """
            INSERT INTO ga_daily_metrics (site_id, date, sessions, engaged_sessions, new_users, active_users,
                                          page_views, event_count, key_events, engagement_duration_seconds, synced_at)
            VALUES (:siteId, :date, :sessions, :engagedSessions, :newUsers, :activeUsers,
                    :pageViews, :eventCount, :keyEvents, :engagementSeconds, now())
            ON CONFLICT (site_id, date) DO UPDATE SET
                sessions                    = EXCLUDED.sessions,
                engaged_sessions            = EXCLUDED.engaged_sessions,
                new_users                   = EXCLUDED.new_users,
                active_users                = EXCLUDED.active_users,
                page_views                  = EXCLUDED.page_views,
                event_count                 = EXCLUDED.event_count,
                key_events                  = EXCLUDED.key_events,
                engagement_duration_seconds = EXCLUDED.engagement_duration_seconds,
                synced_at                   = now()
            """;

    private static final String DELETE_BREAKDOWNS = """
            DELETE FROM ga_daily_breakdown
            WHERE site_id = :siteId AND dimension = :dimension AND date BETWEEN :from AND :to
            """;

    // ON CONFLICT só como proteção: valores truncados em 500 caracteres podem colidir
    private static final String INSERT_BREAKDOWN = """
            INSERT INTO ga_daily_breakdown (site_id, date, dimension, value, sessions, engaged_sessions,
                                            page_views, event_count, key_events, synced_at)
            VALUES (:siteId, :date, :dimension, :value, :sessions, :engagedSessions,
                    :pageViews, :eventCount, :keyEvents, now())
            ON CONFLICT (site_id, date, dimension, value) DO UPDATE SET
                sessions         = EXCLUDED.sessions,
                engaged_sessions = EXCLUDED.engaged_sessions,
                page_views       = EXCLUDED.page_views,
                event_count      = EXCLUDED.event_count,
                key_events       = EXCLUDED.key_events,
                synced_at        = now()
            """;

    private static final String UPSERT_PERIOD_USERS = """
            INSERT INTO ga_period_users (site_id, period_type, period_start, period_end,
                                         total_users, active_users, new_users, synced_at)
            VALUES (:siteId, :periodType, :periodStart, :periodEnd, :totalUsers, :activeUsers, :newUsers, now())
            ON CONFLICT (site_id, period_type, period_start) DO UPDATE SET
                period_end   = EXCLUDED.period_end,
                total_users  = EXCLUDED.total_users,
                active_users = EXCLUDED.active_users,
                new_users    = EXCLUDED.new_users,
                synced_at    = now()
            """;

    private static final String FIND_DAILY = """
            SELECT date, sessions, engaged_sessions, new_users, active_users, page_views,
                   event_count, key_events, engagement_duration_seconds
            FROM ga_daily_metrics
            WHERE site_id = :siteId AND date BETWEEN :from AND :to
            ORDER BY date
            """;

    private static final String FIND_TOP_BREAKDOWNS = """
            SELECT value,
                   SUM(sessions)         AS sessions,
                   SUM(engaged_sessions) AS engaged_sessions,
                   SUM(page_views)       AS page_views,
                   SUM(event_count)      AS event_count,
                   SUM(key_events)       AS key_events
            FROM ga_daily_breakdown
            WHERE site_id = :siteId AND dimension = :dimension AND date BETWEEN :from AND :to
            GROUP BY value
            ORDER BY SUM(sessions) DESC, SUM(event_count) DESC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAnalyticsMetricsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void upsertDailyMetrics(Long siteId, List<DailyMetrics> metrics) {
        if (metrics.isEmpty()) return;

        SqlParameterSource[] batch = metrics.stream()
                .map(m -> new MapSqlParameterSource()
                        .addValue("siteId", siteId)
                        .addValue("date", m.date())
                        .addValue("sessions", m.sessions())
                        .addValue("engagedSessions", m.engagedSessions())
                        .addValue("newUsers", m.newUsers())
                        .addValue("activeUsers", m.activeUsers())
                        .addValue("pageViews", m.pageViews())
                        .addValue("eventCount", m.eventCount())
                        .addValue("keyEvents", m.keyEvents())
                        .addValue("engagementSeconds", m.engagementSeconds()))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(UPSERT_DAILY, batch);
    }

    @Override
    @Transactional
    public void replaceDailyBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range,
                                       List<DailyBreakdown> breakdowns) {
        jdbc.update(DELETE_BREAKDOWNS, rangeParams(siteId, range).addValue("dimension", dimension.name()));

        if (breakdowns.isEmpty()) return;

        SqlParameterSource[] batch = breakdowns.stream()
                .map(b -> new MapSqlParameterSource()
                        .addValue("siteId", siteId)
                        .addValue("date", b.date())
                        .addValue("dimension", b.dimension().name())
                        .addValue("value", b.value())
                        .addValue("sessions", b.sessions())
                        .addValue("engagedSessions", b.engagedSessions())
                        .addValue("pageViews", b.pageViews())
                        .addValue("eventCount", b.eventCount())
                        .addValue("keyEvents", b.keyEvents()))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(INSERT_BREAKDOWN, batch);
    }

    @Override
    public void upsertPeriodUsers(Long siteId, PeriodUsers users) {
        DateRange range = users.range();
        jdbc.update(UPSERT_PERIOD_USERS, new MapSqlParameterSource()
                .addValue("siteId", siteId)
                .addValue("periodType", periodTypeOf(range))
                .addValue("periodStart", range.from())
                .addValue("periodEnd", range.to())
                .addValue("totalUsers", users.totalUsers())
                .addValue("activeUsers", users.activeUsers())
                .addValue("newUsers", users.newUsers()));
    }

    @Override
    public List<DailyMetrics> findDailyMetrics(Long siteId, DateRange range) {
        return jdbc.query(FIND_DAILY, rangeParams(siteId, range), (rs, i) -> new DailyMetrics(
                rs.getObject("date", LocalDate.class),
                rs.getInt("sessions"),
                rs.getInt("engaged_sessions"),
                rs.getInt("new_users"),
                rs.getInt("active_users"),
                rs.getInt("page_views"),
                rs.getInt("event_count"),
                rs.getInt("key_events"),
                rs.getLong("engagement_duration_seconds")));
    }

    @Override
    public List<BreakdownTotal> findTopBreakdowns(Long siteId, BreakdownDimension dimension,
                                                  DateRange range, int limit) {
        MapSqlParameterSource params = rangeParams(siteId, range)
                .addValue("dimension", dimension.name())
                .addValue("limit", limit);

        return jdbc.query(FIND_TOP_BREAKDOWNS, params, (rs, i) -> new BreakdownTotal(
                dimension,
                rs.getString("value"),
                rs.getLong("sessions"),
                rs.getLong("engaged_sessions"),
                rs.getLong("page_views"),
                rs.getLong("event_count"),
                rs.getLong("key_events")));
    }

    private static MapSqlParameterSource rangeParams(Long siteId, DateRange range) {
        return new MapSqlParameterSource()
                .addValue("siteId", siteId)
                .addValue("from", range.from())
                .addValue("to", range.to());
    }

    /**
     * Por enquanto só guardamos períodos mensais (dia 1 até, no máximo, o fim do mesmo mês).
     * 'WEEK' já existe no CHECK da tabela para quando houver relatório semanal.
     */
    private static String periodTypeOf(DateRange range) {
        boolean startsOnFirst = range.from().getDayOfMonth() == 1;
        boolean sameMonth = range.from().getMonth() == range.to().getMonth()
                && range.from().getYear() == range.to().getYear();
        if (startsOnFirst && sameMonth) return "MONTH";
        throw new IllegalArgumentException("Período de usuários não suportado: " + range);
    }
}
