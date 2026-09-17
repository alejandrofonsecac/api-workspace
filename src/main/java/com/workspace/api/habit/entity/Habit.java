package com.workspace.api.habit.entity;

import com.workspace.api.user.domain.UserOwnedEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.util.domain.Category;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Table(name = "tb_habit")
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Entity
public class Habit extends UserOwnedEntity {
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

    @ElementCollection
    @CollectionTable(name = "tb_habit_completions", joinColumns = @JoinColumn(name = "habit_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "completed_date"}))
    @Column(name = "completed_date", nullable = false)
    private Set<LocalDate> completedDates = new HashSet<>();
}
