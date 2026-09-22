package com.workspace.api.user.service;

import com.workspace.api.user.domain.User;
import com.workspace.api.user.dto.request.LoginRequest;
import com.workspace.api.user.dto.request.RegisterRequest;
import com.workspace.api.user.dto.response.AuthResponse;
import com.workspace.api.user.dto.response.UserResponse;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.user.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Cadastra usuários, verifica senhas e emite tokens após autenticação.
 * PasswordEncoder cuida do hash da senha; JwtEncoder cuida da assinatura do token.
 * São responsabilidades distintas. Veja .docs/authentication/JWT_CONFIGURATION.md.
 */
@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CurrentUser currentUser;
    private final String issuer;
    private final Duration ttl;
    private final String dummyHash;

    // O Spring fornece os beans definidos em SecurityConfiguration e JwtConfiguration.
    public UserService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, CurrentUser currentUser,
                       @Value("${app.jwt.issuer}") String issuer,
                       @Value("${app.jwt.ttl:PT15M}") Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("JWT TTL must be positive");
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.currentUser = currentUser;
        this.issuer = issuer;
        this.ttl = ttl;

        // Também executa BCrypt quando o e-mail não existe, reduzindo a diferença
        // de tempo entre "usuário inexistente" e "senha incorreta". Não cria uma conta.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest request) {
        validatePasswordBytes(request.password());
        String email = normalizeEmail(request.email());

        if (users.existsByEmail(email)) throw duplicateEmail();

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }

        return token(user);
    }

    public AuthResponse login(LoginRequest request) {
        validatePasswordBytes(request.password());
        User user = users.findByEmail(normalizeEmail(request.email()))
                .orElse(null);

        boolean matches = passwordEncoder.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());

        if (user == null || !matches || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return token(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return new UserResponse(users.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    /**
     * Emite um token assinado para o usuário já autenticado. O subject (sub) é
     * seu id; exp limita a validade. Nunca inclua senha ou hash nestes dados.
     */
    public AuthResponse token(User user) {
        Instant now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name()).build();

        String value = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new AuthResponse(value, "Bearer", ttl.toSeconds(), new UserResponse(user));
    }

    //--Validações
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at most 72 UTF-8 bytes");
        }
    }

    private ResponseStatusException duplicateEmail() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
}
