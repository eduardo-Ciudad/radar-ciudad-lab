package com.eduar.radarciudadlab.domain.model.lead;

/** Um critério do score: quantos pontos o lead fez de quantos possíveis, e por quê. */
public record ScoreItem(String criterion, String label, int points, int max, String reason) {}
