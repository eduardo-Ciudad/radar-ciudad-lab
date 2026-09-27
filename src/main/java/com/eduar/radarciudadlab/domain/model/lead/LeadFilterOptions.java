package com.eduar.radarciudadlab.domain.model.lead;

import java.util.List;

/** Opções dos selects de Categoria e Bairro, mais frequentes primeiro. */
public record LeadFilterOptions(List<FilterOption> categories, List<FilterOption> neighborhoods) {}
