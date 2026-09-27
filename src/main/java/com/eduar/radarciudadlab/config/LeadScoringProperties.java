package com.eduar.radarciudadlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Nicho-alvo do score de leads (radar.leads.score.target-keywords).
 * Palavras comparadas sem acento contra categoria e nome: "fisioterap" pega Fisioterapia e Fisioterapeuta.
 */
@ConfigurationProperties(prefix = "radar.leads.score")
public record LeadScoringProperties(
        @DefaultValue({"fisioterap", "pilates", "quiropraxia", "osteopatia", "reabilitacao", "policlinica", "clinica"})
        List<String> targetKeywords
) {}
