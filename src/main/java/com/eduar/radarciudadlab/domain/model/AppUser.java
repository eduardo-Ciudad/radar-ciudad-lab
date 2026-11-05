package com.eduar.radarciudadlab.domain.model;

/**
 * tokenVersion vai no claim "ver" do JWT. Somar 1 nela revoga todos os tokens já emitidos para o usuário
 * (logout, desativação), porque o ActiveUserTokenValidator exige que as duas sejam iguais.
 */
public record AppUser(Long id, String email, String passwordHash, UserRole role, boolean active, int tokenVersion) {}