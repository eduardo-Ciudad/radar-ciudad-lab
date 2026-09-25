package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.BreakdownTotal;
import com.eduar.radarciudadlab.domain.model.DailyBreakdown;
import com.eduar.radarciudadlab.domain.model.DailyMetrics;
import com.eduar.radarciudadlab.domain.model.DateRange;
import com.eduar.radarciudadlab.domain.model.PeriodUsers;

import java.util.List;

/** Porta de saída: o cache local das métricas (PostgreSQL). */
public interface AnalyticsMetricsRepository {

    /** Insere ou atualiza os totais diários (um registro por site + dia). */
    void upsertDailyMetrics(Long siteId, List<DailyMetrics> metrics);

    /**
     * Substitui as quebras de uma dimensão no intervalo: apaga o que havia e grava o novo.
     * Upsert não basta aqui: se um valor sumir no reprocessamento, a linha antiga ficaria órfã.
     */
    void replaceDailyBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range,
                                List<DailyBreakdown> breakdowns);

    void upsertPeriodUsers(Long siteId, PeriodUsers users);

    List<DailyMetrics> findDailyMetrics(Long siteId, DateRange range);

    /** Soma o período por valor da dimensão e devolve os N maiores. */
    List<BreakdownTotal> findTopBreakdowns(Long siteId, BreakdownDimension dimension, DateRange range, int limit);
}
