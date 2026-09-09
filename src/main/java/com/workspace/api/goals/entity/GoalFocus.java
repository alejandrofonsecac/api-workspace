package com.workspace.api.goals.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_goal_focus")
@Data
public class GoalFocus {
    @Id
    @Column(length = 8)
    private String weekId;
    @Column(nullable = false, length = 255)
    private String focus;
    @Column(nullable = false, length = 2000)
    private String message;
}
