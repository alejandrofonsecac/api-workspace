package com.workspace.api.task.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "tb_task")
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime executionTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeEstimated;

    @Enumerated(EnumType.STRING)
    private Priority taskPriority;

    private String note;
    private Boolean completed;
}
