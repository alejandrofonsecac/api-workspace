package com.workspace.api.user.preferences.service;

import com.workspace.api.user.preferences.domain.UserPreferences;
import com.workspace.api.user.preferences.dto.UserPreferencesRequest;
import com.workspace.api.user.preferences.dto.UserPreferencesResponse;
import com.workspace.api.user.preferences.repository.UserPreferencesRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;

/**
 * Preferências de interface por conta. Ficam no servidor para acompanhar o usuário entre
 * dispositivos; o navegador mantém apenas uma cópia em cache. Veja .docs/preferences/README.md.
 */
@Service
@RequiredArgsConstructor
public class UserPreferencesService {
    private final UserPreferencesRepository preferences;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserPreferencesResponse current() {
        return preferences.findById(currentUser.id())
                .map(UserPreferencesResponse::new)
                .orElse(UserPreferencesResponse.DEFAULTS);
    }

    /** Substitui todas as preferências; cria a linha na primeira gravação. */
    @Transactional
    public UserPreferencesResponse replace(UserPreferencesRequest request) {
        String userId = currentUser.id();
        UserPreferences stored = preferences.findById(userId).orElseGet(() -> {
            UserPreferences created = new UserPreferences();
            created.setUserId(userId);
            return created;
        });
        stored.setTheme(request.theme());
        stored.setAccent(request.accent());
        stored.setPlannerDisplay(request.plannerDisplay());
        stored.setSleepEnabled(request.sleep().enabled());
        stored.setSleepStart(request.sleep().start().shortValue());
        stored.setSleepEnd(request.sleep().end().shortValue());
        stored.setSidebarWidth(request.sidebarWidth().shortValue());
        stored.setUpdatedAt(Instant.now(clock));
        try {
            return new UserPreferencesResponse(preferences.saveAndFlush(stored));
        } catch (DataIntegrityViolationException exception) {
            // Duas primeiras gravações simultâneas: a chave primária garante uma única linha.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Preferences were updated concurrently");
        }
    }
}
