package com.eduar.radarciudadlab.domain.model.lead;

import java.time.Instant;

/** Mudança de etapa no funil (lead_status_history). from nulo = entrada do lead no funil. */
public record StatusChange(LeadStatus from, LeadStatus to, Instant changedAt, String note) {}
