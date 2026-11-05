package com.eduar.radarciudadlab.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Limite de tentativas de login por conta (e-mail digitado).
 *
 * Por conta e não por IP: atrás do BFF, todo login chega do IP do servidor Next.js. O limite por IP
 * fica no proxy reverso, que enxerga o visitante real.
 *
 * E-mail inexistente também é contado e bloqueado, para o 429 não revelar quais contas existem.
 * Em memória: o Radar roda numa instância só. Com mais de uma, trocar por Redis.
 */
@Component
public class LoginAttemptLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptLimiter.class);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final SecurityProperties.Login config;
    private final Clock clock;

    public LoginAttemptLimiter(SecurityProperties properties, Clock clock) {
        this.config = properties.login();
        this.clock = clock;
    }

    /** Chamado antes de conferir a senha: bloqueado não chega a testar senha nenhuma. */
    public void checkAllowed(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null) {
            return;
        }
        Instant now = clock.instant();
        if (current.isLockedAt(now)) {
            throw new LoginBlockedException(Duration.between(now, current.lockedUntil()));
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        boolean[] newlyLocked = {false};

        Attempts updated = attempts.compute(key(email), (k, current) -> {
            boolean fresh = current == null || current.isExpiredAt(now, config.window());
            int failures = fresh ? 1 : current.failures() + 1;
            Instant windowStart = fresh ? now : current.windowStart();
            if (failures >= config.maxFailures()) {
                newlyLocked[0] = fresh || current.lockedUntil() == null;
                return new Attempts(failures, windowStart, now.plus(config.lockDuration()));
            }
            return new Attempts(failures, windowStart, null);
        });

        if (newlyLocked[0]) {
            log.warn("Login bloqueado por {} para '{}' após {} falhas", config.lockDuration(), key(email),
                    updated.failures());
        }
    }

    /** Login certo zera o histórico daquela conta. */
    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    /** Sem limpeza, milhares de e-mails aleatórios fariam o mapa crescer sem limite. */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void purgeExpired() {
        Instant now = clock.instant();
        attempts.values().removeIf(a -> a.isExpiredAt(now, config.window()));
    }

    /** Quantos e-mails estão sendo acompanhados (para os testes da limpeza). */
    int trackedCount() {
        return attempts.size();
    }

    /** Mesma regra do índice lower(email) do banco: Admin@x.com e admin@x.com são a mesma conta. */
    static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** lockedUntil == null: ainda abaixo do limite, contando falhas dentro da janela. */
    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {

        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpiredAt(Instant now, Duration window) {
            if (lockedUntil != null) {
                return !now.isBefore(lockedUntil);
            }
            return !now.isBefore(windowStart.plus(window));
        }
    }
}
