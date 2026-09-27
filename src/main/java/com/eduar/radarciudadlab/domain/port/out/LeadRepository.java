package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.lead.Lead;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Porta de saída: escrita dos leads (e leitura do agregado inteiro para editar). */
public interface LeadRepository {

    /** Busca de uma vez todos os leads cujas chaves aparecem no arquivo (evita uma consulta por linha). */
    List<Lead> findByDedupKeys(Collection<String> dedupKeys);

    Optional<Lead> findById(Long id);

    /** Todos os leads (usado para recalcular o score). */
    List<Lead> findAll();

    /** Insere (id nulo) ou atualiza, já gravando no banco. Devolve o lead com id preenchido. */
    Lead save(Lead lead);

    /** Guarda a nota do lead nesta importação (histórico para o gráfico de evolução). */
    void saveSnapshot(Long leadId, Long batchId, BigDecimal rating, Integer reviewsCount);

    /** Registra a mudança de etapa no funil. from nulo = entrada do lead. */
    void recordStatusChange(Long leadId, LeadStatus from, LeadStatus to, String note);
}
