package com.workspace.api.user.preferences.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Preferências de interface de um usuário; a chave primária é o próprio id do usuário. */
@Entity
@Table(name = "tb_user_preferences")
@Getter
@Setter
public class UserPreferences {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, length = 20)
    private String theme;

    @Column(nullable = false, length = 20)
    private String accent;

    @Column(name = "planner_display", nullable = false, length = 20)
    private String plannerDisplay;

    @Column(name = "sleep_enabled", nullable = false)
    private boolean sleepEnabled;

    @Column(name = "sleep_start", nullable = false)
    private short sleepStart;

    @Column(name = "sleep_end", nullable = false)
    private short sleepEnd;

    @Column(name = "sidebar_width", nullable = false)
    private short sidebarWidth;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
