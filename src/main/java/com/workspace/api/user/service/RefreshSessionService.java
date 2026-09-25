package com.workspace.api.user.service;

import com.workspace.api.user.domain.RefreshToken;
import com.workspace.api.user.dto.request.LoginRequest;
import com.workspace.api.user.dto.request.RegisterRequest;
import com.workspace.api.user.dto.response.AuthResponse;
import com.workspace.api.user.exception.RefreshRejectedException;
import com.workspace.api.user.repository.RefreshTokenRepository;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.user.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshSessionService {
    private final UserService users;
    private final UserRepository accounts;
    private final RefreshTokenRepository tokens;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final Duration maxAge;
    private final Duration idleTimeout;
    private final SecureRandom random = new SecureRandom();

    public RefreshSessionService(UserService users,
                                 UserRepository accounts,
                                 RefreshTokenRepository tokens,
                                 CurrentUser currentUser, Clock clock,
                                 @Value("${app.refresh.max-age:P90D}")
                                 Duration maxAge,
                                 @Value("${app.refresh.idle-timeout:P30D}")
                                 Duration idleTimeout
    )
    {
        if (maxAge.isNegative() || maxAge.isZero() || maxAge.compareTo(Duration.ofDays(90)) > 0
                || idleTimeout.isNegative() || idleTimeout.isZero() || idleTimeout.compareTo(maxAge) > 0) {
            throw new IllegalArgumentException("Refresh lifetimes must be positive, idle <= maximum <= 90 days");
        }
        this.users = users;
        this.accounts = accounts;
        this.tokens = tokens;
        this.currentUser = currentUser;
        this.clock = clock;
        this.maxAge = maxAge;
        this.idleTimeout = idleTimeout;
    }

    // This internal result must never be used as a controller response or logged.
    public static final class IssuedSession {
        public final AuthResponse auth;
        public final String cookieValue;
        public final Duration cookieLifetime;

        IssuedSession(AuthResponse auth, String cookieValue, Duration cookieLifetime) {
            this.auth = auth;
            this.cookieValue = cookieValue;
            this.cookieLifetime = cookieLifetime;
        }
    }

    @Transactional
    public IssuedSession login(LoginRequest request, String previousCookie) {
        AuthResponse auth = users.login(request);
        revokePrevious(previousCookie);
        accounts.lockById(auth.user().id()).orElseThrow(RefreshRejectedException::new);
        return issue(auth, UUID.randomUUID().toString(), clock.instant().plus(maxAge));
    }

    @Transactional
    public IssuedSession register(RegisterRequest request, String previousCookie) {
        revokePrevious(previousCookie);
        AuthResponse auth = users.register(request);
        return issue(auth, UUID.randomUUID().toString(), clock.instant().plus(maxAge));
    }

    // Commit revocation even though the HTTP result is 401. Rolling back here would allow reuse.
    @Transactional(noRollbackFor = RefreshRejectedException.class)
    public IssuedSession refresh(String rawToken) {
        RefreshToken token = lockedToken(rawToken);
        if (token == null) throw new RefreshRejectedException();
        Instant now = clock.instant();
        if (token.getUsedAt() != null || token.getRevokedAt() != null
                || !now.isBefore(token.getExpiresAt()) || !now.isBefore(token.getAbsoluteExpiresAt())) {
            tokens.revokeFamily(token.getFamilyId(), now);
            throw new RefreshRejectedException();
        }
        var user = accounts.findById(token.getUserId()).orElseThrow(RefreshRejectedException::new);
        if (!user.isEnabled()) {
            tokens.revokeUser(user.getId(), now);
            throw new RefreshRejectedException();
        }
        token.setUsedAt(now);
        return issue(users.token(user), token.getFamilyId(), token.getAbsoluteExpiresAt());
    }

    @Transactional
    public void logout(String rawToken) { revokePrevious(rawToken); }

    @Transactional
    public void logoutAll() {
        String userId = currentUser.id();
        accounts.lockById(userId).orElseThrow(RefreshRejectedException::new);
        tokens.revokeUser(userId, clock.instant());
    }

    private void revokePrevious(String rawToken) {
        RefreshToken token = lockedToken(rawToken);
        if (token != null) tokens.revokeFamily(token.getFamilyId(), clock.instant());
    }

    private RefreshToken lockedToken(String rawToken) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) return null;
        String hash = hash(rawToken);
        String owner = tokens.findOwner(hash).orElse(null);
        if (owner == null || accounts.lockById(owner).isEmpty()) return null;
        // Every issue/rotation/revocation for an account uses this same database lock.
        return tokens.findByTokenHash(hash).orElse(null);
    }

    private IssuedSession issue(AuthResponse auth, String family, Instant absoluteExpiry) {
        Instant now = clock.instant();
        Instant expiry = now.plus(idleTimeout);
        if (expiry.isAfter(absoluteExpiry)) expiry = absoluteExpiry;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(auth.user().id());
        token.setFamilyId(family);
        token.setTokenHash(hash(raw));
        token.setCreatedAt(now);
        token.setExpiresAt(expiry);
        token.setAbsoluteExpiresAt(absoluteExpiry);
        tokens.save(token);

        return new IssuedSession(auth, raw, Duration.between(now, expiry));
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
