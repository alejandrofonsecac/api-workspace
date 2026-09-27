package com.workspace.api.schedule.tasks.domain;

import com.workspace.api.retention.domain.RetainedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Table(name = "tb_tasks")
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Tasks extends RetainedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String title;

    private String priority;

    private String dueLabel;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate taskCreatedDate;
    private LocalDate dueDate;
    private LocalTime dueTime;
}
