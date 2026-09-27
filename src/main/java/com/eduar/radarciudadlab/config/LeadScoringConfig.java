package com.eduar.radarciudadlab.config;

import com.eduar.radarciudadlab.domain.model.lead.LeadScorer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/** O LeadScorer é Java puro (domínio); aqui ele vira bean com o nicho vindo da configuração. */
@Configuration
@EnableConfigurationProperties(LeadScoringProperties.class)
public class LeadScoringConfig {

    @Bean
    public LeadScorer leadScorer(LeadScoringProperties properties) {
        return new LeadScorer(Set.copyOf(properties.targetKeywords()));
    }
}
