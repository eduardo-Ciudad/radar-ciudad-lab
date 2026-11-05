package com.eduar.radarciudadlab.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Teste unitário do limite de login: sem Spring, relógio controlado na mão. */
class LoginAttemptLimiterTest {

    private static final String EMAIL = "admin@teste.local";
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private MutableClock clock;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-05T18:00:00Z"));
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("nao-usado", Duration.ofHours(12), "radar-ciudadlab"),
                new SecurityProperties.Cors(List.of()),
                new SecurityProperties.Admin(null, null),
                new SecurityProperties.Login(5, WINDOW, LOCK));
        limiter = new LoginAttemptLimiter(properties, clock);
    }

    @Test
    void quatroFalhasNaoBloqueiam() {
        fail(4);

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL));
    }

    @Test
    void quintaFalhaBloqueiaPeloTempoConfigurado() {
        fail(5);

        LoginBlockedException e = assertThrows(LoginBlockedException.class, () -> limiter.checkAllowed(EMAIL));
        assertEquals(LOCK, e.getRetryAfter());
        assertEquals(LOCK.toSeconds(), e.retryAfterSeconds());
    }

    @Test
    void tempoRestanteDiminuiComOPassarDoTempo() {
        fail(5);
        clock.advance(Duration.ofMinutes(10));

        LoginBlockedException e = assertThrows(LoginBlockedException.class, () -> limiter.checkAllowed(EMAIL));
        assertEquals(Duration.ofMinutes(5), e.getRetryAfter());
    }

    @Test
    void depoisDoBloqueioVoltaALiberarEComecaContagemNova() {
        fail(5);
        clock.advance(LOCK);

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL));

        fail(4);
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL), "a contagem recomeça do zero depois do bloqueio");
    }

    @Test
    void sucessoZeraOContador() {
        fail(4);
        limiter.recordSuccess(EMAIL);
        fail(4);

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL));
    }

    @Test
    void falhasForaDaJanelaNaoAcumulam() {
        fail(4);
        clock.advance(WINDOW);
        fail(4);

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL));
    }

    @Test
    void emailComMaiusculasEEspacosContaComoAMesmaConta() {
        fail(3);
        limiter.recordFailure("  ADMIN@Teste.Local ");
        limiter.recordFailure("Admin@teste.local");

        assertThrows(LoginBlockedException.class, () -> limiter.checkAllowed(EMAIL));
    }

    @Test
    void bloqueioDeUmEmailNaoAfetaOutro() {
        fail(5);

        assertDoesNotThrow(() -> limiter.checkAllowed("outro@teste.local"));
    }

    @Test
    void limpezaRemoveSoOsVencidos() {
        fail(2);                                       // janela vence em 15 min
        clock.advance(Duration.ofMinutes(10));
        limiter.recordFailure("recente@teste.local");  // janela vence em 25 min
        clock.advance(Duration.ofMinutes(5));          // agora: 15 min

        limiter.purgeExpired();

        assertEquals(1, limiter.trackedCount());
    }

    @Test
    void limpezaMantemBloqueioAtivo() {
        fail(5);
        clock.advance(Duration.ofMinutes(14));

        limiter.purgeExpired();

        assertEquals(1, limiter.trackedCount());
        assertThrows(LoginBlockedException.class, () -> limiter.checkAllowed(EMAIL));
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(EMAIL);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
