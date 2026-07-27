package com.workspace.api.note.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Table(name = "tb_note")
@Entity
@Data
@AllArgsConstructor
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDateTime edited;
    private Boolean favorite = false;
    private Boolean fixed   = false;
    private LocalDateTime fixedAt;
}
