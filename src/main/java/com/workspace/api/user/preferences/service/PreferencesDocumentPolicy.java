package com.workspace.api.user.preferences.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Limites estruturais do documento de preferências. O backend não conhece o significado
 * das chaves; ele apenas impede que o campo seja usado para guardar dados arbitrários.
 */
@Component
public class PreferencesDocumentPolicy {
    static final int MAX_KEYS = 100;
    static final int MAX_DEPTH = 3;
    static final int MAX_STRING_LENGTH = 500;
    static final int MAX_BYTES = 16 * 1024;
    private static final Pattern KEY = Pattern.compile("[a-z][A-Za-z0-9]{0,63}");

    private final ObjectMapper json;

    public PreferencesDocumentPolicy(ObjectMapper json) {
        this.json = json;
    }

    /** Valida as alterações recebidas antes da junção com o documento salvo. */
    public void validateChanges(Map<String, Object> changes) {
        if (changes == null) {
            throw invalid("Preferences must be a JSON object");
        }
        changes.forEach((key, value) -> {
            requireKey(key);
            if (value != null) {
                requireValue(key, value, 1);
            }
        });
    }

    /** Valida o documento final, depois da junção. */
    public void validateDocument(Map<String, Object> document) {
        if (document.size() > MAX_KEYS) {
            throw invalid("Preferences must have at most " + MAX_KEYS + " keys");
        }
        if (json.writeValueAsString(document).getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw invalid("Preferences must have at most " + MAX_BYTES + " bytes");
        }
    }

    private void requireValue(String path, Object value, int depth) {
        if (value == null || value instanceof Boolean || value instanceof Number) return;
        if (value instanceof String text) {
            if (text.length() > MAX_STRING_LENGTH) {
                throw invalid(path + ": strings must have at most " + MAX_STRING_LENGTH + " characters");
            }
            return;
        }
        if (depth > MAX_DEPTH) {
            throw invalid(path + ": nesting deeper than " + MAX_DEPTH + " levels");
        }
        if (value instanceof Map<?, ?> object) {
            object.forEach((key, child) -> {
                requireKey(String.valueOf(key));
                requireValue(path + "." + key, child, depth + 1);
            });
            return;
        }
        if (value instanceof Collection<?> array) {
            int index = 0;
            for (Object child : array) {
                requireValue(path + "[" + index++ + "]", child, depth + 1);
            }
            return;
        }
        throw invalid(path + ": unsupported value");
    }

    private void requireKey(String key) {
        if (!KEY.matcher(key).matches()) {
            throw invalid("Invalid preference key: keys must be camelCase with at most 64 characters");
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
