package com.rbctcsworld.ecommerce.auth;

import com.rbctcsworld.ecommerce.common.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for the login endpoint.
 *
 *  - After MAX failed logins for one e-mail within the WINDOW, that e-mail is locked for LOCK time:
 *    every login (even with the right password) gets 429 + Retry-After, so guessing stops.
 *  - Unknown e-mails are counted exactly like real ones, so the 429 does not reveal which accounts exist.
 *  - A successful login clears the counter.
 *  - Kept in memory (one backend instance). With several instances this would move to Redis.
 *
 * Trade-off (documented): someone who knows a customer's e-mail can lock that account for 15 minutes.
 * A temporary lock (not a permanent one) keeps that risk small.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED = 10_000;

    private final Clock clock;
    private final int maxFailures;
    private final Duration window;
    private final Duration lock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock,
                               @Value("${app.security.login.max-failures:5}") int maxFailures,
                               @Value("${app.security.login.window-minutes:15}") long windowMinutes,
                               @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.window = Duration.ofMinutes(windowMinutes);
        this.lock = Duration.ofMinutes(lockMinutes);
    }

    private record Attempts(int failures, Instant firstFailure, Instant lockedUntil) {
    }

    /** Throws 429 while the e-mail is locked. Call BEFORE checking the password. */
    public void checkAllowed(String email) {
        Attempts a = attempts.get(email);
        Instant now = clock.instant();
        if (a != null && a.lockedUntil() != null && now.isBefore(a.lockedUntil())) {
            long seconds = Math.max(1, Duration.between(now, a.lockedUntil()).toSeconds());
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Try again in " + seconds + " seconds.", seconds);
        }
    }

    public void loginFailed(String email) {
        Instant now = clock.instant();
        if (attempts.size() > MAX_TRACKED) evictStale(now);
        attempts.compute(email, (k, a) -> {
            if (a == null || now.isAfter(a.firstFailure().plus(window))
                    || (a.lockedUntil() != null && !now.isBefore(a.lockedUntil()))) {
                a = new Attempts(0, now, null);                 // new window (or lock has expired)
            }
            int failures = a.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lock) : null;
            return new Attempts(failures, a.firstFailure(), lockedUntil);
        });
    }

    public void loginSucceeded(String email) {
        attempts.remove(email);
    }

    int failures(String email) {
        Attempts a = attempts.get(email);
        return a == null ? 0 : a.failures();
    }

    private void evictStale(Instant now) {
        attempts.entrySet().removeIf(e -> {
            Attempts a = e.getValue();
            boolean windowOver = now.isAfter(a.firstFailure().plus(window));
            boolean notLocked = a.lockedUntil() == null || !now.isBefore(a.lockedUntil());
            return windowOver && notLocked;
        });
    }
}
