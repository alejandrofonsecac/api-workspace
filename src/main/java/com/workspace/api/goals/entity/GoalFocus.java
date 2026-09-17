package com.workspace.api.goals.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_goal_focus")
@Data
@IdClass(GoalFocusId.class)
public class GoalFocus {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private com.workspace.api.user.domain.User owner;

    @Id
    @Column(length = 8)
    private String weekId;
    @Column(nullable = false, length = 255)
    private String focus;
    @Column(nullable = false, length = 2000)
    private String message;
}
