package com.eduar.radarciudadlab.config.security;

import java.time.Duration;

/**
 * Login recusado sem nem conferir a senha: o e-mail estourou o limite de falhas (LoginAttemptLimiter).
 * Não estende AuthenticationException de propósito: vira 429 no ApiExceptionHandler, não 401.
 */
public class LoginBlockedException extends RuntimeException {

    private final Duration retryAfter;

    public LoginBlockedException(Duration retryAfter) {
        super("Login temporariamente bloqueado por excesso de tentativas");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    /** Valor do header Retry-After: segundos arredondados para cima, nunca menos que 1. */
    public long retryAfterSeconds() {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
