package com.workspace.api.goals.entity;

import com.workspace.api.habit.entity.Category;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.task.entity.Priority;
import jakarta.persistence.*;
import lombok.Data;

@Table(name = "tb_goals")
@Entity
@Data
public class Goals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String goalName;
    private String expectedTarget;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;
}
