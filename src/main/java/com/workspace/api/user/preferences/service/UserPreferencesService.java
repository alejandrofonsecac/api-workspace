package com.workspace.api.user.preferences.service;

import com.workspace.api.user.preferences.domain.UserPreferences;
import com.workspace.api.user.preferences.dto.UserPreferencesResponse;
import com.workspace.api.user.preferences.repository.UserPreferencesRepository;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda as preferências de interface de cada conta como um documento JSON. O frontend
 * define as chaves, os valores e os padrões; o backend garante o dono, a junção das
 * alterações e os limites. Veja .docs/preferences/README.md.
 */
@Service
@RequiredArgsConstructor
public class UserPreferencesService {
    private final UserPreferencesRepository preferences;
    private final UserRepository users;
    private final PreferencesDocumentPolicy policy;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserPreferencesResponse current() {
        return preferences.findById(currentUser.id())
                .map(UserPreferencesResponse::new)
                .orElse(UserPreferencesResponse.EMPTY);
    }

    /**
     * Junta as chaves recebidas ao documento salvo: cada chave substitui o valor anterior
     * por inteiro e `null` remove a chave. Chaves não enviadas são preservadas, então um
     * cliente antigo não apaga preferências que não conhece.
     */
    @Transactional
    public UserPreferencesResponse merge(Map<String, Object> changes) {
        policy.validateChanges(changes);
        String userId = currentUser.id();
        // Serializa gravações da mesma conta (dois dispositivos ao mesmo tempo, inclusive a primeira).
        users.lockById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UserPreferences stored = preferences.findById(userId).orElseGet(() -> {
            UserPreferences created = new UserPreferences();
            created.setUserId(userId);
            return created;
        });
        if (changes.isEmpty()) {
            return stored.getUpdatedAt() == null ? UserPreferencesResponse.EMPTY : new UserPreferencesResponse(stored);
        }

        Map<String, Object> document = new LinkedHashMap<>(stored.getData());
        changes.forEach((key, value) -> {
            if (value == null) document.remove(key);
            else document.put(key, value);
        });
        policy.validateDocument(document);

        stored.setData(document);
        stored.setUpdatedAt(Instant.now(clock));
        return new UserPreferencesResponse(preferences.saveAndFlush(stored));
    }
}
