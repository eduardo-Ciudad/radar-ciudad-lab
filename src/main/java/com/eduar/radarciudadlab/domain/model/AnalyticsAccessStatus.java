package com.eduar.radarciudadlab.domain.model;

/** Situação do acesso ao GA de um site. Espelha o CHECK da coluna site.ga_access_status. */
public enum AnalyticsAccessStatus {
    PENDING,    // cadastrado, nunca sincronizou
    OK,         // último sync funcionou
    NO_ACCESS,  // service account sem permissão (cliente removeu, ID errado)
    ERROR       // outra falha (cota, rede, bug)
}
