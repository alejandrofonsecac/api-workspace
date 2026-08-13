package com.workspace.api.tasks.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "tb_tasks")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tasks {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String title;

    private String priority;

    private String dueLabel;
    private String period;
    private String project;
    private Boolean completed = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
    private String assignee;
}
