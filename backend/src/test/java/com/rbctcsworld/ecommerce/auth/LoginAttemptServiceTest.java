package com.rbctcsworld.ecommerce.auth;

import com.rbctcsworld.ecommerce.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    /** A clock the test can move forward. */
    static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-08-01T10:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    private static final String EMAIL = "victim@test.com";
    private TestClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        service = new LoginAttemptService(clock, 5, 15, 15);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) service.loginFailed(EMAIL);
    }

    @Test
    void fourFailuresAreStillAllowed() {
        fail(4);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void fifthFailureLocksForFifteenMinutesWithRetryAfter() {
        fail(5);
        assertThatThrownBy(() -> service.checkAllowed(EMAIL))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(e -> assertThat(((TooManyRequestsException) e).getRetryAfterSeconds()).isEqualTo(900));

        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThatThrownBy(() -> service.checkAllowed(EMAIL)).isInstanceOf(TooManyRequestsException.class);

        clock.advance(Duration.ofSeconds(1));                        // exactly 15 minutes later
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void retryAfterIsRoundedUpSoWaitingThatLongIsEnough() {
        fail(5);
        clock.advance(Duration.ofMillis(300));                       // 899.7 s left
        assertThatThrownBy(() -> service.checkAllowed(EMAIL))
                .satisfies(e -> assertThat(((TooManyRequestsException) e).getRetryAfterSeconds()).isEqualTo(900));
    }

    @Test
    void afterTheLockExpiresCountingStartsAgain() {
        fail(5);
        clock.advance(Duration.ofMinutes(15));
        service.loginFailed(EMAIL);
        assertThat(service.failures(EMAIL)).isEqualTo(1);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void failuresSpreadOverMoreThanTheWindowDoNotLock() {
        fail(4);
        clock.advance(Duration.ofMinutes(16));                       // window over -> counter restarts
        service.loginFailed(EMAIL);
        assertThat(service.failures(EMAIL)).isEqualTo(1);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheCounter() {
        fail(4);
        service.loginSucceeded(EMAIL);
        fail(4);
        assertThatCode(() -> service.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void otherAccountsAreNotAffected() {
        fail(5);
        assertThatCode(() -> service.checkAllowed("someone.else@test.com")).doesNotThrowAnyException();
    }
}
