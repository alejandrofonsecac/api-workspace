package com.workspace.api.goals.entity;

import com.workspace.api.habit.entity.Category;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.util.domain.Priority;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Table(name = "tb_goals")
@Entity
@Data
public class Goals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String goalName;
    @Column(nullable = false)
    private String expectedTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 8)
    private String weekId;

    private Instant createdAt;
    private Instant completedAt;

    @Column(columnDefinition = "integer default 0")
    private int deferredCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id")
    private Habit habit;
}
