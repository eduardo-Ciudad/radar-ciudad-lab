package com.eduar.radarciudadlab.domain.model.lead;

import java.math.BigDecimal;
import java.time.Instant;

/** Nota do lead numa importação (lead_snapshot). */
public record RatingPoint(Long batchId, BigDecimal rating, Integer reviewsCount, Instant capturedAt) {}
