package com.workspace.api.user.preferences.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Documento de preferências de interface de um usuário. As chaves pertencem ao
 * frontend; o backend só garante dono, formato e limites.
 */
@Entity
@Table(name = "tb_user_preferences")
@Getter
@Setter
public class UserPreferences {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> data = new LinkedHashMap<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
