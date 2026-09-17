package com.workspace.api.schedule.event.domain;

import com.workspace.api.user.domain.UserOwnedEntity;
import com.workspace.api.util.domain.Category;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Entity(name = "ScheduleEvent")
@Table(name = "tb_event")
public class Event extends UserOwnedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String description;

    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private Category category;
}
