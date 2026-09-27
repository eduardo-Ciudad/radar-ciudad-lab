package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/**
 * Funil completo numa chamada: as 5 colunas ativas (NOVO a FECHADO) e os totais do cabeçalho.
 * outOfFunnel = PERDIDO + DESCARTADO, que não viram coluna (listados em GET /api/leads?status=...).
 */
public record LeadBoard(List<BoardColumn> columns, long active, long outOfFunnel, long lost, long discarded) {}
