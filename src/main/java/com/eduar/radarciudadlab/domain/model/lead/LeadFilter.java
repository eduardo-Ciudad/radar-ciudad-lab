package com.eduar.radarciudadlab.domain.model.lead;

/**
 * Filtros da listagem. Todos opcionais (null = não filtra).
 * withoutWebsite = só leads sem site cadastrado; mobile = só com celular; search = nome ou telefone.
 */
public record LeadFilter(
        LeadStatus status,
        String category,
        String neighborhood,
        Boolean withoutWebsite,
        Boolean mobile,
        Integer minScore,
        String search
) {

    public static LeadFilter none() {
        return new LeadFilter(null, null, null, null, null, null, null);
    }

    public LeadFilter withStatus(LeadStatus newStatus) {
        return new LeadFilter(newStatus, category, neighborhood, withoutWebsite, mobile, minScore, search);
    }
}
