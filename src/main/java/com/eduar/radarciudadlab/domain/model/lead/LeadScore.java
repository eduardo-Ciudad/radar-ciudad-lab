package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/** Score calculado (0 a 100) com a explicação de cada critério. */
public record LeadScore(int total, List<ScoreItem> items) {}
