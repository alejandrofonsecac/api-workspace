package com.workspace.api.event.domain;

import com.workspace.api.retention.domain.RetainedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event extends RetainedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kind = "event";

    private String priority;

    @Column(nullable = false)
    private String title;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;
}
