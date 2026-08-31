package com.workspace.api.schedule.tasks.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private Boolean completed = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate taskCreatedDate;
    private LocalDate dueDate;
}
