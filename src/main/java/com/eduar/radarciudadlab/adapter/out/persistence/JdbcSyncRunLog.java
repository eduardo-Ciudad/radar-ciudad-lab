package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.QuotaUsage;
import com.eduar.radarciudadlab.domain.port.out.SyncRunLog;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Histórico de execuções do sync na tabela ga_sync_run. */
@Repository
public class JdbcSyncRunLog implements SyncRunLog {

    private static final String FINISH = """
            UPDATE ga_sync_run
            SET finished_at          = now(),
                status               = :status,
                requests_made        = :requests,
                tokens_consumed      = :consumed,
                tokens_remaining_day = :remaining,
                error_message        = :error
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSyncRunLog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Long start(Long siteId, DateRange range) {
        return jdbc.queryForObject("""
                INSERT INTO ga_sync_run (site_id, date_from, date_to, status)
                VALUES (:siteId, :from, :to, 'RUNNING')
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("siteId", siteId)
                .addValue("from", range.from())
                .addValue("to", range.to()), Long.class);
    }

    @Override
    public void finishSuccess(Long runId, int requestsMade, QuotaUsage quota) {
        finish(runId, "SUCCESS", requestsMade, quota, null);
    }

    @Override
    public void finishFailure(Long runId, int requestsMade, QuotaUsage quota, String error) {
        finish(runId, "FAILED", requestsMade, quota, error);
    }

    private void finish(Long runId, String status, int requestsMade, QuotaUsage quota, String error) {
        jdbc.update(FINISH, new MapSqlParameterSource()
                .addValue("id", runId)
                .addValue("status", status)
                .addValue("requests", requestsMade)
                .addValue("consumed", quota.tokensConsumed())
                .addValue("remaining", quota.tokensRemainingToday())
                .addValue("error", error));
    }
}
