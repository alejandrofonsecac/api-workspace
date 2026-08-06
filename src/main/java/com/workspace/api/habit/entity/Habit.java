package com.workspace.api.habit.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Table(name = "tb_habit")
@Data
@Entity
public class Habit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private Boolean frequencyEveryDay;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate localDate;

    private String objective;
    private String observation;
}
