package com.eduar.radarciudadlab.domain.model;

public record AppUser(Long id, String email, String passwordHash, UserRole role, boolean active) {}